package antlr;
import java.util.*;

class CFGNode {
    private static int nextId = 1;
    final int id;
    String label;
    List<CFGNode> successors;
    List<CFGNode> predecessors;
    String varName;
    Set<CFGNode> domSet;      
    Set<CFGNode> sDomSet;     
    Set<CFGNode> DFSet;       
    CFGNode iDom;       
    Map<String, Integer> varVersions;
    public Map<String, Map<CFGNode, Integer>> phiOperands = new HashMap<>();
    Set<String> varUses;     
    Set<String> definitions;
    Set<CFGNode> dominatedNodes;
    CFGNode joinNode;
    ASTNode astNode;
    
    public CFGNode(String label, String varName, ASTNode astNode) {
        this.id = astNode != null ? astNode.line : nextId++;
        this.label = label;
        this.varName = varName != null ? varName : "";
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.domSet = new HashSet<>();
        this.sDomSet = new HashSet<>();
        this.DFSet = new HashSet<>();
        this.dominatedNodes = new HashSet<>();
        this.iDom = null;
        this.varVersions = new HashMap<>();
        this.joinNode = null;
        this.astNode = astNode;
    }
    
    public void updateVarVersion(String var, int version) {
        varVersions.put(var, version);
    }
    
    public int getVarVersion(String var) {
        return varVersions.getOrDefault(var, 0);
    }
    
    public String getVersionedVarName(String var) {
        int version = getVarVersion(var);
        return var + version;
    }

    public static int resetIdCounter() {
        int oldValue = nextId;
        nextId = 1;
        return oldValue;
    }

    @Override
    public String toString() {
        return varName.isEmpty() ? String.valueOf(id) : (id + " (" + varName + ")");
    }

    public void addSuccessor(CFGNode node) {
        if (!successors.contains(node)) {
            successors.add(node);
            node.predecessors.add(this);
        }
    }

    public Set<CFGNode> getNext() {
        return new HashSet<>(successors);
    }

    public Set<CFGNode> getParent() {
        return new HashSet<>(predecessors);
    }
    
    public List<CFGNode> getPostOrder() {
        List<CFGNode> postOrder = new ArrayList<>();
        Set<CFGNode> visited = new HashSet<>();
        dfsPostOrder(this, postOrder, visited);
        return postOrder;
    }

    private void dfsPostOrder(CFGNode node, List<CFGNode> postOrder, Set<CFGNode> visited) {
        visited.add(node);
        for (CFGNode successor : node.getNext()) {
            if (!visited.contains(successor)) {
                dfsPostOrder(successor, postOrder, visited);
            }
        }
        postOrder.add(node);
    }
}

class CFGBuilder {
    private CFGNode entry;
    private CFGNode exit;
    private Map<CFGNode, ASTNode> nodeToAst;
    private final int graphId;
    private Map<String, Integer> ssaCounter;

    public CFGBuilder() {
        this.ssaCounter = new HashMap<>();
        this.nodeToAst = new HashMap<>();
        this.graphId = CFGNode.resetIdCounter();
    }

    public CFGNode build(ASTNode ast) {
        CFGNode.resetIdCounter();
        if (ast instanceof ProgramNode) {
            return buildFromProgram((ProgramNode) ast);
        }
        return null;
    }

    private CFGNode buildFromProgram(ProgramNode program) {
        CFGNode entryNode = new CFGNode("PROGRAM_START", null, program);
        CFGNode exitNode = new CFGNode("PROGRAM_END", null, program);
        this.entry = entryNode;
        this.exit = exitNode;
        
        CFGNode mainFunctionNode = null;
        for (ASTNode decl : program.declarations) {
            if (decl instanceof FunctionNode) {
                FunctionNode func = (FunctionNode) decl;
                if (func.name.equals("main")) {
                    mainFunctionNode = buildFromFunction(func);
                    entryNode.addSuccessor(mainFunctionNode);
                    CFGNode lastMainNode = findLastExecutionNode(mainFunctionNode);
                    lastMainNode.addSuccessor(exitNode);
                }
            }
        }
        return entryNode;
    }

