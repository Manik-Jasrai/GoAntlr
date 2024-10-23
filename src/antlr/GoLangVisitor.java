package antlr;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import java.util.HashMap;
import java.util.Map;

// Extend the generated GoParserBaseVisitor to implement custom logic
public class GoLangVisitor extends GoParserBaseVisitor<Map<String, Object>> {

    // A map to store the AST representation
    private Map<String, Object> ast = new HashMap<>();

    // Visit a package clause and add it to the AST
    @Override
    public Map<String, Object> visitPackageClause(GoParser.PackageClauseContext ctx) {
        Map<String, Object> packageNode = new HashMap<>();
        packageNode.put("packageName", ctx.IDENTIFIER().getText());
        ast.put("package", packageNode);
        return packageNode;
    }

    // Visit import declarations and add them to the AST
    @Override
    public Map<String, Object> visitImportDecl(GoParser.ImportDeclContext ctx) {
        Map<String, Object> importNode = new HashMap<>();
        importNode.put("imports", ctx.importSpec().stream()
                .map(spec -> spec.importPath().string_().getText())
                .toArray(String[]::new));
        ast.put("imports", importNode);
        return importNode;
    }

    // Visit a function declaration and add it to the AST
    @Override
    public Map<String, Object> visitFunctionDecl(GoParser.FunctionDeclContext ctx) {
        Map<String, Object> functionNode = new HashMap<>();
        functionNode.put("functionName", ctx.IDENTIFIER().getText());
        functionNode.put("parameters", ctx.signature().parameters().getText());
        functionNode.put("body", ctx.block() != null ? ctx.block().getText() : "");
        ast.put("function", functionNode);
        return functionNode;
    }

    // Visit a variable declaration and add it to the AST
    @Override
    public Map<String, Object> visitVarDecl(GoParser.VarDeclContext ctx) {
        Map<String, Object> varNode = new HashMap<>();
        varNode.put("variables", ctx.varSpec().stream()
                .map(spec -> spec.identifierList().getText())
                .toArray(String[]::new));
        ast.put("variables", varNode);
        return varNode;
    }

    // Visit statements and add them to the AST
    @Override
    public Map<String, Object> visitStatement(GoParser.StatementContext ctx) {
        Map<String, Object> statementNode = new HashMap<>();
        statementNode.put("statement", ctx.getText());
        ast.put("statement", statementNode);
        return statementNode;
    }

    // Get the final AST after visiting the tree
    public Map<String, Object> getAST() {
        return ast;
    }
}
