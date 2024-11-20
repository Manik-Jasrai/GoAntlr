package antlr;

import java.util.*;

public class CFGAnalyzer {
	List<CFGNode> postOrder;
	
	CFGAnalyzer(CFGNode root) {
		postOrder = root.getPostOrder();
	}
	
	public void calculateDominators(CFGNode root) {
	    calculateDominatorSets(root);
	    calculateStrictDominance(root);
	    calculateImmediateDominators(root);
	    calculateDominatedNodes();  // Add this line
	    calculateDominanceFrontier(root);
	}
    
    // Method to compute the dominance set for each node
	public void calculateDominatorSets(CFGNode root) {
	    // Step 1: Initialize the dominator sets
	    initializeDominatorSets(root);
	    // Step 2: Perform iterative data flow analysis
	    boolean changed;
	    do {
	        changed = false;
	        for (CFGNode node : postOrder) {
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

	private void initializeDominatorSets(CFGNode root) {
	    for (CFGNode node : postOrder) {
	        if (node == root) {
	            node.domSet.add(root);
	        } else {
	            node.domSet.addAll(postOrder);
	        }
	    }
	}


    private void calculateStrictDominance(CFGNode root) {
        for (CFGNode node : postOrder) {
            // Calculate strict dominance set
            node.sDomSet.addAll(node.domSet);
            node.sDomSet.remove(node);
        }
    }
    
    private void calculateDominanceFrontier(CFGNode root) {
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
    	
//    	for(CFGNode b : postOrder) {
//    		if (b != root) {
//    			if (b.getParent().size() >= 2) {
//    				for(CFGNode p : b.getParent()) {
//    					CFGNode runner = p;
//    					while (runner != b.iDom) {
//    						runner.DFSet.add(b);
//    						runner = runner.iDom;
//    					}
//    				}
//    			}
//    		}
//    	}
    }
    
    private void calculateImmediateDominators(CFGNode root) {
    	//If n is b’s immediate dominator, then every node in {Dom(b) − b} is also in Dom(n).
        for (CFGNode node : root.getPostOrder()) {
            if (node == root) {
                node.iDom = null;
            } else {
            	for (CFGNode pre : node.sDomSet ) {
            		if (node.sDomSet.containsAll(pre.domSet) && pre.domSet.containsAll(node.sDomSet)) {
            			node.iDom = pre;
            		}
            	}
            }
        }
    }
    
    private void calculateDominatedNodes() {
        for (CFGNode node : postOrder) {
            node.dominatedNodes.clear();
            for (CFGNode potentialDominated : postOrder) {
                if (potentialDominated != node && potentialDominated.domSet.contains(node)) {
                    node.dominatedNodes.add(potentialDominated);
                }
            }
        }
    }
    
}
