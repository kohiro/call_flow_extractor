package com.extractor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FlowTracer {

    private final ProjectIndexer indexer;
    private final Set<String> visitedMethods = new HashSet<>();
    private final List<ExtractedBlock> extractedBlocks = new ArrayList<>();

    public FlowTracer(ProjectIndexer indexer) {
        this.indexer = indexer;
    }

    public List<ExtractedBlock> trace(String fqcn, String methodName) {
        traceRecursive(fqcn, methodName);
        return extractedBlocks;
    }

    private void traceRecursive(String fqcn, String methodName) {
        String uniqueId = fqcn + "#" + methodName;
        if (visitedMethods.contains(uniqueId)) {
            return; // Avoid infinite loops
        }
        visitedMethods.add(uniqueId);

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
            return;
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

            ClassAstVisitor visitor = new ClassAstVisitor(content, methodName);
            cu.accept(visitor);

            if (visitor.getTargetMethodDecl() == null) {
                // Method not found in this specific class, but might be in superclass.
                String superclass = indexer.getSuperclass(fqcn);
                if (superclass != null) {
                    // Try to trace the superclass
                    traceRecursive(superclass, methodName);
                }
                return;
            }

            // Build the block content
            StringBuilder blockContent = new StringBuilder();
            
            // Determine which text is actually extracted
            String extractedContent = String.join("\n", visitor.getFields()) + "\n" + visitor.getTargetMethodSource();
            
            boolean hasImports = false;
            for (String imp : visitor.getImports()) {
                // Example: "import java.util.List;" -> "List"
                // Example: "import static org.mockito.Mockito.when;" -> "when"
                String simpleName = getSimpleNameFromImport(imp);
                // Wildcards or directly used classes
                if (simpleName.equals("*") || extractedContent.contains(simpleName)) {
                    blockContent.append(imp).append("\n");
                    hasImports = true;
                }
            }
            if (hasImports) blockContent.append("\n");
            
            for (String field : visitor.getFields()) {
                blockContent.append(field).append("\n");
            }
            if (!visitor.getFields().isEmpty()) blockContent.append("\n");
            
            blockContent.append(visitor.getTargetMethodSource()).append("\n");

            extractedBlocks.add(new ExtractedBlock(javaFile.toString(), blockContent.toString()));

            // Check if this is a Mapper with an annotation
            if (isMyBatisAnnotation(visitor.getTargetMethodSource())) {
                return; // SQL is already in the annotation, we can stop here
            }

            // If it's potentially a Mapper but no annotation, try finding XML
            if (javaFile.toString().endsWith("Mapper.java") || javaFile.toString().endsWith("Repository.java")) {
                String xmlSql = XmlSqlExtractor.extractSql(indexer.getAllXmlFiles(), fqcn, methodName);
                if (xmlSql != null) {
                    extractedBlocks.add(new ExtractedBlock(fqcn + " (XML: " + methodName + ")", xmlSql));
                }
                return; // Reached DB layer
            }

            // Trace further down
            for (ClassAstVisitor.MethodCallInfo call : visitor.getMethodCalls()) {
                if (call.receiver == null) {
                    // Could be intra-class or statically imported
                    List<String> staticTargets = resolveStaticImports(call.methodName, visitor.getStaticImports());
                    for (String staticTarget : staticTargets) {
                        traceRecursive(staticTarget, call.methodName);
                    }
                    traceClassAndImplementors(fqcn, call.methodName);
                } else if ("this".equals(call.receiver)) {
                    traceClassAndImplementors(fqcn, call.methodName);
                } else {
                    // Resolve the receiver to a class
                    String targetTypeFqcn = resolveTargetTypeFqcn(call.receiver, fqcn, visitor.getFields(), visitor.getImports(), visitor.getLocalVariableTypes());
                    if (targetTypeFqcn != null) {
                        traceClassAndImplementors(targetTypeFqcn, call.methodName);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void traceClassAndImplementors(String targetFqcn, String methodName) {
        // Trace the declared type itself
        traceRecursive(targetFqcn, methodName);
        
        // Also trace all implementors/subclasses
        List<String> implementors = indexer.getImplementors(targetFqcn);
        for (String impl : implementors) {
            traceRecursive(impl, methodName);
        }
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
