package antlr;

import java.util.*;

public class CFGAnalyzer {
    
    // Method to compute the dominance set for each node
    public Map<CFGNode, Set<CFGNode>> computeDominanceSet(Set<CFGNode> nodes, CFGNode entryNode) {
        Map<CFGNode, Set<CFGNode>> dominanceSet = new HashMap<>();
        
        // Initialize dominance sets
        for (CFGNode node : nodes) {
            if (node == entryNode) {
                dominanceSet.put(node, new HashSet<>(Collections.singletonList(node)));
            } else {
                dominanceSet.put(node, new HashSet<>(nodes));
            }
        }

        boolean changed;
        do {
            changed = false;
            for (CFGNode node : nodes) {
                if (node == entryNode) continue;

                Set<CFGNode> newDomSet = new HashSet<>(nodes);
                for (CFGNode pred : node.getParent()) {
                    newDomSet.retainAll(dominanceSet.get(pred));
                }
                newDomSet.add(node);

                if (!newDomSet.equals(dominanceSet.get(node))) {
                    dominanceSet.put(node, newDomSet);
                    changed = true;
                }
            }
        } while (changed);

        return dominanceSet;
    }

    // Method to compute the strict dominance set
    public Map<CFGNode, Set<CFGNode>> computeStrictDominanceSet(Map<CFGNode, Set<CFGNode>> dominanceSet) {
        Map<CFGNode, Set<CFGNode>> strictDomSet = new HashMap<>();

        for (Map.Entry<CFGNode, Set<CFGNode>> entry : dominanceSet.entrySet()) {
            Set<CFGNode> strictDom = new HashSet<>(entry.getValue());
            strictDom.remove(entry.getKey());
            strictDomSet.put(entry.getKey(), strictDom);
        }

        return strictDomSet;
    }

    // Method to compute the immediate dominator
    public Map<CFGNode, CFGNode> computeImmediateDominator(Map<CFGNode, Set<CFGNode>> dominanceSet) {
        Map<CFGNode, CFGNode> immediateDominator = new HashMap<>();

        for (CFGNode node : dominanceSet.keySet()) {
            if (dominanceSet.get(node).size() > 1) {
                Set<CFGNode> strictDominators = new HashSet<>(dominanceSet.get(node));
                strictDominators.remove(node);

                CFGNode iDom = null;
                for (CFGNode domNode : strictDominators) {
                    if (strictDominators.containsAll(dominanceSet.get(domNode))) {
                        iDom = domNode;
                    }
                }
                immediateDominator.put(node, iDom);
            }
        }

        return immediateDominator;
    }

    // Method to compute dominance frontier
    public Map<CFGNode, Set<CFGNode>> computeDominanceFrontier(Set<CFGNode> nodes, Map<CFGNode, CFGNode> immediateDominator) {
        Map<CFGNode, Set<CFGNode>> dominanceFrontier = new HashMap<>();

        for (CFGNode node : nodes) {
            Set<CFGNode> dfSet = new HashSet<>();

            for (CFGNode succ : node.getNext()) {
                if (immediateDominator.get(succ) != node) {
                    dfSet.add(succ);
                }
            }

            for (CFGNode child : nodes) {
                if (immediateDominator.get(child) == node) {
                    for (CFGNode frontierNode : dominanceFrontier.getOrDefault(child, new HashSet<>())) {
                        if (immediateDominator.get(frontierNode) != node) {
                            dfSet.add(frontierNode);
                        }
                    }
                }
            }

            dominanceFrontier.put(node, dfSet);
        }

        return dominanceFrontier;
    }
}
