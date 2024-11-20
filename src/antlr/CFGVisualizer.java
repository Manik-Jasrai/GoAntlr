
package antlr;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class CFGVisualizer extends JFrame {
    private CFGPanel cfgPanel;

    public CFGVisualizer(CFGNode startNode) {
        super("Control Flow Graph Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cfgPanel = new CFGPanel(startNode);
        JScrollPane scrollPane = new JScrollPane(cfgPanel);
        scrollPane.setPreferredSize(new Dimension(800, 600));

        add(scrollPane);
        pack();
        setLocationRelativeTo(null);
    }
}

class NodeAttributesDialog extends JDialog {
    public NodeAttributesDialog(Frame parent, CFGNode node) {
        super(parent, "Node " + node.id + " Attributes", true);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create a text area for displaying attributes
        JTextArea attributesArea = new JTextArea(20, 40);
        attributesArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(attributesArea);
        
        // Build the attributes text
        StringBuilder sb = new StringBuilder();
        sb.append("Node ID: ").append(node.id).append("\n");
        sb.append("Label: ").append(node.label).append("\n");
        sb.append("Variable Name: ").append(node.varName.isEmpty() ? "none" : node.varName).append("\n");
        
        sb.append("Successors: ").append(formatNodeSet(node.successors)).append("\n");
        sb.append("Predecessors: ").append(formatNodeSet(node.predecessors)).append("\n");
        sb.append("Dominance Set: ").append(formatNodeSet(node.domSet)).append("\n");
        sb.append("Strict Dominance Set: ").append(formatNodeSet(node.sDomSet)).append("\n");
        sb.append("Dominance Frontier Set: ").append(formatNodeSet(node.DFSet)).append("\n");
        sb.append("Immediate Dominator: ").append(node.iDom != null ? node.iDom.id : "none").append("\n\n");
        
        attributesArea.setText(sb.toString());
        
        mainPanel.add(scrollPane);
        
        // Add close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        mainPanel.add(buttonPanel);
        
        add(mainPanel);
        pack();
        setLocationRelativeTo(parent);
    }
    
    private String formatNodeSet(Collection<CFGNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return "[]";
        return nodes.stream()
            .map(n -> String.valueOf(n.id))
            .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);
    }
    
    private String formatStringSet(Set<String> set) {
        if (set == null || set.isEmpty()) return "[]";
        return String.join(", ", set);
    }
    
    private String formatPhiNodes(Map<String, List<List<String>>> phiNodes) {
        if (phiNodes == null || phiNodes.isEmpty()) return "  None\n";
        StringBuilder sb = new StringBuilder();
        phiNodes.forEach((var, lists) -> {
            sb.append("  ").append(var).append(": ").append(lists).append("\n");
        });
        return sb.toString();
    }
    
    private String formatVersionedPhiNodes(Map<String, List<Object>> versionedPhiNodes) {
        if (versionedPhiNodes == null || versionedPhiNodes.isEmpty()) return "  None\n";
        StringBuilder sb = new StringBuilder();
        versionedPhiNodes.forEach((var, value) -> {
            sb.append("  ").append(var).append(": ").append(value).append("\n");
        });
        return sb.toString();
    }
    
    private String formatMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) return "{}";
        return map.toString();
    }
}

class CFGPanel extends JPanel {
    private CFGNode startNode;
    private Map<CFGNode, Point> nodePositions;
    private Map<CFGNode, Dimension> nodeDimensions;
    private static final int NODE_DIAMETER = 50;
    private static final int LEVEL_HEIGHT = 100;
    private static final int NODE_HORIZONTAL_SPACING = 100;

