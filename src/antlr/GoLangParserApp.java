package antlr;

import java.io.FileInputStream;
import java.util.Map;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CommonTokenStream;

public class GoLangParserApp {
    public static void main(String[] args) throws Exception {
        // Assume we have a GoLangLexer and GoLangParser generated from the Go grammar
    	CharStream input = CharStreams.fromStream(new FileInputStream("D:\\Mini-Project\\GoAntlr\\src\\tests\\ex.go"));

        // Create the lexer and parser
        GoLexer lexer = new GoLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GoParser parser = new GoParser(tokens);

        // Parse the input and get the parse tree
        ParseTree tree = parser.sourceFile();  // Start parsing from the sourceFile rule

        GoLangVisitor visitor = new GoLangVisitor();
        visitor.visit(tree);  // Traverse the tree with the visitor

        // Get the AST representation
        Map<String, Object> ast = visitor.getAST();
        System.out.println("AST: " + ast);
    }
}
