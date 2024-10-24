package antlr;

import java.util.ArrayList;
import java.util.List;

// Base class for all AST nodes
abstract class ASTNode {
    public int line;
    public int column;
    
    public ASTNode(int line, int column) {
        this.line = line;
        this.column = column;
    }
}

// Additional AST node types for statements and functions
class FunctionNode extends ASTNode {
    String name;
    List<ParameterNode> parameters;
    BlockNode body;
    
    public FunctionNode(int line, int column, String name) {
        super(line, column);
        this.name = name;
        this.parameters = new ArrayList<>();
    }
}

class ParameterNode extends ASTNode {
    String name;
    TypeNode type;
    
    public ParameterNode(int line, int column, String name, TypeNode type) {
        super(line, column);
        this.name = name;
        this.type = type;
    }
}

class BlockNode extends ASTNode {
    List<StatementNode> statements;
    
    public BlockNode(int line, int column) {
        super(line, column);
        this.statements = new ArrayList<>();
    }
}

abstract class StatementNode extends ASTNode {
    public StatementNode(int line, int column) {
        super(line, column);
    }
}

class IfStatementNode extends StatementNode {
    ExpressionNode condition;
    BlockNode thenBlock;
    BlockNode elseBlock;
    
    public IfStatementNode(int line, int column) {
        super(line, column);
    }
}

class ForStatementNode extends StatementNode {
    StatementNode init;
    ExpressionNode condition;
    StatementNode post;
    BlockNode body;
    
    public ForStatementNode(int line, int column) {
        super(line, column);
    }
}

class ExpressionStatementNode extends StatementNode {
    ExpressionNode expression;
    
    public ExpressionStatementNode(int line, int column, ExpressionNode expression) {
        super(line, column);
        this.expression = expression;
    }
}

class CallExpressionNode extends ExpressionNode {
    ExpressionNode function;
    List<ExpressionNode> arguments;
    
    public CallExpressionNode(int line, int column) {
        super(line, column);
        this.arguments = new ArrayList<>();
    }
}

class IdentifierNode extends ExpressionNode {
    String name;
    
    public IdentifierNode(int line, int column, String name) {
        super(line, column);
        this.name = name;
    }
}

class ShortVarDeclNode extends StatementNode {
    List<String> names;
    List<ExpressionNode> values;
    
    public ShortVarDeclNode(int line, int column) {
        super(line, column);
        this.names = new ArrayList<>();
        this.values = new ArrayList<>();
    }
}

// Specific AST node types
class ProgramNode extends ASTNode {
    String packageName;
    List<ImportNode> imports;
    List<ASTNode> declarations;
    
    public ProgramNode(int line, int column) {
        super(line, column);
        this.imports = new ArrayList<>();
        this.declarations = new ArrayList<>();
    }
}

class ImportNode extends ASTNode {
    String alias;
    String path;
    
    public ImportNode(int line, int column, String alias, String path) {
        super(line, column);
        this.alias = alias;
        this.path = path;
    }
}

class DeclarationNode extends ASTNode {
    String identifier;
    TypeNode type;
    ExpressionNode initializer;
    
    public DeclarationNode(int line, int column, String identifier, TypeNode type, ExpressionNode initializer) {
        super(line, column);
        this.identifier = identifier;
        this.type = type;
        this.initializer = initializer;
    }
}

class TypeNode extends ASTNode {
    String typeName;
    
    public TypeNode(int line, int column, String typeName) {
        super(line, column);
        this.typeName = typeName;
    }
}

class ExpressionNode extends ASTNode {
    public ExpressionNode(int line, int column) {
        super(line, column);
    }
}

class BinaryExpressionNode extends ExpressionNode {
    String operator;
    ExpressionNode left;
    ExpressionNode right;
    
    public BinaryExpressionNode(int line, int column, String operator, ExpressionNode left, ExpressionNode right) {
        super(line, column);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }
}

class LiteralNode extends ExpressionNode {
    Object value;
    
