package antlr;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.*;

import javax.swing.SwingUtilities;


public class GoASTMain {
    public static void main(String[] args) throws IOException {
    	CharStream input = CharStreams.fromFileName("/Users/karandeepsingh/git/GoAntlr/src/tests/ex.go");

        // Create lexer and parser
        GoLexer lexer = new GoLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GoParser parser = new GoParser(tokens);
        
        // Parse the input
        ParseTree tree = parser.sourceFile();
        
        // Create and use our visitor
        GoASTVisitor visitor = new GoASTVisitor();
        ASTNode ast = visitor.visit(tree);
        
        ASTPrinter.printAST(ast, 0);
        generateAndPrintCFG(ast);
    }
    
 // Add this to your GoASTMain class:
    private static void generateAndPrintCFG(ASTNode ast) {
        CFGBuilder builder = new CFGBuilder();
        CFGNode cfg = builder.build(ast);
        
        // Create and show the GUI
        SwingUtilities.invokeLater(() -> {
            CFGVisualizer visualizer = new CFGVisualizer(cfg);
            visualizer.setVisible(true);
        });
        
        // Also print the text version for reference
        String mermaidDiagram = builder.generateMermaidDiagram(cfg);
        System.out.println("\nControl Flow Graph (Text Version):");
        System.out.println(mermaidDiagram);
    }
}
    
    