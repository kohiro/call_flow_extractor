package com.extractor;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassAstVisitor extends ASTVisitor {

    private final String targetMethodName;
    private final int targetArgCount;
    private final String fileContent;

    private final List<String> imports = new ArrayList<>();
    private final List<String> staticImports = new ArrayList<>();
    private final List<String> fields = new ArrayList<>();
    private String targetMethodSource = null;
    private MethodDeclaration targetMethodDecl = null;
    
    private final List<MethodCallInfo> methodCalls = new ArrayList<>();
    private final Map<String, String> localVariableTypes = new HashMap<>();
    private boolean isInterface = false;

    public ClassAstVisitor(String fileContent, String targetMethodName, int targetArgCount) {
        this.fileContent = fileContent;
        this.targetMethodName = targetMethodName;
        this.targetArgCount = targetArgCount;
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
        fields.add(extractSourceDedented(node));
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        if (node.getName().getIdentifier().equals(targetMethodName)) {
            // Check argument count if specified
            if (targetArgCount != -1 && node.parameters().size() != targetArgCount) {
                return super.visit(node);
            }
            
            // Found target method
            targetMethodSource = extractSourceDedented(node);
            targetMethodDecl = node;
            
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
                        localVariableTypes.put(frag.getName().getIdentifier(), typeName);
                    }
                    return super.visit(varNode);
                }

                @Override
                public boolean visit(MethodInvocation methodInvocation) {
                    String receiver = methodInvocation.getExpression() != null ? methodInvocation.getExpression().toString() : null;
                    String methodName = methodInvocation.getName().getIdentifier();
                    int argCount = methodInvocation.arguments().size();
                    methodCalls.add(new MethodCallInfo(receiver, methodName, argCount));
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
        int lineStart = start;
        while (lineStart > 0 && fileContent.charAt(lineStart - 1) != '\n') {
            lineStart--;
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

    public List<String> getFields() {
        return fields;
    }

    public String getTargetMethodSource() {
        return targetMethodSource;
    }

    public MethodDeclaration getTargetMethodDecl() {
        return targetMethodDecl;
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

        public MethodCallInfo(String receiver, String methodName, int argCount) {
            this.receiver = receiver; // Can be null if statically imported or intra-class
            this.methodName = methodName;
            this.argCount = argCount;
        }
    }
}
