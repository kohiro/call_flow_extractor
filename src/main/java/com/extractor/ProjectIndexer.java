package com.extractor;

import org.eclipse.jdt.core.dom.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectIndexer {

    public static class EntryMethod {
        public final String fqcn;
        public final String methodName;
        public EntryMethod(String fqcn, String methodName) {
            this.fqcn = fqcn;
            this.methodName = methodName;
        }
    }

    private final List<EntryMethod> entryMethods = new ArrayList<>();
    
    public List<EntryMethod> getEntryMethods() {
        return entryMethods;
    }



    private final Map<String, Path> javaFilesByFqcn = new HashMap<>();
    private final Map<String, Path> javaFilesBySimpleName = new HashMap<>();
    private final Map<Path, String> fqcnByJavaFile = new HashMap<>();
    private final Map<String, List<String>> implementorsMap = new HashMap<>();
    private final Map<String, String> superclassMap = new HashMap<>(); // Child FQCN -> Parent Name
    private final Map<String, List<String>> superInterfacesMap = new HashMap<>(); // Child FQCN -> List of Parent Names
    private List<Path> allXmlFiles;
    private Path projectRootPath;

    public Path getProjectRootPath() {
        return projectRootPath;
    }

    public List<String> getSuperInterfaces(String fqcn) {
        return superInterfacesMap.getOrDefault(fqcn, new ArrayList<>());
    }

    public void indexProject(String rootPath) throws IOException {
        this.projectRootPath = Path.of(rootPath).toAbsolutePath().normalize();
        
        List<Path> allFiles;
        try (Stream<Path> stream = Files.walk(projectRootPath)) {
            allFiles = stream.collect(Collectors.toList());
        }
            
            allXmlFiles = allFiles.stream()
                    .filter(p -> p.toString().endsWith(".xml"))
                    .collect(Collectors.toList());
                    
            List<Path> javaFiles = allFiles.stream()
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
                    
            for (Path javaFile : javaFiles) {
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

                    cu.accept(new ASTVisitor() {
                        String pkgName = cu.getPackage() != null ? cu.getPackage().getName().getFullyQualifiedName() : "";
                        
                        @Override
                        public boolean visit(TypeDeclaration node) {
                            String simpleName = node.getName().getIdentifier();
                            String fqcn = pkgName.isEmpty() ? simpleName : pkgName + "." + simpleName;
                            
                            String fileName = javaFile.getFileName().toString();
                            String expectedClassName = fileName.substring(0, fileName.lastIndexOf('.'));
                            
                            javaFilesBySimpleName.put(simpleName, javaFile);
                            javaFilesByFqcn.put(fqcn, javaFile);
                            
                            // Map the java file to its primary FQCN
                            if (simpleName.equals(expectedClassName) || !fqcnByJavaFile.containsKey(javaFile)) {
                                fqcnByJavaFile.put(javaFile, fqcn);
                            }
                            
                            Type superclass = node.getSuperclassType();
                            if (superclass != null) {
                                String parentName = addImplementor(superclass, fqcn);
                                if (parentName != null) {
                                    superclassMap.put(fqcn, parentName);
                                }
                            }
                            for (Object superInterface : node.superInterfaceTypes()) {
                                String parentName = addImplementor((Type) superInterface, fqcn);
                                if (parentName != null) {
                                    superInterfacesMap.computeIfAbsent(fqcn, k -> new ArrayList<>()).add(parentName);
                                }
                            }
                            return super.visit(node);
                        }

                        @Override
                        public boolean visit(MethodDeclaration node) {
                            boolean hasMapping = false;
                            for (Object mod : node.modifiers()) {
                                if (mod instanceof Annotation) {
                                    String annotName = ((Annotation) mod).getTypeName().getFullyQualifiedName();
                                    if (annotName.endsWith("Mapping")) {
                                        hasMapping = true;
                                        break;
                                    }
                                }
                            }
                            if (hasMapping) {
                                ASTNode parent = node.getParent();
                                if (parent instanceof TypeDeclaration) {
                                    TypeDeclaration typeNode = (TypeDeclaration) parent;
                                    String simpleName = typeNode.getName().getIdentifier();
                                    String fqcn = pkgName.isEmpty() ? simpleName : pkgName + "." + simpleName;
                                    entryMethods.add(new EntryMethod(fqcn, node.getName().getIdentifier()));
                                }
                            }
                            return super.visit(node);
                        }
                    });
                } catch (Exception e) {
                    // Ignore parsing errors for individual files during indexing
                }
            }
    }

    private String addImplementor(Type parentType, String childFqcn) {
        String parentName;
        if (parentType.isSimpleType()) {
            parentName = ((SimpleType) parentType).getName().getFullyQualifiedName();
        } else if (parentType.isParameterizedType()) {
            Type baseType = ((ParameterizedType) parentType).getType();
            if (baseType.isSimpleType()) {
                parentName = ((SimpleType) baseType).getName().getFullyQualifiedName();
            } else {
                parentName = baseType.toString();
            }
        } else {
            parentName = parentType.toString();
        }
        
        implementorsMap.computeIfAbsent(parentName, k -> new ArrayList<>()).add(childFqcn);
        
        if (parentName.contains(".")) {
            String simple = parentName.substring(parentName.lastIndexOf('.') + 1);
            implementorsMap.computeIfAbsent(simple, k -> new ArrayList<>()).add(childFqcn);
        }
        return parentName;
    }

    public Path getJavaFile(String fqcn) {
        return javaFilesByFqcn.get(fqcn);
    }
    
    public Path getJavaFileBySimpleName(String simpleName) {
        return javaFilesBySimpleName.get(simpleName);
    }

    public String getFqcn(Path file) {
        return fqcnByJavaFile.get(file);
    }

    public List<Path> getAllXmlFiles() {
        return allXmlFiles;
    }

    public List<String> getImplementors(String interfaceOrSuperclassName) {
        String simpleName = interfaceOrSuperclassName.contains(".") ? 
            interfaceOrSuperclassName.substring(interfaceOrSuperclassName.lastIndexOf(".") + 1) : 
            interfaceOrSuperclassName;
            
        Set<String> result = new HashSet<>();
        collectImplementorsRecursive(simpleName, result);
        return new ArrayList<>(result);
    }
    
    private void collectImplementorsRecursive(String parentName, Set<String> result) {
        List<String> direct = implementorsMap.getOrDefault(parentName, new ArrayList<>());
        for (String childFqcn : direct) {
            if (result.add(childFqcn)) {
                String childSimpleName = childFqcn.contains(".") ? childFqcn.substring(childFqcn.lastIndexOf(".") + 1) : childFqcn;
                collectImplementorsRecursive(childSimpleName, result);
            }
        }
    }
    
    public String getSuperclass(String fqcn) {
        return superclassMap.get(fqcn);
    }
}