    public LiteralNode(int line, int column, Object value) {
        super(line, column);
        this.value = value;
    }
}

//First update the FmtPrintNode to extend ExpressionNode instead of StatementNode
class FmtPrintNode extends ExpressionNode {
 List<ExpressionNode> arguments;
 String printType; // "Print", "Println", "Printf"
 
 public FmtPrintNode(int line, int column, String printType) {
     super(line, column);
     this.arguments = new ArrayList<>();
     this.printType = printType;
 }
}

class UnaryExpressionNode extends ExpressionNode {
 String operator;
 ExpressionNode operand;
 
 public UnaryExpressionNode(int line, int column, String operator, ExpressionNode operand) {
     super(line, column);
     this.operator = operator;
     this.operand = operand;
 }
}

class PackageImportNode extends ImportNode {
 String packageName;
 
 public PackageImportNode(int line, int column, String packageName, String path) {
     super(line, column, null, path);
     this.packageName = packageName;
 }
}

class IncDecExpressionNode extends ExpressionNode {
    String operator; // "++" or "--"
    ExpressionNode operand;
    
    public IncDecExpressionNode(int line, int column, String operator, ExpressionNode operand) {
        super(line, column);
        this.operator = operator;
        this.operand = operand;
    }
}

public class GoASTVisitor extends GoParserBaseVisitor<ASTNode> {
    @Override
    public ASTNode visitSourceFile(GoParser.SourceFileContext ctx) {
        ProgramNode program = new ProgramNode(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        
        // Visit package clause
        program.packageName = ctx.packageClause().packageName.getText();
        
        // Visit imports
        if (ctx.importDecl() != null) {
            for (GoParser.ImportDeclContext importCtx : ctx.importDecl()) {
                program.imports.addAll(visitImportDecls(importCtx));
            }
        }
        
        // Visit declarations and functions
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof GoParser.DeclarationContext) {
                DeclarationNode decl = (DeclarationNode) visit(ctx.getChild(i));
                if (decl != null) {
                    program.declarations.add(decl);
                }
            } else if (ctx.getChild(i) instanceof GoParser.FunctionDeclContext) {
                FunctionNode func = (FunctionNode) visit(ctx.getChild(i));
                if (func != null) {
                    program.declarations.add(func);
                }
            }
        }
        
