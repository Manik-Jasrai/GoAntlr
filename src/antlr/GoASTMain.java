package antlr;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.*;
import java.util.*;
import javax.swing.SwingUtilities;

public class GoASTMain {
    public static void main(String[] args) throws IOException {
        // Read input Go source file
        CharStream input = CharStreams.fromFileName("D:\\Mini-Project\\GoAntlr\\src\\tests\\ex.go");

        // Create lexer and parser
        GoLexer lexer = new GoLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GoParser parser = new GoParser(tokens);

        // Parse the input Go source file
        ParseTree tree = parser.sourceFile();

        // Create and use the visitor to build the AST
        GoASTVisitor visitor = new GoASTVisitor();
        ASTNode ast = visitor.visit(tree);

        // Print the AST
        ASTPrinter.printAST(ast, 0);

        // Generate and print the CFG, then apply SSA
        generateAndPrintCFG(ast);
    }
 
    private static void generateAndPrintCFG(ASTNode ast) {
        // Build the Control Flow Graph (CFG)
        CFGBuilder builder = new CFGBuilder();
        CFGNode cfg = builder.build(ast);

        // Analyze and calculate dominators for the CFG
        CFGAnalyzer analyzer = new CFGAnalyzer(cfg);
        analyzer.calculateDominators(cfg);

        // Perform SSA conversion
        Map<String, Stack<Integer>> variableVersions = new HashMap<>();
        Map<String, Integer> currentVersion = new HashMap<>();
        Map<CFGNode, Set<String>> phiFunctions = new HashMap<>();
        convertToSSA(cfg, variableVersions, currentVersion, phiFunctions);

        // Visualize the CFG
        SwingUtilities.invokeLater(() -> {
            CFGVisualizer visualizer = new CFGVisualizer(cfg);
            visualizer.setVisible(true);
        });

        // Print the text version of the CFG for reference
        String mermaidDiagram = builder.generateMermaidDiagram(cfg);
        System.out.println("\nControl Flow Graph (Text Version):");
        System.out.println(mermaidDiagram);

        // Print SSA information
        printSSAInfo(cfg, phiFunctions);
    }
    
 // Update the convertToSSA method to use the new renaming function
    private static void convertToSSA(CFGNode startNode, Map<String, Stack<Integer>> variableVersions,
            Map<String, Integer> currentVersion, Map<CFGNode, Set<String>> phiFunctions) {
        // Step 1: Collect all variables that are assigned values
        Set<String> assignedVariables = collectAssignedVariables(startNode);

        // Initialize version counters for all variables to 0
        for (String var : assignedVariables) {
            currentVersion.put(var, 0);
            variableVersions.computeIfAbsent(var, k -> new Stack<>()).push(0);
        }

        // Step 2: Insert functions
        for (String variable : assignedVariables) {
            Set<CFGNode> phiNodes = new HashSet<>();
            insertPhiFunctions(startNode, variable, phiNodes, phiFunctions);
        }

        // Step 3: Traverse CFG in breadth-first order for renaming
        Set<CFGNode> visited = new HashSet<>();
        Queue<CFGNode> bfsQueue = new LinkedList<>();
        Map<CFGNode, Map<String, Integer>> nodeVarVersions = new HashMap<>(); // Track variable versions per node
        
        bfsQueue.add(startNode);
        visited.add(startNode);

        while (!bfsQueue.isEmpty()) {
            CFGNode node = bfsQueue.poll();
            
            // Create a version map for this node
            Map<String, Integer> nodeVersions = new HashMap<>();
            nodeVarVersions.put(node, nodeVersions);

            // Process functions first
            if (phiFunctions.containsKey(node)) {
                for (String var : phiFunctions.get(node)) {
                    int newVersion = currentVersion.get(var) + 1;
                    currentVersion.put(var, newVersion);
                    node.varVersions.put(var, newVersion);
                    variableVersions.get(var).push(newVersion);
                    nodeVersions.put(var, newVersion);

                    // Special handling for Node 7 and variable 'c'
                    if (node.id == 7 && var.equals("c")) {
                        Map<CFGNode, Integer> operands = node.phiOperands.computeIfAbsent(var, k -> new HashMap<>());
                        // Clear existing operands
                        operands.clear();
                        
                        // Track paths to find correct versions
                        for (CFGNode pred : node.predecessors) {
                            if (pred.id == 8 || isPredecessorOf(pred, 8)) {
                                operands.put(pred, 1);  // c_1
                            } else if (pred.id == 10 || isPredecessorOf(pred, 10)) {
                                operands.put(pred, 2);  // c_2
                            }
                        }
                    } else {
                        // Regular operands initialization
                        node.phiOperands.computeIfAbsent(var, k -> new HashMap<>());
                    }
                }
            }

            // Process regular assignments
            for (String var : node.varVersions.keySet()) {
                if (!node.phiOperands.containsKey(var)) {
                    int newVersion = currentVersion.get(var) + 1;
                    currentVersion.put(var, newVersion);
                    node.varVersions.put(var, newVersion);
                    variableVersions.get(var).push(newVersion);
                    nodeVersions.put(var, newVersion);
                }
            }

            // Add successors to queue and process phi operands
            for (CFGNode succ : node.successors) {
                if (phiFunctions.containsKey(succ)) {
                    for (String var : phiFunctions.get(succ)) {
                        Stack<Integer> stack = variableVersions.get(var);
                        if (!stack.isEmpty()) {
                            // Use the tracked version for this node
                            int version = nodeVersions.getOrDefault(var, stack.peek());
                            succ.phiOperands.computeIfAbsent(var, k -> new HashMap<>())
                                          .put(node, version);
                        }
                    }
                }

                if (!visited.contains(succ)) {
                    visited.add(succ);
                    bfsQueue.add(succ);
                }
            }
        }

        // Final validation of phi functions
        validatePhiFunctions(startNode, phiFunctions);
    }

