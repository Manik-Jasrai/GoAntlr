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
    	List<CFGNode> postOrder = root.getPostOrder();
        for (CFGNode node : postOrder) {
            // Calculate strict dominance set
            node.sDomSet.addAll(node.domSet);
            node.sDomSet.remove(node);
        }
        // Calculate dominance frontier set
        
        for(CFGNode node : postOrder) {// for each Node N
        	
            // Find each node M dominated by node N
        	List<CFGNode> dominated = new ArrayList<>(); // List of all the nodes dominated by N
        	for(CFGNode M : postOrder) {
        		if (M.domSet.contains(node)) {
        			dominated.add(M);
        		}
        	}
        	List<CFGNode>succ = new ArrayList<>();
        	for(CFGNode n : dominated) {
        		for(CFGNode s : n.successors) {
        			if(!succ.contains(s) && !s.sDomSet.contains(node)) {
        				succ.add(s);
        				node.DFSet.add(s);
        			}
        		}
        	}
        }
        
    }
    
    private static void calculateImmediateDominators(CFGNode root) {
        for (CFGNode node : root.getPostOrder()) {
            if (node == root) {
                node.iDom = null;
            } else {
                for (CFGNode predecessor : node.getParent()) {
                    if (node.domSet.contains(predecessor)) {
                        node.iDom = predecessor;
                        break;
                    }
                }
            }
        }
    }
}