    public CFGPanel(CFGNode startNode) {
        this.startNode = startNode;
        this.nodePositions = new HashMap<>();
        this.nodeDimensions = new HashMap<>();
        calculateLayout();
        setPreferredSize(calculatePreferredSize());
        
        // Add mouse listener for node clicking
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CFGNode clickedNode = findClickedNode(e.getPoint());
                if (clickedNode != null) {
                    showNodeAttributes(clickedNode);
                }
            }
        });
    }

    private CFGNode findClickedNode(Point clickPoint) {
        for (Map.Entry<CFGNode, Point> entry : nodePositions.entrySet()) {
            Point pos = entry.getValue();
            Ellipse2D.Double nodeBounds = new Ellipse2D.Double(
                pos.x, pos.y, NODE_DIAMETER, NODE_DIAMETER);
            if (nodeBounds.contains(clickPoint)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void showNodeAttributes(CFGNode node) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof Frame) {
            NodeAttributesDialog dialog = new NodeAttributesDialog((Frame) window, node);
            dialog.setVisible(true);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawNodes(g2d);
        drawEdges(g2d);
    }


    private void drawNodes(Graphics2D g2d) {
        for (Map.Entry<CFGNode, Point> entry : nodePositions.entrySet()) {
            CFGNode node = entry.getKey();
            Point pos = entry.getValue();

            // Draw node background
            g2d.setColor(Color.WHITE);
            g2d.fill(new Ellipse2D.Double(pos.x, pos.y, NODE_DIAMETER, NODE_DIAMETER));

            // Draw node border
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(new Ellipse2D.Double(pos.x, pos.y, NODE_DIAMETER, NODE_DIAMETER));

            // Draw node text
            g2d.setColor(Color.BLACK);
            String nodeText = String.valueOf(node.id);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(nodeText);
            int textHeight = fm.getHeight();
            int textX = pos.x + (NODE_DIAMETER - textWidth) / 2;
            int textY = pos.y + (NODE_DIAMETER + textHeight) / 2 - fm.getDescent();
            g2d.drawString(nodeText, textX, textY);
        }
    }

    private void drawEdges(Graphics2D g2d) {
        g2d.setColor(Color.GREEN);  // Changed color to green
        g2d.setStroke(new BasicStroke(1.5f));

        for (Map.Entry<CFGNode, Point> entry : nodePositions.entrySet()) {
            CFGNode node = entry.getKey();
            Point startPos = entry.getValue();

            for (CFGNode successor : node.successors) {
                Point endPos = nodePositions.get(successor);

                if (endPos != null) {
                    // Calculate start and end points for the arrow, adjusting for circular nodes
                    double startX = startPos.x + NODE_DIAMETER / 2;
                    double startY = startPos.y + NODE_DIAMETER / 2;
                    double endX = endPos.x + NODE_DIAMETER / 2;
                    double endY = endPos.y + NODE_DIAMETER / 2;

                    // Calculate angle and adjusted positions to start/end at circle edge
                    double angle = Math.atan2(endY - startY, endX - startX);
                    double radius = NODE_DIAMETER / 2;

                    Point2D.Double start = new Point2D.Double(
                        startX + Math.cos(angle) * radius,
                        startY + Math.sin(angle) * radius
                    );
                    Point2D.Double end = new Point2D.Double(
                        endX - Math.cos(angle) * radius,
                        endY - Math.sin(angle) * radius
                    );

                    drawArrow(g2d, start, end);
                }
            }
        }
    }


    private void calculateLayout() {
        Map<CFGNode, Integer> levels = new HashMap<>();
        Map<Integer, List<CFGNode>> nodesAtLevel = new HashMap<>();
        Queue<CFGNode> queue = new LinkedList<>();
        Set<CFGNode> visited = new HashSet<>();

        queue.offer(startNode);
        levels.put(startNode, 0);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            CFGNode node = queue.poll();
            int level = levels.get(node);

            nodesAtLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(node);

            for (CFGNode successor : node.successors) {
                if (!visited.contains(successor)) {
                    queue.offer(successor);
                    levels.put(successor, level + 1);
                    visited.add(successor);
                }
            }
        }

        int maxNodesInLevel = nodesAtLevel.values().stream()
            .mapToInt(List::size)
            .max()
            .orElse(1);

        for (Map.Entry<Integer, List<CFGNode>> entry : nodesAtLevel.entrySet()) {
            int level = entry.getKey();
            List<CFGNode> nodes = entry.getValue();
            int totalWidth = nodes.size() * NODE_HORIZONTAL_SPACING;
            int startX = (maxNodesInLevel * NODE_HORIZONTAL_SPACING - totalWidth) / 2;

            for (int i = 0; i < nodes.size(); i++) {
                CFGNode node = nodes.get(i);
                int x = startX + i * NODE_HORIZONTAL_SPACING;
                int y = level * LEVEL_HEIGHT;
                nodePositions.put(node, new Point(x, y));
                nodeDimensions.put(node, new Dimension(NODE_DIAMETER, NODE_DIAMETER));
            }
        }
    }

    private Dimension calculatePreferredSize() {
        int width = nodePositions.values().stream()
                .mapToInt(p -> p.x + NODE_DIAMETER)
                .max()
                .orElse(800);
        int height = nodePositions.values().stream()
                .mapToInt(p -> p.y + NODE_DIAMETER)
                .max()
                .orElse(600);
        return new Dimension(width, height);
    }

    private void drawArrow(Graphics2D g2d, Point2D.Double start, Point2D.Double end) {
        g2d.draw(new Line2D.Double(start, end));

        double arrowAngle = Math.PI / 6;  // 30 degrees
        double arrowLength = 10;
        double angle = Math.atan2(end.y - start.y, end.x - start.x);

        Point2D.Double arrowHead1 = new Point2D.Double(
            end.x - arrowLength * Math.cos(angle - arrowAngle),
            end.y - arrowLength * Math.sin(angle - arrowAngle)
        );
        Point2D.Double arrowHead2 = new Point2D.Double(
            end.x - arrowLength * Math.cos(angle + arrowAngle),
            end.y - arrowLength * Math.sin(angle + arrowAngle)
        );

        g2d.draw(new Line2D.Double(end, arrowHead1));
        g2d.draw(new Line2D.Double(end, arrowHead2));
    }
}