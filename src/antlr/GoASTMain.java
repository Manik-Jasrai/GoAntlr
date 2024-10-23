package antlr;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.*;


public class GoASTMain {
    public static void main(String[] args) throws IOException {
    	CharStream input = CharStreams.fromFileName("D:\\Mini-Project\\GoAntlr\\src\\tests\\ex.go");
    	
    	
    	 String goCode = 
    	            "package main\n\n" +
    	            "import (\n" +
    	            "    \"fmt\"\n" +
    	            ")\n\n" +
    	            "func main() {\n" +
    	            "    number := 5\n" +
    	            "    \n" +
    	            "    if number % 2 == 0 {\n" +
    	            "        fmt.Println(number, \"is even\")\n" +
    	            "    } else {\n" +
    	            "        fmt.Println(number, \"is odd\")\n" +
    	            "    }\n" +
    	            "    \n" +
    	            "    fmt.Println(\"Counting from 1 to 5:\")\n" +
    	            "    for i := 1; i <= 5; i++ {\n" +
    	            "        fmt.Println(i)\n" +
    	            "    }\n" +
    	            "}\n";

        // Create lexer and parser
        GoLexer lexer = new GoLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GoParser parser = new GoParser(tokens);
        
        // Parse the input
        ParseTree tree = parser.sourceFile();
        
        // Create and use our visitor
        GoASTVisitor visitor = new GoASTVisitor();
        ASTNode ast = visitor.visit(tree);
        
        // Print the AST structure
        printAST(ast, 0);
    }
    
    private static void printAST(ASTNode node, int indent) {
        String indentStr = " ".repeat(indent * 2);
        
        if (node == null) return;
        
        if (node instanceof ProgramNode) {
            ProgramNode program = (ProgramNode) node;
            System.out.println(indentStr + "Program (line " + node.line + ")");
            System.out.println(indentStr + "  Package: " + program.packageName);
            
            System.out.println(indentStr + "  Imports:");
            for (ImportNode imp : program.imports) {
                printAST(imp, indent + 2);
            }
            
            System.out.println(indentStr + "  Declarations:");
            for (ASTNode decl : program.declarations) {
                printAST(decl, indent + 2);
            }
        } else if (node instanceof BinaryExpressionNode) {
            BinaryExpressionNode binExp = (BinaryExpressionNode) node;
            System.out.println(indentStr + "BinaryExpression (line " + node.line + ")");
            System.out.println(indentStr + "  Operator: " + binExp.operator);
            System.out.println(indentStr + "  Left:");
            printAST(binExp.left, indent + 2);
            System.out.println(indentStr + "  Right:");
            printAST(binExp.right, indent + 2);
        }
        else if (node instanceof LiteralNode) {
            LiteralNode lit = (LiteralNode) node;
            System.out.println(indentStr + "Literal: " + lit.value);
        }
        else if (node instanceof FunctionNode) {
            FunctionNode func = (FunctionNode) node;
            System.out.println(indentStr + "Function: " + func.name + " (line " + node.line + ")");
            if (!func.parameters.isEmpty()) {
                System.out.println(indentStr + "  Parameters:");
                for (ParameterNode param : func.parameters) {
                    printAST(param, indent + 2);
                }
            }
            if (func.body != null) {
                System.out.println(indentStr + "  Body:");
                printAST(func.body, indent + 2);
            }
        }
        else if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            System.out.println(indentStr + "Block:");
            for (StatementNode stmt : block.statements) {
                printAST(stmt, indent + 1);
            }
        }
        else if (node instanceof IfStatementNode) {
            IfStatementNode ifStmt = (IfStatementNode) node;
            System.out.println(indentStr + "If Statement (line " + node.line + ")");
            System.out.println(indentStr + "  Condition:");
            printAST(ifStmt.condition, indent + 2);
            System.out.println(indentStr + "  Then:");
            printAST(ifStmt.thenBlock, indent + 2);
            if (ifStmt.elseBlock != null) {
                System.out.println(indentStr + "  Else:");
                printAST(ifStmt.elseBlock, indent + 2);
            }
        }
        else if (node instanceof ForStatementNode) {
            ForStatementNode forStmt = (ForStatementNode) node;
            System.out.println(indentStr + "For Statement (line " + node.line + ")");
            if (forStmt.init != null) {
                System.out.println(indentStr + "  Init:");
                printAST(forStmt.init, indent + 2);
            }
            if (forStmt.condition != null) {
                System.out.println(indentStr + "  Condition:");
                printAST(forStmt.condition, indent + 2);
            }
            if (forStmt.post != null) {
                System.out.println(indentStr + "  Post:");
                printAST(forStmt.post, indent + 2);
            }
            System.out.println(indentStr + "  Body:");
            printAST(forStmt.body, indent + 2);
        }
        else if (node instanceof ShortVarDeclNode) {
            ShortVarDeclNode shortVar = (ShortVarDeclNode) node;
            System.out.println(indentStr + "Short Variable Declaration (line " + node.line + ")");
            System.out.println(indentStr + "  Names: " + String.join(", ", shortVar.names));
            System.out.println(indentStr + "  Values:");
            for (ExpressionNode value : shortVar.values) {
                printAST(value, indent + 2);
            }
        }
        else if (node instanceof CallExpressionNode) {
            CallExpressionNode call = (CallExpressionNode) node;
            System.out.println(indentStr + "Call Expression (line " + node.line + ")");
            System.out.println(indentStr + "  Function:");
            printAST(call.function, indent + 2);
            if (!call.arguments.isEmpty()) {
                System.out.println(indentStr + "  Arguments:");
                for (ExpressionNode arg : call.arguments) {
                    printAST(arg, indent + 2);
                }
            }
        }
        else if (node instanceof IdentifierNode) {
            IdentifierNode id = (IdentifierNode) node;
            System.out.println(indentStr + "Identifier: " + id.name + " (line " + node.line + ")");
        }
        else if (node instanceof ParameterNode) {
            ParameterNode param = (ParameterNode) node;
            System.out.println(indentStr + "Parameter: " + param.name + " (line " + node.line + ")");
            if (param.type != null) {
                System.out.println(indentStr + "  Type:");
                printAST(param.type, indent + 2);
            }
        }
        else if (node instanceof ImportNode) {
            ImportNode imp = (ImportNode) node;
            System.out.println(indentStr + "Import: " + imp.path + " (line " + node.line + ")");
        }
        else if (node instanceof ExpressionStatementNode) {
            ExpressionStatementNode exprStmt = (ExpressionStatementNode) node;
            System.out.println(indentStr + "Expression Statement (line " + node.line + ")");
            printAST(exprStmt.expression, indent + 1);
        }
        else {
            System.out.println(indentStr + "Unknown Node Type: " + node.getClass().getSimpleName() + 
                             " (line " + node.line + ")");
        }
    }
}