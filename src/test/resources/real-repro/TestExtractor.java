package cryptobot;
import com.extractor.ClassAstVisitor;
import org.eclipse.jdt.core.dom.*;

public class TestExtractor {
    public static void main(String[] args) throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/test/resources/real-repro/MarketView.java")));
        ClassAstVisitor visitor = new ClassAstVisitor(content, "placeOrder");
        
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(content.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.accept(visitor);
        
        System.out.println("Local variables:");
        visitor.getLocalVariableTypes().forEach((k, v) -> System.out.println(k + " -> " + v));
        
        System.out.println("\nMethod calls:");
        for (ClassAstVisitor.MethodCallInfo call : visitor.getMethodCalls()) {
            System.out.println(call.receiver + " . " + call.methodName);
        }
    }
}
