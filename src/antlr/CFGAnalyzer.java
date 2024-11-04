package antlr;

import java.util.*;

public class CFGAnalyzer {
	
	public static void calculateDominators(CFGNode root) {
		calculateDominatorSets(root);
		calculateStrictDominanceAndDominanceFrontier(root);
		calculateImmediateDominators(root);
	}
    
    // Method to compute the dominance set for each node
	public static void calculateDominatorSets(CFGNode root) {
	    // Step 1: Initialize the dominator sets
	    initializeDominatorSets(root);

	    // Step 2: Perform iterative dataflow analysis
	    boolean changed;
	    do {
	        changed = false;
	        for (CFGNode node : root.getPostOrder()) {
	            if (node == root) continue; // Skip the root node as it only dominates itself

	            Set<CFGNode> newDomSet = new HashSet<>(node.getParent().iterator().next().domSet);
	            for (CFGNode predecessor : node.getParent()) {
	                newDomSet.retainAll(predecessor.domSet);
	            }

	            // Add the node to its own dominator set
	            newDomSet.add(node);

	            // Check if there is any change in the dominator set
	            if (!newDomSet.equals(node.domSet)) {
	                node.domSet = newDomSet;
	                changed = true;
	            }
	        }
	    } while (changed);
	}

	private static void initializeDominatorSets(CFGNode root) {
	    List<CFGNode> postOrder = root.getPostOrder();
	    for (CFGNode node : postOrder) {
	        if (node == root) {
	            node.domSet.add(root);
	        } else {
	            node.domSet.addAll(postOrder);
	        }
	    }
	}



	private static void calculateStrictDominanceAndDominanceFrontier(CFGNode root) {
	    // Calculate strict dominance and dominance frontier for each node
	    for (CFGNode node : root.getPostOrder()) {
	        // Calculate strict dominance set (sDomSet)
	        node.sDomSet.addAll(node.domSet);
	        node.sDomSet.remove(node);

	        // Calculate dominance frontier set (DFSet)
	        for (CFGNode successor : node.getNext()) {
	            // Case 1: Successor is not dominated by node, add to DFSet
	            if (!node.domSet.contains(successor)) {
	                node.DFSet.add(successor);
	            } else {
	                // Case 2: Successor is dominated by node
	                // Look for nodes in successor's dominance frontier
	                for (CFGNode frontierNode : successor.DFSet) {
	                    if (!node.domSet.contains(frontierNode)) {
	                        node.DFSet.add(frontierNode);
	                    }
	                }
	            }
	        }
	    }
	}

	private static void calculateImmediateDominators(CFGNode root) {
	    // Calculate immediate dominators (iDom) for each node
	    for (CFGNode node : root.getPostOrder()) {
	        if (node == root) {
	            node.iDom = null; // Root has no immediate dominator
	        } else {
	            CFGNode immediateDominator = null;
	            for (CFGNode predecessor : node.getParent()) {
	                if (node.domSet.contains(predecessor)) {
	                    if (immediateDominator == null) {
	                        immediateDominator = predecessor;
	                    } else {
	                        // Find the intersection of dominator paths to find closest dominator
	                        Set<CFGNode> intersection = new HashSet<>(immediateDominator.domSet);
	                        intersection.retainAll(predecessor.domSet);
	                        immediateDominator = findClosestNode(intersection, node);
	                    }
	                }
	            }
	            node.iDom = immediateDominator;
	        }
	    }
	}

	// Helper method to find the closest node to 'node' from a set of candidates
	private static CFGNode findClosestNode(Set<CFGNode> candidates, CFGNode node) {
	    for (CFGNode candidate : node.getPostOrder()) {
	        if (candidates.contains(candidate)) {
	            return candidate;
	        }
	    }
	    return null;
	}

}