        return program;
    }
    
    private List<ImportNode> visitImportDecls(GoParser.ImportDeclContext ctx) {
        List<ImportNode> imports = new ArrayList<>();
        
        for (GoParser.ImportSpecContext spec : ctx.importSpec()) {
            String alias = spec.alias != null ? spec.alias.getText() : null;
            String path = spec.importPath().string_().getText();
            // Remove quotes from path
            path = path.substring(1, path.length() - 1);
            
            imports.add(new ImportNode(
                spec.getStart().getLine(),
                spec.getStart().getCharPositionInLine(),
                alias,
                path
            ));
        }
        
        return imports;
    }
    
    @Override
    public ASTNode visitVarDecl(GoParser.VarDeclContext ctx) {
        List<DeclarationNode> declarations = new ArrayList<>();
        
        for (GoParser.VarSpecContext spec : ctx.varSpec()) {
            List<String> identifiers = new ArrayList<>();
            for (var id : spec.identifierList().IDENTIFIER()) {
                identifiers.add(id.getText());
            }
            
            TypeNode type = null;
            if (spec.type_() != null) {
                type = (TypeNode) visit(spec.type_());
            }
            
            List<ExpressionNode> initializers = new ArrayList<>();
            if (spec.expressionList() != null) {
                for (var expr : spec.expressionList().expression()) {
                    initializers.add((ExpressionNode) visit(expr));
                }
            }
            
            // Create declaration nodes for each identifier
            for (int i = 0; i < identifiers.size(); i++) {
                ExpressionNode init = i < initializers.size() ? initializers.get(i) : null;
                declarations.add(new DeclarationNode(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    identifiers.get(i),
                    type,
                    init
                ));
            }
        }
        
        return declarations.get(0); // Return first declaration for now
    }
    
    @Override
    public ASTNode visitExpression(GoParser.ExpressionContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visit(ctx.primaryExpr());
        }
        
        if (ctx.getChildCount() == 3) {
            ExpressionNode left = (ExpressionNode) visit(ctx.expression(0));
            ExpressionNode right = (ExpressionNode) visit(ctx.expression(1));
            String operator = ctx.getChild(1).getText();
            
            return new BinaryExpressionNode(
                ctx.getStart().getLine(),
                ctx.getStart().getCharPositionInLine(),
                operator,
                left,
                right
            );
        }
        
        return null;
    }
    
    @Override
    public ASTNode visitBasicLit(GoParser.BasicLitContext ctx) {
        Object value;
        
        if (ctx.integer() != null) {
            value = Integer.parseInt(ctx.integer().getText());
        } else if (ctx.FLOAT_LIT() != null) {
            value = Float.parseFloat(ctx.FLOAT_LIT().getText());
        } else if (ctx.string_() != null) {
            String str = ctx.string_().getText();
            value = str.substring(1, str.length() - 1); // Remove quotes
        } else {
            value = null;
        }

        LiteralNode l = new LiteralNode(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), value);
        
        return l;
    }
    
    @Override
    public ASTNode visitFunctionDecl(GoParser.FunctionDeclContext ctx) {
        FunctionNode func = new FunctionNode(
            ctx.getStart().getLine(),
            ctx.getStart().getCharPositionInLine(),
            ctx.IDENTIFIER().getText()
        );
        
        if (ctx.signature().parameters() != null) {
            for (GoParser.ParameterDeclContext param : ctx.signature().parameters().parameterDecl()) {
                // Add parameters if they exist
                TypeNode type = (TypeNode) visit(param.type_());
                if (param.identifierList() != null) {
                    for (var id : param.identifierList().IDENTIFIER()) {
                        func.parameters.add(new ParameterNode(
                            param.getStart().getLine(),
                            param.getStart().getCharPositionInLine(),
                            id.getText(),
                            type
                        ));
                    }
                }
            }
        }
        
        if (ctx.block() != null) {
            func.body = (BlockNode) visit(ctx.block());
        }
        
        return func;
    }
    
    @Override
    public ASTNode visitBlock(GoParser.BlockContext ctx) {
        BlockNode block = new BlockNode(
            ctx.getStart().getLine(),
            ctx.getStart().getCharPositionInLine()
        );
        
        if (ctx.statementList() != null) {
            for (GoParser.StatementContext stmt : ctx.statementList().statement()) {
                ASTNode node = visit(stmt);
                if (node instanceof StatementNode) {
                    block.statements.add((StatementNode) node);
                }
            }
        }
        
        return block;
    }
    
    @Override
    public ASTNode visitIfStmt(GoParser.IfStmtContext ctx) {
        IfStatementNode ifStmt = new IfStatementNode(
            ctx.getStart().getLine(),
            ctx.getStart().getCharPositionInLine()
        );
        
        // Handle condition
        if (ctx.expression() != null) {
            ifStmt.condition = (ExpressionNode) visit(ctx.expression());
        }
        
        // Handle then block
        if (ctx.block().size() > 0) {
            ifStmt.thenBlock = (BlockNode) visit(ctx.block(0));
        }
        
        // Handle else block
        if (ctx.ELSE() != null && ctx.block().size() > 1) {
            ifStmt.elseBlock = (BlockNode) visit(ctx.block(1));
        }
        
        return ifStmt;
    }
    
    @Override
    public ASTNode visitShortVarDecl(GoParser.ShortVarDeclContext ctx) {
        ShortVarDeclNode shortVar = new ShortVarDeclNode(
            ctx.getStart().getLine(),
            ctx.getStart().getCharPositionInLine()
        );
        
        // Add identifiers
        for (var id : ctx.identifierList().IDENTIFIER()) {
            shortVar.names.add(id.getText());
        }
        
        // Add values
        for (var expr : ctx.expressionList().expression()) {
            shortVar.values.add((ExpressionNode) visit(expr));
        }
        
        return shortVar;
    }
    
    @Override
    public ASTNode visitPrimaryExpr(GoParser.PrimaryExprContext ctx) {
        if (ctx.operand() != null && ctx.operand().operandName() != null) {
            String identifier = ctx.operand().operandName().getText();
            
            // Handle basic identifier
            if (ctx.getChildCount() == 1) {
                return new IdentifierNode(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    identifier
                );
            }
        }
        
        if (ctx.arguments() != null) {
            // Check if this is a fmt.Print call
            if (ctx.primaryExpr() != null && ctx.primaryExpr().getText().startsWith("fmt.")) {
                String printType = ctx.primaryExpr().getText().substring(4); // Remove "fmt."
                FmtPrintNode fmtPrint = new FmtPrintNode(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    printType
                );
                
                // Add arguments
                if (ctx.arguments().expressionList() != null) {
                    for (var expr : ctx.arguments().expressionList().expression()) {
                        fmtPrint.arguments.add((ExpressionNode) visit(expr));
                    }
                }
                
                return new ExpressionStatementNode(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    fmtPrint
                );
            }
            
            // Handle other function calls
            CallExpressionNode call = new CallExpressionNode(
                ctx.getStart().getLine(),
                ctx.getStart().getCharPositionInLine()
            );
            
            call.function = (ExpressionNode) visit(ctx.primaryExpr());
            
            if (ctx.arguments().expressionList() != null) {
                for (var expr : ctx.arguments().expressionList().expression()) {
                    call.arguments.add((ExpressionNode) visit(expr));
                }
            }
            
            return call;
        }
        
        return super.visitPrimaryExpr(ctx);
    }
    
    public ASTNode visitForStmt(GoParser.ForStmtContext ctx) {
        ForStatementNode forStmt = new ForStatementNode(
            ctx.getStart().getLine(),
            ctx.getStart().getCharPositionInLine()
        );
        
        if (ctx.forClause() != null) {
            // Handle for clause (traditional for loop)
            GoParser.ForClauseContext forClause = ctx.forClause();
            if (forClause.initStmt != null) {
                forStmt.init = (StatementNode) visit(forClause.initStmt);
            }
            if (forClause.expression() != null) {
                forStmt.condition = (ExpressionNode) visit(forClause.expression());
            }
            if (forClause.postStmt != null) {
                // Handle post statement which might be increment/decrement
                ASTNode postNode = visit(forClause.postStmt);
                if (postNode instanceof ExpressionNode) {
                    forStmt.post = new ExpressionStatementNode(
                        forClause.postStmt.getStart().getLine(),
                        forClause.postStmt.getStart().getCharPositionInLine(),
                        (ExpressionNode) postNode
                    );
                } else if (postNode instanceof StatementNode) {
                    forStmt.post = (StatementNode) postNode;
                }
            }
        } else if (ctx.expression() != null) {
            // Handle condition-only for loop
            forStmt.condition = (ExpressionNode) visit(ctx.expression());
        }
        
        // Handle body
        if (ctx.block() != null) {
            forStmt.body = (BlockNode) visit(ctx.block());
        }
        
        return forStmt;
    }
    
    @Override
    public ASTNode visitIncDecStmt(GoParser.IncDecStmtContext ctx) {
        ExpressionNode operand = (ExpressionNode) visit(ctx.expression());
        String operator = ctx.getChild(1).getText(); // Get "++" or "--"
        
        IncDecExpressionNode incDec = new IncDecExpressionNode(
            ctx.getStart().getLine(),
            ctx.getStart().getCharPositionInLine(),
            operator,
            operand
        );
        
        // Wrap in ExpressionStatementNode since this is a statement
        return new ExpressionStatementNode(
            ctx.getStart().getLine(),
            ctx.getStart().getCharPositionInLine(),
            incDec
        );
    }
}
