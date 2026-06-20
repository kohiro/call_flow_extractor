import org.eclipse.jdt.core.dom.*;
public class TestAst {
    public static void main(String[] args) {
        String source = "class A { void test(Service service) { worker.execute(() -> { service.order(); }); } }";
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.accept(new ASTVisitor() {
            public boolean visit(MethodInvocation node) {
                System.out.println("MethodInvocation: " + node.getName() + " receiver: " + node.getExpression());
                return true;
            }
        });
    }
}
