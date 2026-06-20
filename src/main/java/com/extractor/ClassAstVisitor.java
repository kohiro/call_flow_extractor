package com.extractor;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassAstVisitor extends ASTVisitor {

    private final String targetFqcn;
    private final String targetMethodName;
    private final int targetArgCount;
    private final String fileContent;
    private final CompilationUnit cu;
    private int targetMethodFirstLineNumber = -1;

    public static class FieldData {
        public final String source;
        public final List<String> names;
        public FieldData(String source, List<String> names) {
            this.source = source;
            this.names = names;
        }
    }

    private final List<String> imports = new ArrayList<>();
    private final List<String> staticImports = new ArrayList<>();
    public static class ExtractedMethod {
        public String source;
        public int firstLineNumber;
        public MethodDeclaration decl;
    }
    
    private List<ExtractedMethod> extractedMethods = new ArrayList<>();
    private String targetClassSignature = null;
    
    public List<ExtractedMethod> getExtractedMethods() {
        return extractedMethods;
    }
    
    private final List<FieldData> fields = new ArrayList<>();
    private final List<MethodCallInfo> methodCalls = new ArrayList<>();
    private final Map<String, String> localVariableTypes = new HashMap<>();
    private final Map<String, String> methodReturnTypes = new HashMap<>();
    private boolean isInterface = false;

    public ClassAstVisitor(String fileContent, String targetFqcn, String targetMethodName, int targetArgCount, CompilationUnit cu) {
        this.fileContent = fileContent;
        this.targetFqcn = targetFqcn;
        this.targetMethodName = targetMethodName;
        this.targetArgCount = targetArgCount;
        this.cu = cu;
        
        // First pass to collect method return types
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                if (node.getReturnType2() != null) {
                    methodReturnTypes.put(node.getName().getIdentifier(), node.getReturnType2().toString());
                }
                return false; // No need to visit children of MethodDeclaration for this pass
            }
        });
    }

    private String computeNodeFqcn(ASTNode node) {
        if (node != null) {
            StringBuilder sb = new StringBuilder();
            ASTNode parent = node.getParent();
            while (parent != null) {
                if (parent instanceof TypeDeclaration) {
                    if (sb.length() > 0) sb.insert(0, ".");
                    sb.insert(0, ((TypeDeclaration) parent).getName().getIdentifier());
                } else if (parent instanceof EnumDeclaration) {
                    if (sb.length() > 0) sb.insert(0, ".");
                    sb.insert(0, ((EnumDeclaration) parent).getName().getIdentifier());
                } else if (parent instanceof RecordDeclaration) {
                    if (sb.length() > 0) sb.insert(0, ".");
                    sb.insert(0, ((RecordDeclaration) parent).getName().getIdentifier());
                }
                parent = parent.getParent();
            }
            if (sb.length() > 0) {
                if (cu != null && cu.getPackage() != null) {
                    return cu.getPackage().getName().getFullyQualifiedName() + "." + sb.toString();
                }
                return sb.toString();
            }
        }
        return null;
    }

    public int getTargetMethodFirstLineNumber() {
        return targetMethodFirstLineNumber;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (node.isInterface()) {
            isInterface = true;
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ImportDeclaration node) {
        String source = extractSource(node).trim();
        // Fallback to string check if node.isStatic() returns false incorrectly
        if (node.isStatic() || source.startsWith("import static ")) {
            staticImports.add(node.getName().getFullyQualifiedName());
            imports.add(source); // Also add to normal imports so it appears in output
        } else {
            imports.add(source);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        String source = extractSourceDedented(node);
        List<String> names = new ArrayList<>();
        for (Object obj : node.fragments()) {
            if (obj instanceof VariableDeclarationFragment) {
                names.add(((VariableDeclarationFragment) obj).getName().getIdentifier());
            }
        }
        fields.add(new FieldData(source, names));
        return super.visit(node);
    }



    @Override
    public boolean visit(MethodDeclaration node) {
        if (node.getName().getIdentifier().equals(targetMethodName)) {
            if (targetArgCount != -1 && node.parameters().size() != targetArgCount) {
                return super.visit(node);
            }
            
            String currentFqcn = computeNodeFqcn(node);
            boolean fqcnMatch = false;
            if (targetFqcn.equals(currentFqcn)) {
                fqcnMatch = true;
            } else {
                String targetSimple = targetFqcn;
                int lastDot = targetFqcn.lastIndexOf('.');
                if (lastDot != -1) {
                    targetSimple = targetFqcn.substring(lastDot + 1);
                }
                if (currentFqcn != null && (currentFqcn.endsWith("." + targetSimple) || currentFqcn.equals(targetSimple))) {
                    // Ensure the package matches if targetFqcn had a package
                    if (lastDot != -1) {
                        String targetPkg = targetFqcn.substring(0, lastDot);
                        if (currentFqcn.startsWith(targetPkg + ".")) {
                            fqcnMatch = true;
                        }
                    } else {
                        fqcnMatch = true;
                    }
                }
            }
            
            
            if (!fqcnMatch) {
                return super.visit(node);
            }
            
            // Found target method
            ExtractedMethod em = new ExtractedMethod();
            em.source = extractSourceDedented(node);
            em.decl = node;
            em.firstLineNumber = cu.getLineNumber(node.getStartPosition());
            extractedMethods.add(em);
            
            // Extract class signature if not already extracted
            ASTNode parent = node.getParent();
            while (parent != null && !(parent instanceof TypeDeclaration) && !(parent instanceof EnumDeclaration) && !(parent instanceof RecordDeclaration)) {
                parent = parent.getParent();
            }
            if (parent instanceof TypeDeclaration) {
                TypeDeclaration td = (TypeDeclaration) parent;
                StringBuilder sig = new StringBuilder();
                if (td.isInterface()) sig.append("interface ");
                else sig.append("class ");
                sig.append(td.getName().getIdentifier());
                if (td.getSuperclassType() != null) {
                    sig.append(" extends ").append(td.getSuperclassType().toString());
                }
                if (!td.superInterfaceTypes().isEmpty()) {
                    sig.append(" implements ");
                    List<String> itfs = new ArrayList<>();
                    for (Object itf : td.superInterfaceTypes()) {
                        itfs.add(itf.toString());
                    }
                    sig.append(String.join(", ", itfs));
                }
                targetClassSignature = sig.toString();
            } else if (parent instanceof EnumDeclaration) {
                targetClassSignature = "enum " + ((EnumDeclaration) parent).getName().getIdentifier();
            } else if (parent instanceof RecordDeclaration) {
                targetClassSignature = "record " + ((RecordDeclaration) parent).getName().getIdentifier();
            }
            
            // Extract method parameters
            for (Object obj : node.parameters()) {
                if (obj instanceof SingleVariableDeclaration) {
                    SingleVariableDeclaration param = (SingleVariableDeclaration) obj;
                    String varName = param.getName().getIdentifier();
                    String typeName = param.getType().toString();
                    localVariableTypes.put(varName, typeName);
                }
            }
            
            // Extract all local variables and MethodInvocations within this method
            node.accept(new ASTVisitor() {
                @Override
                public boolean visit(VariableDeclarationStatement varNode) {
                    String typeName = varNode.getType().toString();
                    for (Object fragObj : varNode.fragments()) {
                        VariableDeclarationFragment frag = (VariableDeclarationFragment) fragObj;
                        String name = frag.getName().getIdentifier();
                        String inferredType = typeName;
                        if ("var".equals(typeName) && frag.getInitializer() != null) {
                            if (frag.getInitializer() instanceof MethodInvocation) {
                                MethodInvocation minv = (MethodInvocation) frag.getInitializer();
                                if (minv.getExpression() == null) {
                                    inferredType = methodReturnTypes.getOrDefault(minv.getName().getIdentifier(), "var");
                                }
                            } else if (frag.getInitializer() instanceof ClassInstanceCreation) {
                                inferredType = ((ClassInstanceCreation) frag.getInitializer()).getType().toString();
                            }
                        }
                        localVariableTypes.put(name, inferredType);
                    }
                    return super.visit(varNode);
                }

                @Override
                public boolean visit(MethodInvocation methodInvocation) {
                    String receiver = methodInvocation.getExpression() != null ? methodInvocation.getExpression().toString() : null;
                    String methodName = methodInvocation.getName().getIdentifier();
                    int argCount = methodInvocation.arguments().size();
                    int lineNumber = cu.getLineNumber(methodInvocation.getStartPosition());
                    methodCalls.add(new MethodCallInfo(receiver, methodName, argCount, lineNumber));
                    return super.visit(methodInvocation);
                }
            });
        }
        return super.visit(node);
    }

    private String extractSource(ASTNode node) {
        int start = node.getStartPosition();
        int length = node.getLength();
        return fileContent.substring(start, start + length);
    }

    private String extractSourceDedented(ASTNode node) {
        int start = node.getStartPosition();
        int length = node.getLength();
        
        // Go back to the very beginning of the line
        int lineStart = -1;
        for (int i = start; i >= 0; i--) {
            if (fileContent.charAt(i) == '\n') {
                lineStart = i + 1;
                break;
            }
        }
        if (lineStart == -1) {
            lineStart = 0;
        }
        
        String rawSource = fileContent.substring(lineStart, start + length);
        String[] lines = rawSource.split("\n", -1);
        
        String firstLinePrefix = fileContent.substring(lineStart, start);
        boolean hasNonWhitespacePrefix = !firstLinePrefix.trim().isEmpty();
        
        int minIndentLength = Integer.MAX_VALUE;
        String minIndent = "";
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // Remove \r if present
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
                lines[i] = line;
            }
            
            if (i == 0 && hasNonWhitespacePrefix) {
                continue;
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            
            String currentIndent = "";
            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);
                if (c == ' ' || c == '\t') {
                    currentIndent += c;
                } else {
                    break;
                }
            }
            if (currentIndent.length() < minIndentLength) {
                minIndentLength = currentIndent.length();
                minIndent = currentIndent;
            }
        }
        
        if (minIndentLength == Integer.MAX_VALUE || minIndentLength == 0) {
            return fileContent.substring(start, start + length);
        }
        
        StringBuilder dedented = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            if (i == 0 && hasNonWhitespacePrefix) {
                String nodeContentOnFirstLine = line.substring(firstLinePrefix.length());
                dedented.append(nodeContentOnFirstLine);
            } else {
                if (line.startsWith(minIndent)) {
                    dedented.append(line.substring(minIndent.length()));
                } else if (line.trim().isEmpty()) {
                    dedented.append("");
                } else {
                    dedented.append(line);
                }
            }
            
            if (i < lines.length - 1) {
                dedented.append("\n");
            }
        }
        return dedented.toString();
    }

    public List<String> getImports() {
        return imports;
    }

    public List<String> getStaticImports() {
        return staticImports;
    }
    public List<FieldData> getFields() {
        return fields;
    }

    public String getTargetClassName() {
        if (!extractedMethods.isEmpty()) {
            MethodDeclaration targetMethodDecl = extractedMethods.get(0).decl;
            ASTNode parent = targetMethodDecl.getParent();
            while (parent != null && !(parent instanceof TypeDeclaration) && !(parent instanceof EnumDeclaration) && !(parent instanceof RecordDeclaration)) {
                parent = parent.getParent();
            }
            if (parent instanceof TypeDeclaration) {
                return ((TypeDeclaration) parent).getName().getIdentifier();
            } else if (parent instanceof EnumDeclaration) {
                return ((EnumDeclaration) parent).getName().getIdentifier();
            } else if (parent instanceof RecordDeclaration) {
                return ((RecordDeclaration) parent).getName().getIdentifier();
            }
        }
        return null;
    }

    public String getTargetClassSignature() {
        return targetClassSignature;
    }

    public String getTargetFqcn() {
        if (!extractedMethods.isEmpty()) {
            MethodDeclaration targetMethodDecl = extractedMethods.get(0).decl;
            StringBuilder sb = new StringBuilder();
            ASTNode parent = targetMethodDecl.getParent();
            while (parent != null) {
                if (parent instanceof TypeDeclaration) {
                    if (sb.length() > 0) sb.insert(0, ".");
                    sb.insert(0, ((TypeDeclaration) parent).getName().getIdentifier());
                } else if (parent instanceof EnumDeclaration) {
                    if (sb.length() > 0) sb.insert(0, ".");
                    sb.insert(0, ((EnumDeclaration) parent).getName().getIdentifier());
                } else if (parent instanceof RecordDeclaration) {
                    if (sb.length() > 0) sb.insert(0, ".");
                    sb.insert(0, ((RecordDeclaration) parent).getName().getIdentifier());
                }
                parent = parent.getParent();
            }
            if (sb.length() > 0) {
                if (cu != null && cu.getPackage() != null) {
                    return cu.getPackage().getName().getFullyQualifiedName() + "." + sb.toString();
                }
                return sb.toString();
            }
        }
        return null;
    }

    public List<MethodCallInfo> getMethodCalls() {
        return methodCalls;
    }

    public Map<String, String> getLocalVariableTypes() {
        return localVariableTypes;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public static class MethodCallInfo {
        public final String receiver;
        public final String methodName;
        public final int argCount;
        public final int lineNumber;

        public MethodCallInfo(String receiver, String methodName, int argCount, int lineNumber) {
            this.receiver = receiver; // Can be null if statically imported or intra-class
            this.methodName = methodName;
            this.argCount = argCount;
            this.lineNumber = lineNumber;
        }
    }
}
