package com.extractor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FlowTracer {

    private final ProjectIndexer indexer;
    private final Map<String, List<String>> visitedMethods = new HashMap<>();
    private final List<ExtractedBlock> extractedBlocks = new ArrayList<>();
    private final java.nio.file.Path outDirPath;

    public FlowTracer(ProjectIndexer indexer, java.nio.file.Path outDirPath) {
        this.indexer = indexer;
        this.outDirPath = outDirPath != null ? outDirPath.toAbsolutePath().normalize() : null;
    }

    public FlowTracer(ProjectIndexer indexer) {
        this(indexer, null);
    }

    private final Set<String> dataModelsToExtract = new LinkedHashSet<>();
    private String entryFqcn;
    private String entryMethodName;

    public List<ExtractedBlock> getExtractedBlocks() {
        extractedBlocks.removeIf(java.util.Objects::isNull);
        return extractedBlocks;
    }

    public List<ExtractedBlock> trace(String fqcn, String methodName, int argCount) {
        this.entryFqcn = fqcn;
        this.entryMethodName = methodName;
        traceHierarchy(fqcn, methodName, argCount);
        
        for (String modelFqcn : new ArrayList<>(dataModelsToExtract)) {
            tracePojo(modelFqcn);
        }
        
        return getExtractedBlocks();
    }

    /**
     * メソッドの呼び出しフローを再帰的にトレースし、各メソッドのソースコードや呼び出し関係を抽出します。
     * 指定されたクラス(FQCN)とメソッド名から出発し、AST（抽象構文木）を解析して
     * メソッド内部で呼び出されている他のメソッドを再帰的に探索します。
     *
     * 【CallFlowを作成するうえで作成するデータ構造（主にマップ）の説明】
     * - visitedMethods (Map<String, List<String>>): 
     *   無限ループ（循環呼び出し）を防ぎ、同一メソッドの重複解析を避けるためのキャッシュです。
     *   キーは "FQCN#メソッド名(引数数)" のユニークID、値はそのメソッドを表すMarkdownリンクのリストです。
     * - extractedBlocks (List<ExtractedBlock>): 
     *   最終的なMarkdown出力をプレオーダー（先行順）で保持するリストです。
     *   解析開始時に自身のインデックス（null）を予約し、解析後にソースコードブロックを格納します。
     *
     * @param fqcn 対象メソッドが属するクラスの完全修飾クラス名(FQCN)
     * @param methodName トレース対象のメソッド名
     * @param argCount メソッドの引数の数（オーバーロード解決用、不明な場合は-1）
     * @return 呼び出し元へ返すための、このメソッド自身を表すMarkdownリンク文字列のリスト
     */
    private List<String> traceRecursive(String fqcn, String methodName, int argCount) {
        // メソッドの一意な識別子（探索済み判定用）
        String uniqueId = fqcn + "#" + methodName + (argCount != -1 ? "(" + argCount + ")" : "");
        if (visitedMethods.containsKey(uniqueId)) {
            return visitedMethods.get(uniqueId);
        }
        
        // このメソッド自身のMarkdownリンクを保持するリスト。探索済みキャッシュとして先に登録しておく
        List<String> currentLinks = new ArrayList<>();
        visitedMethods.put(uniqueId, currentLinks);


        // 対象クラスのJavaソースファイルを検索
        Path javaFile = indexer.getJavaFile(fqcn);
        if (javaFile == null) {
            String simpleName = fqcn.contains(".") ? fqcn.substring(fqcn.lastIndexOf(".") + 1) : fqcn;
            javaFile = indexer.getJavaFileBySimpleName(simpleName);
        }


        if (javaFile == null) {
            // 外部ライブラリなど、ソースが解決できない場合はこれ以上トレースしない
            return currentLinks;
        }

        try {
            // ソースコードの読み込み（文字コードのフォールバック対応）
            String content;
            try {
                content = Files.readString(javaFile);
            } catch (java.nio.charset.MalformedInputException e) {
                try {
                    content = Files.readString(javaFile, java.nio.charset.Charset.forName("Windows-31J"));
                } catch (Exception ex) {
                    content = Files.readString(javaFile, java.nio.charset.StandardCharsets.ISO_8859_1);
                }
            }
            
            // JDTを利用してソースコードのAST（抽象構文木）を構築
            ASTParser parser = ASTParser.newParser(AST.JLS21); 
            parser.setSource(content.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            
            // メソッド内の呼び出しやフィールド参照を抽出するためのVisitorを実行
            ClassAstVisitor visitor = new ClassAstVisitor(content, fqcn, methodName, argCount, cu);
            cu.accept(visitor);

            if (visitor.getExtractedMethods().isEmpty()) {
                // Method not found in this specific class, but might be in superclass.
                String superclass = indexer.getSuperclass(fqcn);
                if (superclass != null) {
                    // Try to trace the superclass
                    List<String> superLinks = traceRecursive(superclass, methodName, argCount);
                    currentLinks.clear();
                    currentLinks.addAll(superLinks);
                }
                if (visitor.getExtractedMethods().isEmpty()) {
                    System.err.println("WARNING: Target method not found: " + methodName + " in " + fqcn);
                    return currentLinks;
                }
            }

            String actualFqcn = visitor.getTargetFqcn();
            if (actualFqcn == null) {
                actualFqcn = fqcn;
            }

            String title = actualFqcn + "#" + methodName + (argCount != -1 ? "(" + argCount + ")" : "");
            
            if (!actualFqcn.equals(fqcn)) {
                String actualUniqueId = actualFqcn + "#" + methodName + (argCount != -1 ? "(" + argCount + ")" : "");
                if (visitedMethods.containsKey(actualUniqueId)) {
                    List<String> existing = visitedMethods.get(actualUniqueId);
                    currentLinks.clear();
                    currentLinks.addAll(existing);
                    return currentLinks;
                }
                visitedMethods.put(actualUniqueId, currentLinks);
            }
            
            // Markdown出力時のジャンプ先アンカー文字列を生成
            String anchor = title.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-");
            String simpleClassName = actualFqcn;
            int lastDot = actualFqcn.lastIndexOf('.');
            if (lastDot != -1) {
                simpleClassName = actualFqcn.substring(lastDot + 1);
            }
            
            // 呼び出し元に戻すための自メソッドへのリンクを構築
            currentLinks.clear();
            currentLinks.add(String.format("[%s](#%s)", simpleClassName + "#" + methodName + (argCount != -1 ? "(" + argCount + ")" : ""), anchor));

            // 出力順序を保つため、現在の抽出ブロックの位置(index)を予約しておく
            int blockIndex = extractedBlocks.size();
            extractedBlocks.add(null);


            // メソッドごとに抽出したソースコードを統合する
            StringBuilder combinedSource = new StringBuilder();
            boolean firstMethod = true;
            boolean hasMybatis = false;
            
            for (ClassAstVisitor.ExtractedMethod em : visitor.getExtractedMethods()) {
                if (isMyBatisAnnotation(em.source)) {
                    hasMybatis = true;
                }
                
                // 抽出した各メソッド内での他のメソッド呼び出しを再帰的にトレースする
                for (ClassAstVisitor.MethodCallInfo call : visitor.getMethodCalls()) {
                    // その呼び出しが現在のメソッド定義行の中に含まれているかチェック
                    int lineIndex = call.lineNumber - em.firstLineNumber;
                    String[] methodLines = em.source.split("\n", -1);
                    if (lineIndex >= 0 && lineIndex < methodLines.length) {
                        // 呼び出し先のインスタンス（receiver）の種類に応じてFQCNを解決し、再帰トレースを実行する
                        if (call.receiver == null) {
                            List<String> staticTargets = resolveStaticImports(call.methodName, visitor.getStaticImports());
                            for (String staticTarget : staticTargets) {
                                traceRecursive(staticTarget, call.methodName, call.argCount);
                            }
                            traceHierarchy(fqcn, call.methodName, call.argCount);
                        } else if ("this".equals(call.receiver)) {
                            traceHierarchy(fqcn, call.methodName, call.argCount);
                        } else {
                            List<String> fieldSources = visitor.getFields().stream().map(fd -> fd.source).collect(Collectors.toList());
                            String targetTypeFqcn = resolveTargetTypeFqcn(call.receiver, fqcn, fieldSources, visitor.getImports(), visitor.getLocalVariableTypes());
                            if (targetTypeFqcn != null) {
                                traceHierarchy(targetTypeFqcn, call.methodName, call.argCount);
                            }
                        }
                    }
                }

                String[] methodLines = em.source.split("\n", -1);

                
                if (!firstMethod) {
                    combinedSource.append("\n\n");
                }
                combinedSource.append(String.join("\n", methodLines));
                firstMethod = false;
            }

            StringBuilder blockContent = new StringBuilder();
            String methodSource = combinedSource.toString();
            List<String> usedFields = new ArrayList<>();
            for (ClassAstVisitor.FieldData fd : visitor.getFields()) {
                boolean isUsed = false;
                for (String name : fd.names) {
                    // Match whole word
                    if (methodSource.matches("(?s).*\\b" + name + "\\b.*")) {
                        isUsed = true;
                        break;
                    }
                }
                if (isUsed) {
                    usedFields.add(fd.source);
                }
            }

            String extractedContent = String.join("\n", usedFields) + "\n" + methodSource;
            
            Set<String> words = new HashSet<>();
            java.util.regex.Matcher wordMatcher = java.util.regex.Pattern.compile("\\b[A-Za-z_][A-Za-z0-9_]*\\b").matcher(extractedContent);
            while (wordMatcher.find()) {
                words.add(wordMatcher.group());
            }
            
            Set<String> standaloneWords = new HashSet<>();
            java.util.regex.Matcher standaloneMatcher = java.util.regex.Pattern.compile("(?<![A-Za-z0-9_.])([A-Za-z_][A-Za-z0-9_]*)").matcher(extractedContent);
            while (standaloneMatcher.find()) {
                standaloneWords.add(standaloneMatcher.group(1));
            }
            
            boolean hasImports = false;
            StringBuilder importsBlock = new StringBuilder();
            for (String imp : visitor.getImports()) {
                String simpleName = getSimpleNameFromImport(imp);
                if (simpleName.equals("*")) {
                    boolean keep = true;
                    if (imp.startsWith("import static ")) {
                        String className = imp.substring(14, imp.lastIndexOf('.')).trim();
                        keep = isStaticWildcardUsed(className, standaloneWords, indexer);
                    } else {
                        String packageName = imp.substring(7, imp.lastIndexOf('.')).trim();
                        keep = isPackageWildcardUsed(packageName, words, indexer);
                    }
                    if (keep) {
                        importsBlock.append(imp).append("\n");
                        hasImports = true;
                    }
                } else if (words.contains(simpleName)) {
                    importsBlock.append(imp).append("\n");
                    hasImports = true;
                }
            }
            if (hasImports) {
                blockContent.append("// --- インポート ---\n");
                blockContent.append(importsBlock).append("\n");
            }
            
            if (visitor.getTargetClassSignature() != null) {
                blockContent.append("// --- 所属クラス ---\n");
                blockContent.append("// ").append(visitor.getTargetClassSignature()).append("\n\n");
            }
            
            if (!usedFields.isEmpty()) {
                blockContent.append("// --- フィールド ---\n");
                for (String field : usedFields) {
                    blockContent.append(field).append("\n");
                }
                blockContent.append("\n");
            }
            
            blockContent.append("// --- メソッド定義 ---\n");
            
            blockContent.append(methodSource).append("\n");

            String fileLink = null;
            if (javaFile != null && !visitor.getExtractedMethods().isEmpty()) {
                int firstLine = visitor.getExtractedMethods().get(0).firstLineNumber;
                String absPath = javaFile.toAbsolutePath().normalize().toString().replace('\\', '/');
                // Ensure the path starts with a slash for the vscode://file/ URL format
                if (!absPath.startsWith("/")) {
                    absPath = "/" + absPath;
                }
                fileLink = "vscode://file" + absPath + ":" + firstLine;
            }
            extractedBlocks.set(blockIndex, new ExtractedBlock(title, blockContent.toString(), anchor, fileLink));

            if (hasMybatis) {
                collectJavaMethodPojos(visitor, actualFqcn, methodName);
                return currentLinks; // SQL is already in the annotation, we can stop here
            }

            boolean isMapper = javaFile.toString().endsWith("Mapper.java") || javaFile.toString().endsWith("Repository.java");
            boolean isEntry = (actualFqcn.equals(this.entryFqcn) && methodName.equals(this.entryMethodName));
            if (isEntry || isMapper) {
                collectJavaMethodPojos(visitor, actualFqcn, methodName);
            }

            // If it's potentially a Mapper but no annotation, try finding XML
            if (isMapper) {
                XmlSqlExtractor.XmlResult xmlResult = XmlSqlExtractor.extractSql(indexer.getAllXmlFiles(), actualFqcn, methodName);
                if (xmlResult != null) {
                    if (xmlResult.pojoFqcns != null) {
                        for (String pojoType : xmlResult.pojoFqcns) {
                            String resolved = resolveModelTypeFqcn(pojoType, actualFqcn, visitor.getImports());
                            if (isCustomProjectClass(resolved)) {
                                dataModelsToExtract.add(resolved);
                            }
                        }
                    }

                    String xmlTitle = actualFqcn + "#" + methodName + " (XML)";
                    String xmlAnchor = xmlTitle.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-");
                    
                    String absPath = xmlResult.filePath.replace('\\', '/');
                    if (!absPath.startsWith("/")) {
                        absPath = "/" + absPath;
                    }
                    String xmlFileLink = "vscode://file" + absPath + ":" + xmlResult.lineNumber;
                    
                    extractedBlocks.add(new ExtractedBlock(xmlTitle, xmlResult.sql, xmlAnchor, xmlFileLink));
                    
                    String simpleXmlName = actualFqcn;
                    int xmlDot = actualFqcn.lastIndexOf('.');
                    if (xmlDot != -1) simpleXmlName = actualFqcn.substring(xmlDot + 1);
                    
                    currentLinks.clear();
                    currentLinks.add(String.format("[%s](#%s)", simpleXmlName + "#" + methodName + " (XML)", xmlAnchor));
                }
                return currentLinks; // Reached DB layer
            }

            return currentLinks;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void collectJavaMethodPojos(ClassAstVisitor visitor, String actualFqcn, String methodName) {
        for (String typeName : visitor.getMethodSignatureTypes()) {
            String resolved = resolveModelTypeFqcn(typeName, actualFqcn, visitor.getImports());
            if (isCustomProjectClass(resolved)) {
                dataModelsToExtract.add(resolved);
            }
        }
    }

    private List<String> traceHierarchy(String targetFqcn, String methodName, int argCount) {
        List<String> links = new ArrayList<>();
        
        // 1. Trace superclasses and superinterfaces (declarations)
        List<String> parents = new ArrayList<>();
        collectParentsRecursive(targetFqcn, parents);
        for (String parent : parents) {
            links.addAll(traceRecursive(parent, methodName, argCount));
        }

        // 2. Trace the target class itself
        links.addAll(traceRecursive(targetFqcn, methodName, argCount));
        
        // 3. Trace implementors/subclasses
        List<String> implementors = indexer.getImplementors(targetFqcn);
        for (String impl : implementors) {
            links.addAll(traceRecursive(impl, methodName, argCount));
        }
        return links;
    }

    private void collectParentsRecursive(String fqcn, List<String> result) {
        String superclass = indexer.getSuperclass(fqcn);
        if (superclass != null && !result.contains(superclass)) {
            collectParentsRecursive(superclass, result);
            result.add(superclass);
        }
        List<String> superInterfaces = indexer.getSuperInterfaces(fqcn);
        for (String iface : superInterfaces) {
            if (!result.contains(iface)) {
                collectParentsRecursive(iface, result);
                result.add(iface);
            }
        }
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                try {
                    return Class.forName(className.substring(0, lastDot) + "$" + className.substring(lastDot + 1));
                } catch (ClassNotFoundException ex) {}
            }
        }
        return null;
    }

    private boolean isStaticWildcardUsed(String className, Set<String> words, ProjectIndexer indexer) {
        Set<String> safeWords = new HashSet<>(words);
        safeWords.removeAll(Arrays.asList("public", "private", "protected", "static", "final", "void", "return", "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue", "new", "try", "catch", "finally", "throw", "throws", "this", "super", "synchronized", "volatile", "transient", "instanceof", "null", "true", "false", "class", "interface", "enum", "extends", "implements", "package", "import", "var"));

        try {
            Class<?> clazz = loadClass(className);
            if (clazz != null) {
                for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && java.lang.reflect.Modifier.isPublic(f.getModifiers())) {
                        if (safeWords.contains(f.getName())) return true;
                    }
                }
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                        if (safeWords.contains(m.getName())) return true;
                    }
                }
                return false;
            }
        } catch (Throwable t) {}

        java.nio.file.Path srcFile = indexer.getJavaFile(className);
        if (srcFile != null) {
            try {
                String content = java.nio.file.Files.readString(srcFile);
                for (String w : safeWords) {
                    if (content.matches("(?s).*\\b" + w + "\\b.*")) return true;
                }
                return false;
            } catch (Exception e) {}
        }
        
        boolean keepUnknown = Boolean.getBoolean("extractor.keep.unknown.wildcards");
        return className.startsWith("java.") || className.startsWith("javax.") ? false : keepUnknown;
    }

    private boolean isPackageWildcardUsed(String packageName, Set<String> words, ProjectIndexer indexer) {
        for (String w : words) {
            if (w.length() > 0 && Character.isUpperCase(w.charAt(0))) {
                try {
                    Class.forName(packageName + "." + w);
                    return true;
                } catch (Throwable t) {}
                
                if (indexer.getJavaFile(packageName + "." + w) != null) {
                    return true;
                }
            }
        }
        boolean keepUnknown = Boolean.getBoolean("extractor.keep.unknown.wildcards");
        return packageName.startsWith("java.") || packageName.startsWith("javax.") ? false : keepUnknown;
    }

    private boolean isMyBatisAnnotation(String methodSource) {
        return methodSource.contains("@Select") || methodSource.contains("@Update") || 
               methodSource.contains("@Insert") || methodSource.contains("@Delete");
    }

    private List<String> resolveStaticImports(String methodName, List<String> staticImports) {
        List<String> candidates = new ArrayList<>();
        for (String imp : staticImports) {
            // imp is like "org.apache.commons.lang3.StringUtils.isBlank" or "org.apache.commons.lang3.StringUtils.*"
            if (imp.endsWith("." + methodName)) {
                candidates.add(imp.substring(0, imp.lastIndexOf('.')));
            } else if (imp.endsWith(".*")) {
                candidates.add(imp.substring(0, imp.lastIndexOf('.')));
            }
        }
        return candidates;
    }

    private String getSimpleNameFromImport(String imp) {
        String cleaned = imp.replace("import ", "").replace("static ", "").replace(";", "").trim();
        int lastDot = cleaned.lastIndexOf('.');
        if (lastDot != -1) {
            return cleaned.substring(lastDot + 1);
        }
        return cleaned;
    }

    private String resolveModelTypeFqcn(String typeName, String currentFqcn, List<String> imports) {
        if (typeName == null) return null;
        String simpleType = typeName;
        if (simpleType.contains("<") && simpleType.contains(">")) {
            simpleType = simpleType.substring(simpleType.indexOf("<") + 1, simpleType.lastIndexOf(">"));
        }
        simpleType = simpleType.replace("[]", "").trim();

        if (simpleType.equals("String") || simpleType.equals("Integer") || simpleType.equals("Long") || 
            simpleType.equals("Boolean") || simpleType.equals("Double") || simpleType.equals("Float") ||
            simpleType.equals("Object") || simpleType.equals("void") || simpleType.equals("int") ||
            simpleType.equals("long") || simpleType.equals("boolean") || simpleType.equals("double")) {
            return null;
        }

        for (String imp : imports) {
            if (imp.endsWith("." + simpleType + ";") || imp.endsWith("." + simpleType + "\n")) {
                return imp.replace("import ", "").replace(";", "").trim();
            }
        }
        if (currentFqcn != null && currentFqcn.contains(".")) {
            String pkg = currentFqcn.substring(0, currentFqcn.lastIndexOf('.'));
            return pkg + "." + simpleType;
        }
        return simpleType;
    }

    private boolean isCustomProjectClass(String fqcn) {
        if (fqcn == null || fqcn.startsWith("java.") || fqcn.startsWith("javax.") || fqcn.startsWith("org.springframework.")) {
            return false;
        }
        return indexer.getJavaFile(fqcn) != null || indexer.getJavaFileBySimpleName(fqcn) != null;
    }

    private void tracePojo(String fqcn) {
        Path javaFile = indexer.getJavaFile(fqcn);
        if (javaFile == null) {
            String simpleName = fqcn.contains(".") ? fqcn.substring(fqcn.lastIndexOf(".") + 1) : fqcn;
            javaFile = indexer.getJavaFileBySimpleName(simpleName);
        }
        if (javaFile == null) return;

        try {
            String content = Files.readString(javaFile);
            ASTParser parser = ASTParser.newParser(AST.JLS21); 
            parser.setSource(content.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            
            ClassAstVisitor visitor = new ClassAstVisitor(content, fqcn, "non_existent_method_to_force_pojo_extraction", -1, cu);
            cu.accept(visitor);
            
            StringBuilder blockContent = new StringBuilder();
            
            if (!visitor.getImports().isEmpty()) {
                blockContent.append("// --- インポート ---\n");
                for (String imp : visitor.getImports()) {
                    blockContent.append(imp).append("\n");
                }
                blockContent.append("\n");
            }
            
            if (visitor.getTargetClassSignature() != null) {
                blockContent.append("// --- クラス定義 ---\n");
                blockContent.append(visitor.getTargetClassSignature()).append(" {\n");
            } else {
                blockContent.append("// --- クラス定義 ---\nclass ").append(fqcn.substring(fqcn.lastIndexOf('.') + 1)).append(" {\n");
            }
            
            if (!visitor.getFields().isEmpty()) {
                for (ClassAstVisitor.FieldData fd : visitor.getFields()) {
                    blockContent.append("    ").append(fd.source.replace("\n", "\n    ")).append("\n");
                }
            }
            blockContent.append("}\n");

            String title = fqcn + " (Data Model)";
            String anchor = title.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-");
            String absPath = javaFile.toAbsolutePath().normalize().toString().replace('\\', '/');
            if (!absPath.startsWith("/")) absPath = "/" + absPath;
            String fileLink = "vscode://file" + absPath + ":1";

            extractedBlocks.add(new ExtractedBlock(title, blockContent.toString(), anchor, fileLink));
        } catch (Exception e) {
            // ignore
        }
    }

    private String resolveTargetTypeFqcn(String receiver, String currentFqcn, List<String> fields, List<String> imports, Map<String, String> localVariableTypes) {
        if ("super".equals(receiver)) {
            return indexer.getSuperclass(currentFqcn);
        }
        
        String targetSimpleType = localVariableTypes.get(receiver);

        if (targetSimpleType == null) {
            for (String field : fields) {
                if (field.contains(" " + receiver + ";") || field.contains(" " + receiver + " =")) {
                    String[] parts = field.trim().split("\\s+");
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].equals(receiver + ";") || parts[i].equals(receiver)) {
                            targetSimpleType = parts[i - 1]; 
                            break;
                        }
                    }
                }
            }
        }

        if (targetSimpleType == null) {
            targetSimpleType = receiver;
        }

        if (targetSimpleType.contains("<")) {
            targetSimpleType = targetSimpleType.substring(0, targetSimpleType.indexOf("<"));
        }

        for (String imp : imports) {
            if (imp.endsWith("." + targetSimpleType + ";") || imp.endsWith("." + targetSimpleType + "\n")) {
                return imp.replace("import ", "").replace(";", "").trim();
            }
        }

        return targetSimpleType;
    }
}
