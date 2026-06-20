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
    private final List<ExtractedBlock> extractedBlocks = new ArrayList<>();
    private final Map<String, List<String>> visitedMethods = new HashMap<>();

    public FlowTracer(ProjectIndexer indexer) {
        this.indexer = indexer;
    }

    public List<ExtractedBlock> getExtractedBlocks() {
        extractedBlocks.removeIf(java.util.Objects::isNull);
        return extractedBlocks;
    }

    public List<ExtractedBlock> trace(String fqcn, String methodName, int argCount) {
        traceClassAndImplementors(fqcn, methodName, argCount);
        return getExtractedBlocks();
    }

    private List<String> traceRecursive(String fqcn, String methodName, int argCount) {
        // Prevent infinite recursion and duplicate processing
        String uniqueId = fqcn + "#" + methodName + (argCount != -1 ? "(" + argCount + ")" : "");
        if (visitedMethods.containsKey(uniqueId)) {
            return visitedMethods.get(uniqueId);
        }
        
        List<String> currentLinks = new ArrayList<>();
        visitedMethods.put(uniqueId, currentLinks);


        Path javaFile = indexer.getJavaFile(fqcn);
        if (javaFile == null) {
            String simpleName = fqcn.contains(".") ? fqcn.substring(fqcn.lastIndexOf(".") + 1) : fqcn;
            javaFile = indexer.getJavaFileBySimpleName(simpleName);
        }


        if (javaFile == null) {
            // It could be an external library or we couldn't resolve it. Just skip.
            return currentLinks;
        }

        try {
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
            
            ASTParser parser = ASTParser.newParser(AST.JLS21); 
            parser.setSource(content.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);
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
            
            String anchor = title.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-");
            String simpleClassName = actualFqcn;
            int lastDot = actualFqcn.lastIndexOf('.');
            if (lastDot != -1) {
                simpleClassName = actualFqcn.substring(lastDot + 1);
            }
            
            currentLinks.clear();
            currentLinks.add(String.format("[%s](#%s)", simpleClassName + "#" + methodName + (argCount != -1 ? "(" + argCount + ")" : ""), anchor));

            // Reserve spot for PRE-ORDER traversal output
            int blockIndex = extractedBlocks.size();
            extractedBlocks.add(null);

            // Assign targetIds to method calls
            for (ClassAstVisitor.MethodCallInfo call : visitor.getMethodCalls()) {
                List<String> targetIds = new ArrayList<>();
                
                if (call.receiver == null) {
                    List<String> staticTargets = resolveStaticImports(call.methodName, visitor.getStaticImports());
                    for (String staticTarget : staticTargets) {
                        targetIds.addAll(traceRecursive(staticTarget, call.methodName, call.argCount));
                    }
                    targetIds.addAll(traceClassAndImplementors(fqcn, call.methodName, call.argCount));
                } else if ("this".equals(call.receiver)) {
                    targetIds.addAll(traceClassAndImplementors(fqcn, call.methodName, call.argCount));
                } else {
                    List<String> fieldSources = visitor.getFields().stream().map(fd -> fd.source).collect(Collectors.toList());
                    String targetTypeFqcn = resolveTargetTypeFqcn(call.receiver, fqcn, fieldSources, visitor.getImports(), visitor.getLocalVariableTypes());
                    if (targetTypeFqcn != null) {
                        targetIds.addAll(traceClassAndImplementors(targetTypeFqcn, call.methodName, call.argCount));
                    }
                }
                
                // Use a dynamic field or just a local map to group targets
                // Actually, wait, let's keep it simple
            }

            // Group links per method
            StringBuilder combinedSource = new StringBuilder();
            boolean firstMethod = true;
            boolean hasMybatis = false;
            
            for (ClassAstVisitor.ExtractedMethod em : visitor.getExtractedMethods()) {
                if (isMyBatisAnnotation(em.source)) {
                    hasMybatis = true;
                }
                
                Map<Integer, Set<String>> lineToLinks = new HashMap<>();
                for (ClassAstVisitor.MethodCallInfo call : visitor.getMethodCalls()) {
                    // Check if call belongs to THIS method
                    int lineIndex = call.lineNumber - em.firstLineNumber;
                    String[] methodLines = em.source.split("\n", -1);
                    if (lineIndex >= 0 && lineIndex < methodLines.length) {
                        // resolve again for the call since we didn't store it
                        List<String> targetIds = new ArrayList<>();
                        if (call.receiver == null) {
                            List<String> staticTargets = resolveStaticImports(call.methodName, visitor.getStaticImports());
                            for (String staticTarget : staticTargets) {
                                targetIds.addAll(traceRecursive(staticTarget, call.methodName, call.argCount));
                            }
                            targetIds.addAll(traceClassAndImplementors(fqcn, call.methodName, call.argCount));
                        } else if ("this".equals(call.receiver)) {
                            targetIds.addAll(traceClassAndImplementors(fqcn, call.methodName, call.argCount));
                        } else {
                            List<String> fieldSources = visitor.getFields().stream().map(fd -> fd.source).collect(Collectors.toList());
                            String targetTypeFqcn = resolveTargetTypeFqcn(call.receiver, fqcn, fieldSources, visitor.getImports(), visitor.getLocalVariableTypes());
                            if (targetTypeFqcn != null) {
                                targetIds.addAll(traceClassAndImplementors(targetTypeFqcn, call.methodName, call.argCount));
                            }
                        }
                        if (!targetIds.isEmpty()) {
                            lineToLinks.computeIfAbsent(lineIndex, k -> new LinkedHashSet<>()).addAll(targetIds);
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
            if (javaFile != null && indexer.getProjectRootPath() != null && !visitor.getExtractedMethods().isEmpty()) {
                int firstLine = visitor.getExtractedMethods().get(0).firstLineNumber;
                fileLink = indexer.getProjectRootPath().relativize(javaFile).toString().replace('\\', '/') + "#L" + firstLine;
            }
            extractedBlocks.set(blockIndex, new ExtractedBlock(title, blockContent.toString(), anchor, fileLink));

            if (hasMybatis) {
                return currentLinks; // SQL is already in the annotation, we can stop here
            }

            // If it's potentially a Mapper but no annotation, try finding XML
            if (javaFile.toString().endsWith("Mapper.java") || javaFile.toString().endsWith("Repository.java")) {
                String xmlSql = XmlSqlExtractor.extractSql(indexer.getAllXmlFiles(), fqcn, methodName);
                if (xmlSql != null) {
                    String xmlTitle = fqcn + "#" + methodName + " (XML)";
                    String xmlAnchor = xmlTitle.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-");
                    extractedBlocks.add(new ExtractedBlock(xmlTitle, xmlSql, xmlAnchor));
                    
                    String simpleXmlName = fqcn;
                    int xmlDot = fqcn.lastIndexOf('.');
                    if (xmlDot != -1) simpleXmlName = fqcn.substring(xmlDot + 1);
                    
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

    private List<String> traceClassAndImplementors(String targetFqcn, String methodName, int argCount) {
        List<String> links = new ArrayList<>();
        // Trace the declared type itself
        links.addAll(traceRecursive(targetFqcn, methodName, argCount));
        
        // Also trace all implementors/subclasses
        List<String> implementors = indexer.getImplementors(targetFqcn);
        for (String impl : implementors) {
            links.addAll(traceRecursive(impl, methodName, argCount));
        }
        return links;
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
        // e.g. "import java.util.List;" -> "List"
        // e.g. "import static org.mockito.Mockito.when;" -> "when"
        String cleaned = imp.replace("import ", "").replace("static ", "").replace(";", "").trim();
        int lastDot = cleaned.lastIndexOf('.');
        if (lastDot != -1) {
            return cleaned.substring(lastDot + 1);
        }
        return cleaned;
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