    private CFGNode findLastExecutionNode(CFGNode startNode) {
        if (startNode == null) return null;
        
        Set<CFGNode> visited = new HashSet<>();
        Queue<CFGNode> queue = new LinkedList<>();
        CFGNode lastNode = startNode;
        queue.add(startNode);
        
        while (!queue.isEmpty()) {
            CFGNode current = queue.poll();
            visited.add(current);
            
            if (current.successors.isEmpty() || 
                current.successors.stream().allMatch(visited::contains)) {
                if (!current.label.startsWith("IF_CONDITION") &&
                    !current.label.equals("FOR_UPDATE")) {
                    lastNode = current;
                }
            }
            
            for (CFGNode successor : current.successors) {
                if (!visited.contains(successor)) {
                    queue.add(successor);
                }
            }
        }
        return lastNode;
    }

    class BlockEnds {
        CFGNode firstNode;
        CFGNode lastNode;
        public BlockEnds(CFGNode firstNode, CFGNode lastNode) {
            this.firstNode = firstNode;
            this.lastNode = lastNode;
        }
    }

    private CFGNode buildFromFunction(FunctionNode func) {
        CFGNode entryNode = new CFGNode("FUNCTION_" + func.name, null, func);
        if (func.body != null) {
            CFGNode bodyNode = buildFromBlock(func.body).firstNode;
            entryNode.addSuccessor(bodyNode);
        }
        return entryNode;
    }

    private BlockEnds buildFromBlock(BlockNode block) {
        if (block.statements.isEmpty()) {
            CFGNode emptyNode = new CFGNode("EMPTY_BLOCK", null, block);
            return new BlockEnds(emptyNode, emptyNode);
        }

        CFGNode firstNode = null;
        CFGNode previousNode = null;
        CFGNode lastNode = null;

        for (StatementNode stmt : block.statements) {
            CFGNode currentNode = buildFromStatement(stmt);
            if (firstNode == null) firstNode = currentNode;
            
            if (previousNode != null) {
                CFGNode previousLastNode = getLastNode(previousNode);
                previousLastNode.addSuccessor(currentNode);
            }
            
            previousNode = currentNode;
            lastNode = getLastNode(currentNode);
        }
        
        return new BlockEnds(firstNode, lastNode);
    }

    private CFGNode buildFromStatement(StatementNode stmt) {
        if (stmt instanceof IfStatementNode) {
            return buildFromIf((IfStatementNode) stmt);
        } else if (stmt instanceof ForStatementNode) {
            return buildFromFor((ForStatementNode) stmt);
        } else if (stmt instanceof AssignmentNode) {
            return handleAssignmentNode((AssignmentNode) stmt);
        } else if (stmt instanceof ExpressionStatementNode) {
            ExpressionStatementNode exprStmt = (ExpressionStatementNode) stmt;
            if (exprStmt.expression instanceof FmtPrintNode) {
                return new CFGNode("PRINT", null, stmt);
            }
            return new CFGNode("EXPR", null, stmt);
        } else if (stmt instanceof ShortVarDeclNode) {
            ShortVarDeclNode varDecl = (ShortVarDeclNode) stmt;
            String varNames = String.join(", ", varDecl.names);
            CFGNode declNode = new CFGNode("VAR_DECL", varNames, stmt);
            for (String var : varDecl.names) {
                int version = ssaCounter.getOrDefault(var, 0);
                ssaCounter.put(var, version + 1);
                declNode.updateVarVersion(var, version);
            }
            return declNode;
        }
        return new CFGNode("UNKNOWN_STMT", null, stmt);
    }

    private CFGNode handleAssignmentNode(AssignmentNode assignmentNode) {
        List<String> assignedVars = extractAssignedVars(assignmentNode.leftSide);
        CFGNode assignmentCFGNode = new CFGNode("ASSIGNMENT", String.join(", ", assignedVars), assignmentNode);

        for (String var : assignedVars) {
            int currentVersion = ssaCounter.getOrDefault(var, 0);
            int newVersion = currentVersion + 1;
            ssaCounter.put(var, newVersion);
            assignmentCFGNode.updateVarVersion(var, newVersion);
        }

        return assignmentCFGNode;
    }

