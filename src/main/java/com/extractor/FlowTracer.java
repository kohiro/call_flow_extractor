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

    public List<ExtractedBlock> trace(String fqcn, String methodName, int argCount) {
        traceRecursive(fqcn, methodName, argCount);
        return extractedBlocks;
    }

    private List<String> traceRecursive(String fqcn, String methodName, int argCount) {
        // Use argCount in uniqueId to distinguish overloads, use -1 if unknown
        String uniqueId = fqcn + "#" + methodName + (argCount != -1 ? "#" + argCount : "");
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

        if (javaFile != null) {
            fqcn = indexer.getFqcn(javaFile);
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

            ClassAstVisitor visitor = new ClassAstVisitor(content, methodName, argCount, cu);
            cu.accept(visitor);

            if (visitor.getTargetMethodDecl() == null) {
                // Method not found in this specific class, but might be in superclass.
                String superclass = indexer.getSuperclass(fqcn);
                if (superclass != null) {
                    // Try to trace the superclass
                    List<String> superLinks = traceRecursive(superclass, methodName, argCount);
                    currentLinks.clear();
                    currentLinks.addAll(superLinks);
                }
                return currentLinks;
            }

            String relativePath = indexer.getProjectRootPath().relativize(javaFile).toString();
            String title = relativePath + " (" + methodName + ")";
            String anchor = title.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-");
            
            currentLinks.clear();
            currentLinks.add(String.format("[%s](#%s)", javaFile.getFileName().toString() + " (" + methodName + ")", anchor));

            // Trace further down and collect links
            Map<Integer, Set<String>> lineToLinks = new HashMap<>();
            
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
                    // Resolve the receiver to a class
                    List<String> fieldSources = visitor.getFields().stream().map(fd -> fd.source).collect(Collectors.toList());
                    String targetTypeFqcn = resolveTargetTypeFqcn(call.receiver, fqcn, fieldSources, visitor.getImports(), visitor.getLocalVariableTypes());
                    if (targetTypeFqcn != null) {
                        targetIds.addAll(traceClassAndImplementors(targetTypeFqcn, call.methodName, call.argCount));
                    }
                }
                
                if (!targetIds.isEmpty()) {
                    int lineIndex = call.lineNumber - visitor.getTargetMethodFirstLineNumber();
                    lineToLinks.computeIfAbsent(lineIndex, k -> new LinkedHashSet<>()).addAll(targetIds);
                }
            }

            // Append comments to method source lines
            String[] methodLines = visitor.getTargetMethodSource().split("\n", -1);
            for (Map.Entry<Integer, Set<String>> entry : lineToLinks.entrySet()) {
                int lineIndex = entry.getKey();
                if (lineIndex >= 0 && lineIndex < methodLines.length) {
                    String linksComment = " // -> " + String.join(", ", entry.getValue());
                    methodLines[lineIndex] = methodLines[lineIndex] + linksComment;
                }
            }
            String methodSource = String.join("\n", methodLines);

            // Build the block content
            StringBuilder blockContent = new StringBuilder();

            // Filter fields based on usage in the target method
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

            // Determine which text is actually extracted for import filtering
            String extractedContent = String.join("\n", usedFields) + "\n" + methodSource;
            
            boolean hasImports = false;
            StringBuilder importsBlock = new StringBuilder();
            for (String imp : visitor.getImports()) {
                // Example: "import java.util.List;" -> "List"
                // Example: "import static org.mockito.Mockito.when;" -> "when"
                String simpleName = getSimpleNameFromImport(imp);
                // Wildcards or directly used classes
                if (simpleName.equals("*") || extractedContent.contains(simpleName)) {
                    importsBlock.append(imp).append("\n");
                    hasImports = true;
                }
            }
            if (hasImports) {
                blockContent.append("// --- インポート ---\n");
                blockContent.append(importsBlock).append("\n");
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

            extractedBlocks.add(new ExtractedBlock(title, blockContent.toString(), anchor));

            // Check if this is a Mapper with an annotation
            if (isMyBatisAnnotation(visitor.getTargetMethodSource())) {
                return currentLinks; // SQL is already in the annotation, we can stop here
            }

            // If it's potentially a Mapper but no annotation, try finding XML
            if (javaFile.toString().endsWith("Mapper.java") || javaFile.toString().endsWith("Repository.java")) {
                String xmlSql = XmlSqlExtractor.extractSql(indexer.getAllXmlFiles(), fqcn, methodName);
                if (xmlSql != null) {
                    String xmlTitle = relativePath + " (XML: " + methodName + ")";
                    String xmlAnchor = xmlTitle.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-");
                    extractedBlocks.add(new ExtractedBlock(xmlTitle, xmlSql, xmlAnchor));
                    
                    currentLinks.clear();
                    currentLinks.add(String.format("[%s](#%s)", javaFile.getFileName().toString() + " (XML)", xmlAnchor));
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
