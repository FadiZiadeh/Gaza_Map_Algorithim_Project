package com.example.algoproj3;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;

public class MapPanel extends Canvas {

    // Bounds derived from Beit Hanoun (890,115) and Khan Younis (270,790) anchor points
    public static final double MIN_LAT = 31.213117;//bottom edge of image (south)
    public static final double MAX_LAT = 31.573311; //top edge of image (north)
    public static final double MIN_LON = 34.213803;//left west
    public static final double MAX_LON = 34.562578;//right east

    private Image mapImage;
    private MapGraph graph;
    private Node sourceNode = null;
    private Node targetNode = null;
    private List<Node> currentPath = null;

    private double scaleX;
    private double scaleY;

    public MapPanel(double width, double height, MapGraph graph, Image mapImage) {
        super(width, height); //760x860
        this.graph = graph;
        this.mapImage = mapImage;

        // compute scale factors once here
        // scaleX how many pixels fit in one degree of longitude
        scaleX = getWidth()  / (MAX_LON - MIN_LON);

        //scaleY how many pixels fit in one degree of latitude
        scaleY = getHeight() / (MAX_LAT - MIN_LAT);
    }

    // lonToX converts longitude to canvas X pixel
    //shift lon to start from 0 -> lon - MIN_LON
    //scale to canvas width -> multiply by scaleX
    public double lonToX(double lon) {
        return (lon - MIN_LON) * scaleX;
    }

    // latToY: converts latitude to canvas Y pixel
    // if he asked  me why (MAX_LAT - lat)?
    // latitude increases UPWARD  (north = bigger number)
    // pixels   increase DOWNWARD (top   = smaller number)
    //so we flip: higher lat - smaller y - higher on screen
    public double latToY(double lat) {
        return (MAX_LAT - lat) * scaleY;
    }


    // these methods are the reverse of the above , they are for mouse clicks
    // xToLon: converts canvas X pixel back to longitude
    // Reverse of lonToX: undo scale then undo shift
    public double xToLon(double x) {
        return MIN_LON + x / scaleX;
    }

    // yToLat: converts canvas Y pixel back to latitude
    // Reverse of latToY: undo scale then undo flip
    public double yToLat(double y) {
        return MAX_LAT - y / scaleY;
    }

    public void setSource(Node n) { this.sourceNode = n; this.currentPath = null; draw(); }
    public void setTarget(Node n) { this.targetNode = n; this.currentPath = null; draw(); }
    public void setPath(List<Node> path) { this.currentPath = path; draw(); }
    public void clearAll() { sourceNode = null; targetNode = null; currentPath = null; draw(); }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double W = getWidth(), H = getHeight();

        // Layer 1: Map background image
        if (mapImage != null && !mapImage.isError()) {
            gc.drawImage(mapImage, 0, 0, W, H);
        } else {
            gc.setFill(Color.web("#e8e0d0"));
            gc.fillRect(0, 0, W, H);
            gc.setFill(Color.GRAY);
            gc.fillText("Place gaza_map.png in project folder", 150, H / 2);
        }

        // Layer 2: All edges (gray lines)
        gc.setStroke(Color.color(0.15, 0.15, 0.15, 0.65));
        gc.setLineWidth(1.2);
        for (String id : graph.adjacency.keySet()) {
            Node u = graph.nodes.get(id);
            for (String nid : graph.adjacency.get(id)) {
                if (id.compareTo(nid) < 0) {
                    Node v = graph.nodes.get(nid);
                    gc.strokeLine(lonToX(u.lon), latToY(u.lat),
                            lonToX(v.lon), latToY(v.lat));
                }
            }
        }

        // Layer 3: Intersection dots (small gray)
        gc.setFill(Color.color(0.3, 0.3, 0.3, 0.7));
        for (Node node : graph.nodes.values()) {
            if (!node.isCity) {
                gc.fillOval(lonToX(node.lon) - 2, latToY(node.lat) - 2, 5, 5);
            }
        }

        // Layer 4: Shortest path with ARROWS
        // Each segment from node A to node B gets an arrow
        // pointing in the direction of travel (source → target)
        if (currentPath != null && currentPath.size() > 1) {
            gc.setStroke(Color.RED);
            gc.setFill(Color.RED);
            gc.setLineWidth(3.0);
            for (int i = 0; i < currentPath.size() - 1; i++) {
                Node a = currentPath.get(i);
                Node b = currentPath.get(i + 1);
                double x1 = lonToX(a.lon), y1 = latToY(a.lat);
                double x2 = lonToX(b.lon), y2 = latToY(b.lat);
                drawArrow(gc, x1, y1, x2, y2);
            }
        }

        // Layer 5: City circles (blue)
        for (Node city : graph.cities) {
            double x = lonToX(city.lon);
            double y = latToY(city.lat);
            gc.setFill(Color.STEELBLUE);
            gc.fillOval(x - 6, y - 6, 12, 12);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(x - 6, y - 6, 12, 12);
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            gc.fillText(city.getDisplayName(), x + 8, y + 4);
        }

        // Layer 6: Source city (green - bigger, on top)
        if (sourceNode != null) {
            double x = lonToX(sourceNode.lon);
            double y = latToY(sourceNode.lat);
            gc.setFill(Color.LIMEGREEN);
            gc.fillOval(x - 9, y - 9, 18, 18);
            gc.setStroke(Color.DARKGREEN);
            gc.setLineWidth(2.0);
            gc.strokeOval(x - 9, y - 9, 18, 18);
            gc.setFill(Color.DARKGREEN);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            gc.fillText(sourceNode.getDisplayName(), x + 11, y + 4);
        }

        // Layer 7: Target city (red - bigger, on top)
        if (targetNode != null) {
            double x = lonToX(targetNode.lon);
            double y = latToY(targetNode.lat);
            gc.setFill(Color.TOMATO);
            gc.fillOval(x - 9, y - 9, 18, 18);
            gc.setStroke(Color.DARKRED);
            gc.setLineWidth(2.0);
            gc.strokeOval(x - 9, y - 9, 18, 18);
            gc.setFill(Color.DARKRED);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            gc.fillText(targetNode.getDisplayName(), x + 11, y + 4);
        }
    }
    private void drawArrow(GraphicsContext gc, double x1, double y1, double x2, double y2) {

        gc.strokeLine(x1, y1, x2, y2);

        double angle = Math.atan2(y2 - y1, x2 - x1);

        //arrow size
        double arrowLength = 12.0; //lenght of the arrow
        double arrowAngle  = Math.toRadians(25); // angle of wings

        //calculate the arrow head
        // this for rotation
        double wing1X = x2 - arrowLength * Math.cos(angle - arrowAngle);
        double wing1Y = y2 - arrowLength * Math.sin(angle - arrowAngle);

        // Wing 2: rotate angle by -arrowAngle
        double wing2X = x2 - arrowLength * Math.cos(angle + arrowAngle);
        double wing2Y = y2 - arrowLength * Math.sin(angle + arrowAngle);

        gc.fillPolygon(new double[]{x2,     wing1X, wing2X},  // x coordinates
                new double[]{y2,     wing1Y, wing2Y},  // y coordinates
                3  // number of points
        );
    }
}