    private List<String> extractAssignedVars(List<ExpressionNode> leftSide) {
        List<String> assignedVars = new ArrayList<>();
        for (ExpressionNode expr : leftSide) {
            if (expr instanceof IdentifierNode) {
                assignedVars.add(((IdentifierNode) expr).name);
            }
        }
        return assignedVars;
    }

    private CFGNode buildFromIf(IfStatementNode ifStmt) {
        CFGNode conditionNode = new CFGNode("IF_CONDITION", null, ifStmt);
        CFGNode joinNode = new CFGNode("IF_JOIN", null, ifStmt);
        
        BlockEnds thenRange = buildFromBlock(ifStmt.thenBlock);
        conditionNode.addSuccessor(thenRange.firstNode);
        thenRange.lastNode.addSuccessor(joinNode);
        
        if (ifStmt.elseBlock != null) {
            BlockEnds elseRange = buildFromBlock(ifStmt.elseBlock);
            conditionNode.addSuccessor(elseRange.firstNode);
            elseRange.lastNode.addSuccessor(joinNode);
        } else {
            conditionNode.addSuccessor(joinNode);
        }
        
        conditionNode.joinNode = joinNode;
        return conditionNode;
    }

    private CFGNode buildFromFor(ForStatementNode forStmt) {
        CFGNode initNode;
        if (forStmt.init instanceof ShortVarDeclNode) {
            ShortVarDeclNode initVarDecl = (ShortVarDeclNode) forStmt.init;
            initNode = new CFGNode("FOR_INIT", String.join(", ", initVarDecl.names), forStmt);
        } else {
            initNode = forStmt.init != null ? buildFromStatement(forStmt.init) : 
                      new CFGNode("FOR_INIT", null, forStmt);
        }

        CFGNode conditionNode = new CFGNode("FOR_CONDITION", null, forStmt);
        CFGNode updateNode = new CFGNode("FOR_UPDATE", null, forStmt);
        CFGNode exitNode = new CFGNode("FOR_EXIT", null, forStmt);
        
        BlockEnds bodyRange = buildFromBlock(forStmt.body);
        initNode.addSuccessor(conditionNode);
        conditionNode.addSuccessor(bodyRange.firstNode);
        bodyRange.lastNode.addSuccessor(updateNode);
        updateNode.addSuccessor(conditionNode);
        conditionNode.addSuccessor(exitNode);
        
        return initNode;
    }

    private CFGNode getLastNode(CFGNode node) {
        if (node.label.equals("IF_CONDITION")) {
            return node.joinNode;
        } else if (node.label.equals("FOR_INIT")) {
            return findForExitNode(node);
        }
        return node;
    }

    private CFGNode findForExitNode(CFGNode forInitNode) {
        Queue<CFGNode> queue = new LinkedList<>();
        Set<CFGNode> visited = new HashSet<>();
        queue.add(forInitNode);
        
        while (!queue.isEmpty()) {
            CFGNode current = queue.poll();
            if (current.label.equals("FOR_EXIT")) {
                return current;
            }
            
            if (!visited.contains(current)) {
                visited.add(current);
                queue.addAll(current.successors);
            }
        }
        return forInitNode;
    }

    public String generateMermaidDiagram(CFGNode start) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");
        List<CFGNode> visited = new ArrayList<>();
        generateMermaidNodes(start, visited, sb);
        return sb.toString();
    }

    private void generateMermaidNodes(CFGNode node, List<CFGNode> visited, StringBuilder sb) {
        if (visited.contains(node)) return;
        visited.add(node);
        sb.append("    ").append(node.id).append("[\"").append(node.label).append("\"]\n");
        for (CFGNode successor : node.successors) {
            sb.append("    ").append(node.id).append(" --> ").append(successor.id).append("\n");
            generateMermaidNodes(successor, visited, sb);
        }
    }
}