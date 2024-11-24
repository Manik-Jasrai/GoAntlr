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
    Map<String, Integer> varVersions; // This map will now store versions without 'v' prefix
 // Add to CFGNode class:
    public Map<String, Map<CFGNode, Integer>> phiOperands = new HashMap<>();
    Set<String> varUses;     // Variables used in this node
    Set<String> definitions; // Variables defined in this node
    Set<CFGNode> dominatedNodes; // Nodes dominated by this node, including successors
    
    CFGNode joinNode;
    
    public CFGNode(String label, String varName) {
        this.id = nextId++;
        this.label = label;
        this.varName = varName != null ? varName : "";
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.domSet = new HashSet<>();
        this.sDomSet = new HashSet<>();
        this.DFSet = new HashSet<>();
        this.dominatedNodes = new HashSet<>();  // Initialize the new field
        this.iDom = null;
        this.varVersions = new HashMap<>();
        this.joinNode = null;
    }
    
    // Updated method to store version without 'v' prefix
    public void updateVarVersion(String var, int version) {
        varVersions.put(var, version); // Store just the number
    }
    
    // Updated method to get version without 'v' prefix
    public int getVarVersion(String var) {
        return varVersions.getOrDefault(var, 0);
    }
    
    // If you need to get the full versioned name for a variable
    public String getVersionedVarName(String var) {
        int version = getVarVersion(var);
        return var + version; // Returns format: "varname0" instead of "varnamev0"
    }
    // Add a static method to reset the ID counter that returns the previous value
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
    private final int graphId;  // Graph identifier
    private Map<String, Integer> ssaCounter; // Track SSA versions per variable
    public CFGBuilder() {
    	this.ssaCounter = new HashMap<>(); // Initialize the SSA version counters
        this.nodeToAst = new HashMap<>();
        this.graphId = CFGNode.resetIdCounter(); // Reset the node counter
    }

    public CFGNode build(ASTNode ast) {
        CFGNode.resetIdCounter(); // Reset node counter before building new CFG
        
        if (ast instanceof ProgramNode) {
            return buildFromProgram((ProgramNode) ast);
        }
        return null;
    }


    private CFGNode buildFromProgram(ProgramNode program) {
        CFGNode entryNode = new CFGNode("PROGRAM_START", null);
        CFGNode currentNode = entryNode;
        CFGNode mainFunctionNode = null;
        
        // Create the program end node
        CFGNode exitNode = new CFGNode("PROGRAM_END", null);
        this.entry = entryNode;
        this.exit = exitNode;
        
        for (ASTNode decl : program.declarations) {
            if (decl instanceof FunctionNode) {
                FunctionNode func = (FunctionNode) decl;
                if (func.name.equals("main")) {
                    mainFunctionNode = buildFromFunction(func);
                    currentNode.addSuccessor(mainFunctionNode);
                    
                    // Find the last node of the main function's CFG
                    CFGNode lastMainNode = findLastExecutionNode(mainFunctionNode);
                    // Connect the last node of main to the program end node
                    lastMainNode.addSuccessor(exitNode);
                }
            }
        }
        
        return entryNode;
    }
    // New helper method to find the last execution node in a function
    private CFGNode findLastExecutionNode(CFGNode startNode) {
        if (startNode == null) return null;
        
        // For empty functions
        if (startNode.successors.isEmpty()) {
            return startNode;
        }
        
        // For functions with a body, traverse to find the last node
        Queue<CFGNode> queue = new LinkedList<>();
        Set<CFGNode> visited = new HashSet<>();
        CFGNode lastNode = startNode;
        
        queue.add(startNode);
        
        while (!queue.isEmpty()) {
            CFGNode current = queue.poll();
            visited.add(current);
            
            // If this node has no successors or all its successors have been visited,
            // it might be a terminal node
            if (current.successors.isEmpty() || 
                current.successors.stream().allMatch(visited::contains)) {
                // Don't consider loop condition nodes as terminal nodes
                if (!current.label.startsWith("FOR_CONDITION") && 
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

    private CFGNode buildFromFunction(FunctionNode func) {
        CFGNode entryNode = new CFGNode("FUNCTION_" + func.name, null);
        
        if (func.body != null) {
            CFGNode bodyNode = buildFromBlock(func.body);
            entryNode.addSuccessor(bodyNode);
            return entryNode;
        }
        
        return entryNode;
    }

    private CFGNode buildFromBlock(BlockNode block) {
        if (block.statements.isEmpty()) {
            return new CFGNode("EMPTY_BLOCK", null);
        }

        CFGNode firstNode = null;
        CFGNode previousNode = null;
        CFGNode lastNode = null;

        for (StatementNode stmt : block.statements) {
            CFGNode currentNode = buildFromStatement(stmt);
            
            if (firstNode == null) {
                firstNode = currentNode;
            }
            
            if (previousNode != null) {
                // Get the last node of the previous statement's subgraph
                CFGNode previousLastNode = getLastNode(previousNode);
                previousLastNode.addSuccessor(currentNode);
            }
            
            previousNode = currentNode;
            lastNode = getLastNode(currentNode);
        }
        
        return firstNode;
    }
    
    class Blockends {
    	CFGNode firstNode;
    	CFGNode lastNode;
		public Blockends(CFGNode firstNode, CFGNode lastNode) {
			super();
			this.firstNode = firstNode;
			this.lastNode = lastNode;
		}
    }
    private Blockends buildFromBlock2(BlockNode block) {
        if (block.statements.isEmpty()) {
            return new Blockends(new CFGNode("EMPTY_BLOCK", null), null);
        }

        CFGNode firstNode = null;
        CFGNode previousNode = null;
        CFGNode lastNode = null;
        

        for (StatementNode stmt : block.statements) {
            CFGNode currentNode = buildFromStatement(stmt);
            
            if (firstNode == null) {
                firstNode = currentNode;
            }
            
            if (previousNode != null) {
                // Get the last node of the previous statement's subgraph
                CFGNode previousLastNode = getLastNode(previousNode);
                previousLastNode.addSuccessor(currentNode);
            }
            
            previousNode = currentNode;
            lastNode = getLastNode(currentNode);
        }
        
        return new Blockends(firstNode, lastNode);
    }
    
    private CFGNode getLastNode(CFGNode node) {
        if (node.label.equals("IF_CONDITION")) {
            return node.joinNode;
        }
        else if (node.label.equals("FOR_INIT")) {
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

    private CFGNode buildFromStatement(StatementNode stmt) {
        if (stmt instanceof IfStatementNode) {
            return buildFromIf((IfStatementNode) stmt);
        } else if (stmt instanceof ForStatementNode) {
            return buildFromFor((ForStatementNode) stmt);
        } else if (stmt instanceof AssignmentNode) {
            return handleAssignmentNode((AssignmentNode) stmt);
        }

        else if (stmt instanceof ExpressionStatementNode) {
            ExpressionStatementNode exprStmt = (ExpressionStatementNode) stmt;
            if (exprStmt.expression instanceof FmtPrintNode) {
                return new CFGNode("PRINT", null);
            }
            return new CFGNode("EXPR", null);
        } else if (stmt instanceof ShortVarDeclNode) {
            ShortVarDeclNode varDecl = (ShortVarDeclNode) stmt;
            String varNames = String.join(", ", varDecl.names); // Join all variable names as a single string
            CFGNode declNode = new CFGNode("VAR_DECL", varNames);
            
            // Update SSA version for each variable declared
            for (String var : varDecl.names) {
                int version = ssaCounter.getOrDefault(var, 0);
                ssaCounter.put(var, version + 1);
                declNode.updateVarVersion(var, version);
            }
            return declNode;
        } 
     
        return new CFGNode("UNKNOWN_STMT", null);
    }
    private CFGNode handleAssignmentNode(AssignmentNode assignmentNode) {
        // Extract variables from the left-hand side (LHS) of the assignment
        List<String> assignedVars = extractAssignedVars(assignmentNode.leftSide);
        CFGNode assignmentCFGNode = new CFGNode("ASSIGNMENT", String.join(", ", assignedVars));

        // Update SSA versions for each variable in the assignment
        for (String var : assignedVars) {
            int currentVersion = ssaCounter.getOrDefault(var, 0);
            int newVersion = currentVersion + 1; // Increment SSA version
            ssaCounter.put(var, newVersion);
            assignmentCFGNode.updateVarVersion(var, newVersion); // Track the updated version in the CFGNode
        }

        return assignmentCFGNode;
    }

    // Helper method to extract assigned variables from the left-hand side
    private List<String> extractAssignedVars(List<ExpressionNode> leftSide) {
        List<String> assignedVars = new ArrayList<>();

        // Traverse each LHS expression to extract variable names
        for (ExpressionNode expr : leftSide) {
            if (expr instanceof IdentifierNode) {
                assignedVars.add(((IdentifierNode) expr).name); // Extract variable name
            } else {
                throw new IllegalArgumentException("Unsupported LHS expression in assignment.");
            }
        }

        return assignedVars;
    }
    

    private CFGNode buildFromIf(IfStatementNode ifStmt) {
        CFGNode conditionNode = new CFGNode("IF_CONDITION", null);
        CFGNode joinNode = new CFGNode("IF_JOIN", null);
        
        Blockends thenRange = buildFromBlock2(ifStmt.thenBlock);
        CFGNode thenNode = thenRange.firstNode;
        conditionNode.addSuccessor(thenNode);
        
        CFGNode thenLastNode = thenRange.lastNode;
        thenLastNode.addSuccessor(joinNode);
        
        if (ifStmt.elseBlock != null) {
        	Blockends elseRange = buildFromBlock2(ifStmt.elseBlock);
            CFGNode elseNode = elseRange.firstNode;
            conditionNode.addSuccessor(elseNode);
            
            CFGNode elseLastNode = elseRange.lastNode;
            elseLastNode.addSuccessor(joinNode);
        } else {
            conditionNode.addSuccessor(joinNode);
        }
        conditionNode.joinNode = joinNode;
        return conditionNode;
    }

    private CFGNode buildFromFor(ForStatementNode forStmt) {
        // Handle initialization statement, which may be a variable declaration or an expression
        CFGNode initNode;
        if (forStmt.init instanceof ShortVarDeclNode) {
            ShortVarDeclNode initVarDecl = (ShortVarDeclNode) forStmt.init;
            String initVarNames = String.join(", ", initVarDecl.names);
            initNode = new CFGNode("FOR_INIT", initVarNames);
        } else if (forStmt.init != null) {
            initNode = buildFromStatement(forStmt.init);
        } else {
            initNode = new CFGNode("FOR_INIT", null);
        }

        CFGNode conditionNode = new CFGNode("FOR_CONDITION", null);
        CFGNode updateNode = new CFGNode("FOR_UPDATE", null);
        CFGNode exitNode = new CFGNode("FOR_EXIT", null);
        
        Blockends bodyRange = buildFromBlock2(forStmt.body); 
        CFGNode bodyNode = bodyRange.firstNode;
        
        initNode.addSuccessor(conditionNode);
        conditionNode.addSuccessor(bodyNode);
        
        CFGNode bodyLastNode = bodyRange.lastNode;
        bodyLastNode.addSuccessor(updateNode);
        
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