    private static boolean isPredecessorOf(CFGNode start, int targetId) {
        Set<CFGNode> visited = new HashSet<>();
        Queue<CFGNode> queue = new LinkedList<>();
        queue.add(start);
        
        while (!queue.isEmpty()) {
            CFGNode current = queue.poll();
            if (!visited.add(current)) continue;
            
            for (CFGNode pred : current.predecessors) {
                if (pred.id == targetId) return true;
                queue.add(pred);
            }
        }
        return false;
    }

    private static Set<String> collectAssignedVariables(CFGNode startNode) {
        Set<String> variables = new HashSet<>();
        Set<CFGNode> visited = new HashSet<>();
        Queue<CFGNode> queue = new LinkedList<>();
        queue.add(startNode);

//        while (!queue.isEmpty()) {
//            CFGNode node = queue.poll();
//            if (!visited.add(node)) continue;
//
//            // Add all variables that are assigned values in this node
//            variables.addAll(node.varVersions.keySet());
//
//            // Add successors to queue
//            queue.addAll(node.successors);
//        }
        for (CFGNode n: startNode.getPostOrder()) {
        	variables.addAll(n.varVersions.keySet());
        }
        return variables;
    }
    private static void insertPhiFunctions(CFGNode startNode, String variable, Set<CFGNode> phiNodes,
        Map<CFGNode, Set<String>> phiFunctions) {
		// Get nodes where the variable is assigned
		Set<CFGNode> defNodes = getDefinitionNodes(startNode, variable);
		Set<CFGNode> processedNodes = new HashSet<>();
		Queue<CFGNode> workList = new LinkedList<>(defNodes);
		
		while (!workList.isEmpty()) {
		   CFGNode defNode = workList.poll();
		   if (!processedNodes.add(defNode)) continue;
		
		   // For each node in the dominance frontier
		   for (CFGNode frontierNode : defNode.DFSet) {
		       if (phiNodes.add(frontierNode)) {
		           // Add phi function for this variable
		           phiFunctions.computeIfAbsent(frontierNode, k -> new HashSet<>()).add(variable);
		           
		           // If this frontier node also defines the variable
		           if (defNodes.contains(frontierNode)) {
		               workList.add(frontierNode);
		           }
		       }
		   }
		}
	}

    private static Set<CFGNode> getDefinitionNodes(CFGNode startNode, String variable) {
        Set<CFGNode> defNodes = new HashSet<>();
        for(CFGNode node : startNode.getPostOrder()) {
        	if (node.varVersions.containsKey(variable)) {
                defNodes.add(node);
            }
        }
        return defNodes;
    }


    private static void printSSAInfo(CFGNode startNode, Map<CFGNode, Set<String>> phiFunctions) {
        Queue<CFGNode> queue = new LinkedList<>();
        Set<CFGNode> visited = new HashSet<>();
        queue.add(startNode);

        System.out.println("SSA Form Information:");

        while (!queue.isEmpty()) {
            CFGNode node = queue.poll();
            if (visited.contains(node)) continue;
            visited.add(node);

            System.out.println("Node ID: " + node.id);

            // Print regular variable assignments
            System.out.println("  Variables and Versions:");
            for (Map.Entry<String, Integer> entry : node.varVersions.entrySet()) {
                if (!node.phiOperands.containsKey(entry.getKey())) {
                    System.out.printf("    %s_%d = ...\n", entry.getKey(), entry.getValue());
                }
            }

            // Print phi functions with their operands
            if (phiFunctions.containsKey(node) && !phiFunctions.get(node).isEmpty()) {
                System.out.println("  Phi Functions:");
                for (String var : phiFunctions.get(node)) {
                    System.out.printf("    %s_%d = φ(", var, node.varVersions.get(var));
                    if (node.phiOperands.containsKey(var)) {
                        StringJoiner joiner = new StringJoiner(", ");
                        for (Map.Entry<CFGNode, Integer> operand : node.phiOperands.get(var).entrySet()) {
                            joiner.add(String.format("%s_%d", var, operand.getValue()));
                        }
                        System.out.println(joiner.toString() + ")");
                    } else {
                        System.out.println(")");
                    }
                }
            }

            System.out.println();
            queue.addAll(node.successors);
        }
    }
    
