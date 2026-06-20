package com.extractor;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassAstVisitor extends ASTVisitor {

    private final String targetMethodName;
    private final String fileContent;

    private final List<String> imports = new ArrayList<>();
    private final List<String> staticImports = new ArrayList<>();
    private final List<String> fields = new ArrayList<>();
    private String targetMethodSource = null;
    private MethodDeclaration targetMethodDecl = null;
    
    private final List<MethodCallInfo> methodCalls = new ArrayList<>();
    private final Map<String, String> localVariableTypes = new HashMap<>();
    private boolean isInterface = false;

    public ClassAstVisitor(String fileContent, String targetMethodName) {
        this.fileContent = fileContent;
        this.targetMethodName = targetMethodName;
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
        fields.add(extractSource(node));
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        if (node.getName().getIdentifier().equals(targetMethodName)) {
            // Found target method
            targetMethodSource = extractSource(node);
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
                    methodCalls.add(new MethodCallInfo(receiver, methodName));
                    return super.visit(methodInvocation);
                }
            });
        }
        return super.visit(node);
    }

    private String extractSource(ASTNode node) {
        int start = node.getStartPosition();
        int length = node.getLength();
        
        // Find the start of the line to include indentation
        int lineStart = start;
        while (lineStart > 0 && fileContent.charAt(lineStart - 1) != '\n') {
            if (!Character.isWhitespace(fileContent.charAt(lineStart - 1))) {
                break; // Stop if we hit non-whitespace
            }
            lineStart--;
        }
        
        return fileContent.substring(lineStart, start + length);
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

        public MethodCallInfo(String receiver, String methodName) {
            this.receiver = receiver; // Can be null if statically imported or intra-class
            this.methodName = methodName;
        }
    }
}
