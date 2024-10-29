package antlr;
import java.util.*;

class CFGNode {
    private static int nextId = 1;
    int id;
    String label;
    List<CFGNode> successors;
    List<CFGNode> predecessors;

    // Attributes for dominance calculations
    Set<CFGNode> domSet;      // Dominance Set
    Set<CFGNode> sDomSet;     // Strict Dominance Set
    Set<CFGNode> DFSet;       // Dominance Frontier Set
    CFGNode iDom;             // Immediate Dominator

    public CFGNode(String label) {
        this(nextId++, label);
    }

    public CFGNode(int id, String label) {
        this.id = id;
        this.label = label;
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();

        // Initialize sets for dominance calculations
        this.domSet = new HashSet<>();
        this.sDomSet = new HashSet<>();
        this.DFSet = new HashSet<>();
        this.iDom = null;
    }

    public void addSuccessor(CFGNode node) {
        if (!successors.contains(node)) {
            successors.add(node);
            node.predecessors.add(this);
        }
    }

    // Getter for successors
    public Set<CFGNode> getNext() {
        return new HashSet<>(successors);
    }

    // Getter for predecessors
    public Set<CFGNode> getParent() {
        return new HashSet<>(predecessors);
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}

class CFGBuilder {
    private CFGNode entry;
    private CFGNode exit;
    private Map<CFGNode, ASTNode> nodeToAst;

    public CFGBuilder() {
        this.nodeToAst = new HashMap<>();
    }

    public CFGNode build(ASTNode ast) {
        if (ast instanceof ProgramNode) {
            return buildFromProgram((ProgramNode) ast);
        }
        return null;
    }

    private CFGNode buildFromProgram(ProgramNode program) {
        CFGNode entryNode = new CFGNode("PROGRAM_START");
        CFGNode currentNode = entryNode;
        
        for (ASTNode decl : program.declarations) {
            if (decl instanceof FunctionNode) {
                FunctionNode func = (FunctionNode) decl;
                if (func.name.equals("main")) {
                    CFGNode functionCFG = buildFromFunction(func);
                    currentNode.addSuccessor(functionCFG);
                    currentNode = functionCFG;
                }
            }
        }
        
        CFGNode exitNode = new CFGNode("PROGRAM_END");
        currentNode.addSuccessor(exitNode);
        
        return entryNode;
    }

    private CFGNode buildFromFunction(FunctionNode func) {
        CFGNode entryNode = new CFGNode("FUNCTION_" + func.name);
        
        if (func.body != null) {
            CFGNode bodyNode = buildFromBlock(func.body);
            entryNode.addSuccessor(bodyNode);
            return entryNode;
        }
        
        return entryNode;
    }

    private CFGNode buildFromBlock(BlockNode block) {
        CFGNode currentNode = null;
        CFGNode firstNode = null;
        
        for (StatementNode stmt : block.statements) {
            CFGNode stmtNode = buildFromStatement(stmt);
            
            if (firstNode == null) {
                firstNode = stmtNode;
            }
            
            if (currentNode != null) {
                currentNode.addSuccessor(stmtNode);
            }
            
            currentNode = stmtNode;
        }
        
        return firstNode != null ? firstNode : new CFGNode("EMPTY_BLOCK");
    }

    private CFGNode buildFromStatement(StatementNode stmt) {
        if (stmt instanceof IfStatementNode) {
            return buildFromIf((IfStatementNode) stmt);
        } else if (stmt instanceof ForStatementNode) {
            return buildFromFor((ForStatementNode) stmt);
        } else if (stmt instanceof ExpressionStatementNode) {
            ExpressionStatementNode exprStmt = (ExpressionStatementNode) stmt;
            if (exprStmt.expression instanceof FmtPrintNode) {
                return new CFGNode("PRINT");
            }
            return new CFGNode("EXPR");
        } else if (stmt instanceof ShortVarDeclNode) {
            return new CFGNode("VAR_DECL");
        }
        
        return new CFGNode("UNKNOWN_STMT");
    }

    private CFGNode buildFromIf(IfStatementNode ifStmt) {
        CFGNode conditionNode = new CFGNode("IF_CONDITION");
        CFGNode joinNode = new CFGNode("IF_JOIN");
        
        // Build then branch
        CFGNode thenNode = buildFromBlock(ifStmt.thenBlock);
        conditionNode.addSuccessor(thenNode);
        thenNode.addSuccessor(joinNode);
        
        // Build else branch if it exists
        if (ifStmt.elseBlock != null) {
            CFGNode elseNode = buildFromBlock(ifStmt.elseBlock);
            conditionNode.addSuccessor(elseNode);
            elseNode.addSuccessor(joinNode);
        } else {
            conditionNode.addSuccessor(joinNode);
        }
        
        return conditionNode;
    }

    private CFGNode buildFromFor(ForStatementNode forStmt) {
        CFGNode initNode = new CFGNode("FOR_INIT");
        CFGNode conditionNode = new CFGNode("FOR_CONDITION");
        CFGNode bodyNode = buildFromBlock(forStmt.body);
        CFGNode updateNode = new CFGNode("FOR_UPDATE");
        CFGNode exitNode = new CFGNode("FOR_EXIT");
        
        // Connect the nodes
        initNode.addSuccessor(conditionNode);
        conditionNode.addSuccessor(bodyNode);
        bodyNode.addSuccessor(updateNode);
        updateNode.addSuccessor(conditionNode);
        conditionNode.addSuccessor(exitNode);
        
        return initNode;
    }

    public String generateMermaidDiagram(CFGNode start) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");
        
        // Use a list to track visited nodes
        List<CFGNode> visited = new ArrayList<>();
        generateMermaidNodes(start, visited, sb);
        
        return sb.toString();
    }

    private void generateMermaidNodes(CFGNode node, List<CFGNode> visited, StringBuilder sb) {
        if (visited.contains(node)) {
            return;
        }
        
        visited.add(node);
        
        // Add node definition
        sb.append("    ").append(node.id).append("[\"").append(node.label).append("\"]\n");
        
        // Add edges to successors
        for (CFGNode successor : node.successors) {
            sb.append("    ").append(node.id).append(" --> ").append(successor.id).append("\n");
            generateMermaidNodes(successor, visited, sb);
        }
    }
}