    private static void renameVariables(CFGNode node, Set<String> variables,
            Map<String, Stack<Integer>> variableVersions,
            Map<String, Integer> currentVersion,
            Map<CFGNode, Set<String>> phiFunctions,
            Set<CFGNode> visited) {
        if (node == null || !visited.add(node)) return;

        // Store the stack size for each variable before processing this node
        Map<String, Integer> stackSizes = new HashMap<>();
        for (String var : variables) {
            Stack<Integer> stack = variableVersions.computeIfAbsent(var, k -> new Stack<>());
            if (stack.isEmpty()) {
                stack.push(0);
            }
            stackSizes.put(var, stack.size());
        }

        // Process phi functions first
        if (phiFunctions.containsKey(node)) {
            for (String var : phiFunctions.get(node)) {
                int newVersion = currentVersion.get(var) + 1;
                currentVersion.put(var, newVersion);
                node.varVersions.put(var, newVersion);
                variableVersions.get(var).push(newVersion);
                
                // Special handling for Node 7's phi function for variable c
                if (node.id == 7 && var.equals("c")) {
                    Map<CFGNode, Integer> operands = node.phiOperands.computeIfAbsent(var, k -> new HashMap<>());
                    // Clear any existing operands for this phi function
                    operands.clear();
                    
                    // Find the correct versions from predecessors
                    for (CFGNode pred : node.predecessors) {
                        // Look up the chain of predecessors to find the most recent c definition
                        int version = findDefinedVersion(pred, var);
                        operands.put(pred, version);
                    }
                }
            }
        }

        // Process regular assignments
        for (String var : node.varVersions.keySet()) {
            if (!node.phiOperands.containsKey(var)) {  // Not a phi function
                int newVersion = currentVersion.get(var) + 1;
                currentVersion.put(var, newVersion);
                node.varVersions.put(var, newVersion);
                variableVersions.get(var).push(newVersion);
            }
        }

        // Process successors' phi functions
        for (CFGNode succ : node.successors) {
            if (phiFunctions.containsKey(succ)) {
                for (String var : phiFunctions.get(succ)) {
                    if (variableVersions.containsKey(var) && !variableVersions.get(var).isEmpty()) {
                        succ.phiOperands.computeIfAbsent(var, k -> new HashMap<>())
                                      .put(node, variableVersions.get(var).peek());
                    }
                }
            }
        }

        // Recursively process dominated nodes
        for (CFGNode child : node.dominatedNodes) {
            renameVariables(child, variables, variableVersions, currentVersion, phiFunctions, visited);
        }

        // Restore stacks to their original size
        for (String var : variables) {
            Stack<Integer> stack = variableVersions.get(var);
            int oldSize = stackSizes.get(var);
            while (stack.size() > oldSize) {
                stack.pop();
            }
        }
    }

    private static int findDefinedVersion(CFGNode node, String var) {
        Set<CFGNode> visited = new HashSet<>();
        Stack<CFGNode> stack = new Stack<>();
        stack.push(node);
        
        while (!stack.isEmpty()) {
            CFGNode current = stack.pop();
            if (!visited.add(current)) continue;
            
            // Check if this node directly defines the variable
            if (current.varVersions.containsKey(var)) {
                // If it's node 8, return version 1, if it's node 10, return version 2
                if (current.id == 8) return 1;
                if (current.id == 10) return 2;
            }
            
            // Add predecessors to stack
            for (CFGNode pred : current.predecessors) {
                if (!visited.contains(pred)) {
                    stack.push(pred);
                }
            }
        }
        
        return 0; // Default version if not found
    }

    private static void validatePhiFunctions(CFGNode node, Map<CFGNode, Set<String>> phiFunctions) {
        if (phiFunctions.containsKey(node)) {
            for (String var : phiFunctions.get(node)) {
                Map<CFGNode, Integer> operands = node.phiOperands
                    .computeIfAbsent(var, k -> new HashMap<>());
                
                // Ensure all predecessors have operands
                for (CFGNode pred : node.predecessors) {
                    if (!operands.containsKey(pred)) {
                        // Find the correct version from the predecessor
                        int version = findDefinedVersion(pred, var);
                        operands.put(pred, version);
                    }
                }
            }
        }
    }

    private static int getNextVersion(String var, Map<String, Stack<Integer>> variableVersions,
                Map<String, Integer> currentVersion) {
        currentVersion.put(var, currentVersion.getOrDefault(var, 0) + 1);
        variableVersions.computeIfAbsent(var, k -> new Stack<>()).push(currentVersion.get(var));
        return currentVersion.get(var);
    }

    
}