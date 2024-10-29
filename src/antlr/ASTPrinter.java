package antlr;

public class ASTPrinter {
    private static String getIndent(int indent) {
        return " ".repeat(indent * 2);
    }
    
    public static void printAST(ASTNode node, int indent) {
        String indentStr = getIndent(indent);
        
        if (node == null) return;
        
        switch (node) {
            case ProgramNode program -> {
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
            }
            case BinaryExpressionNode binExp -> {
                System.out.println(indentStr + "BinaryExpression (line " + node.line + ")");
                System.out.println(indentStr + "  Operator: " + binExp.operator);
                System.out.println(indentStr + "  Left:");
                printAST(binExp.left, indent + 2);
                System.out.println(indentStr + "  Right:");
                printAST(binExp.right, indent + 2);
            }
            case LiteralNode lit -> 
                System.out.println(indentStr + "Literal: " + lit.value);
            case FunctionNode func -> {
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
            case BlockNode block -> {
                System.out.println(indentStr + "Block:");
                for (StatementNode stmt : block.statements) {
                    printAST(stmt, indent + 1);
                }
            }
            case IfStatementNode ifStmt -> {
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
            case ForStatementNode forStmt -> {
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
            case ShortVarDeclNode shortVar -> {
                System.out.println(indentStr + "Short Variable Declaration (line " + node.line + ")");
                System.out.println(indentStr + "  Names: " + String.join(", ", shortVar.names));
                System.out.println(indentStr + "  Values:");
                for (ExpressionNode value : shortVar.values) {
                    printAST(value, indent + 2);
                }
            }
            case CallExpressionNode call -> {
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
            case IdentifierNode id ->
                System.out.println(indentStr + "Identifier: " + id.name + " (line " + node.line + ")");
            case ParameterNode param -> {
                System.out.println(indentStr + "Parameter: " + param.name + " (line " + node.line + ")");
                if (param.type != null) {
                    System.out.println(indentStr + "  Type:");
                    printAST(param.type, indent + 2);
                }
            }
            case ImportNode imp ->
                System.out.println(indentStr + "Import: " + imp.path + " (line " + node.line + ")");
            case ExpressionStatementNode exprStmt -> {
                System.out.println(indentStr + "Expression Statement (line " + node.line + ")");
                printAST(exprStmt.expression, indent + 1);
            }
            case FmtPrintNode fmt -> {
                System.out.println(indentStr + "fmt." + fmt.printType + " Statement (line " + node.line + ")");
                if (!fmt.arguments.isEmpty()) {
                    System.out.println(indentStr + "  Arguments:");
                    for (ExpressionNode arg : fmt.arguments) {
                        printAST(arg, indent + 2);
                    }
                }
            }
            case IncDecExpressionNode incDec -> {
                System.out.println(indentStr + "IncDec Expression (line " + node.line + ")");
                System.out.println(indentStr + "  Operator: " + incDec.operator);
                System.out.println(indentStr + "  Operand:");
                printAST(incDec.operand, indent + 2);
            }
            default ->
                System.out.println(indentStr + "Unknown Node Type: " + node.getClass().getSimpleName() + 
                                 " (line " + node.line + ")");
        }
    }
}