package antlr;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.*;
import java.util.*;
import javax.swing.SwingUtilities;

public class GoASTMain {
    public static void main(String[] args) throws IOException {
        // Read input Go source file
        CharStream input = CharStreams.fromFileName("/Users/karandeepsingh/git/GoAntlr/src/tests/ex.go");

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

        // Step 2: Insert phi functions based on the dominance frontier (unchanged)
        for (String variable : assignedVariables) {
            Set<CFGNode> phiNodes = new HashSet<>();
            insertPhiFunctions(startNode, variable, phiNodes, phiFunctions);
        }

        // Step 3: Rename variables using the dominator tree
        Set<CFGNode> visited = new HashSet<>();
        renameVariables(startNode, assignedVariables, variableVersions, currentVersion, phiFunctions, visited);
    }

    private static Set<String> collectAssignedVariables(CFGNode startNode) {
        Set<String> variables = new HashSet<>();
        Set<CFGNode> visited = new HashSet<>();
        Queue<CFGNode> queue = new LinkedList<>();
        queue.add(startNode);

        while (!queue.isEmpty()) {
            CFGNode node = queue.poll();
            if (!visited.add(node)) continue;

            // Add all variables that are assigned values in this node
            variables.addAll(node.varVersions.keySet());

            // Add successors to queue
            queue.addAll(node.successors);
        }
        return variables;
    }

    private static void insertPhiFunctions(CFGNode startNode, String variable, Set<CFGNode> phiNodes, 
                     Map<CFGNode, Set<String>> phiFunctions) {
        // Get nodes where the variable is assigned
        Set<CFGNode> defNodes = getDefinitionNodes(startNode, variable);

        // For each node that defines the variable
        for (CFGNode defNode : defNodes) {
            // Add phi functions at dominance frontier nodes
            for (CFGNode frontierNode : defNode.DFSet) {
                if (phiNodes.add(frontierNode)) {  // If we haven't placed a phi function here yet
                    // Add phi function for this variable
                    phiFunctions.computeIfAbsent(frontierNode, k -> new HashSet<>()).add(variable);

                    // If the variable is also defined in the frontier node, we need to
                    // consider its dominance frontier as well (for nested loops)
                    if (defNodes.contains(frontierNode)) {
                        insertPhiFunctions(startNode, variable, phiNodes, phiFunctions);
                    }
                }
            }
        }
    }

    private static Set<CFGNode> getDefinitionNodes(CFGNode startNode, String variable) {
        Set<CFGNode> defNodes = new HashSet<>();
        Set<CFGNode> visited = new HashSet<>();
        Queue<CFGNode> queue = new LinkedList<>();
        queue.add(startNode);

        while (!queue.isEmpty()) {
            CFGNode node = queue.poll();
            if (!visited.add(node)) continue;

            // Check if this node defines the variable
            if (node.varVersions.containsKey(variable)) {
                defNodes.add(node);
            }

            queue.addAll(node.successors);
        }
        return defNodes;
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
      stackSizes.put(var, stack.size());
  }

  // Process phi functions first
  if (phiFunctions.containsKey(node)) {
      for (String var : phiFunctions.get(node)) {
          int newVersion = getNextVersion(var, variableVersions, currentVersion);
          node.varVersions.put(var, newVersion);
      }
  }

  // Process uses before definitions
  // Replace uses with the most recent version from the stack
  for (String var : node.varVersions.keySet()) {
      Stack<Integer> stack = variableVersions.get(var);
      if (!stack.isEmpty() && !node.phiOperands.containsKey(var)) {
          node.varVersions.put(var, stack.peek());
      }
  }

  // Process definitions by creating new versions
  for (String var : node.varVersions.keySet()) {
      if (variables.contains(var) && !node.phiOperands.containsKey(var)) {
          int newVersion = getNextVersion(var, variableVersions, currentVersion);
          node.varVersions.put(var, newVersion);
      }
  }

  // Record phi operands for successor nodes before recursing
  for (CFGNode succ : node.successors) {
      if (phiFunctions.containsKey(succ)) {
          for (String var : phiFunctions.get(succ)) {
              Stack<Integer> stack = variableVersions.get(var);
              if (!stack.isEmpty()) {
                  succ.phiOperands.computeIfAbsent(var, k -> new HashMap<>())
                                .put(node, stack.peek());
              }
          }
      }
  }

  // Recursively process all children in the dominator tree
  for (CFGNode child : node.dominatedNodes) {
      renameVariables(child, variables, variableVersions, currentVersion, phiFunctions, visited);
  }

  // Pop all versions that were pushed in this node
  for (String var : variables) {
      Stack<Integer> stack = variableVersions.get(var);
      int oldSize = stackSizes.get(var);
      while (stack.size() > oldSize) {
          stack.pop();
      }
  }
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

    private static int getNextVersion(String var, Map<String, Stack<Integer>> variableVersions,
                  Map<String, Integer> currentVersion) {
        currentVersion.put(var, currentVersion.getOrDefault(var, 0) + 1);
        variableVersions.computeIfAbsent(var, k -> new Stack<>()).push(currentVersion.get(var));
        return currentVersion.get(var);
    }

    private static void restoreVersion(String var, Map<String, Stack<Integer>> variableVersions) {
        if (variableVersions.containsKey(var) && !variableVersions.get(var).isEmpty()) {
            variableVersions.get(var).pop();
        }
    }
}