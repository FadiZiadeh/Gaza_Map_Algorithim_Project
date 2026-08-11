package com.example.algoproj3;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.List;

public class Main extends Application {

    private MapGraph graph = new MapGraph();
    private MapPanel mapPanel;

    private ComboBox<String> sourceBox = new ComboBox<>();
    private ComboBox<String> targetBox = new ComboBox<>();
    private TextArea pathArea = new TextArea();
    private Label distanceLabel = new Label("--");

    private Node selectedSource = null;
    private Node selectedTarget = null;
    private int clickState = 0;

    @Override
    public void start(Stage stage) {

        // Load graph
        try {
            graph.loadFromFile("gaza_map.txt");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Cannot load gaza_map.txt:\n" + e.getMessage()).showAndWait();
            return;
        }

        // Load map image
        Image mapImage = null;
        try {
            java.io.File f = new java.io.File("gaza_map.png");
            if (f.exists()) mapImage = new Image(f.toURI().toString());
        } catch (Exception e) {
            System.out.println("gaza_map.png not found, using fallback background.");
        }

        // Map panel
        mapPanel = new MapPanel(760, 860, graph, mapImage);
        mapPanel.draw();

        // Mouse click on map -> pick nearest city
        mapPanel.setOnMouseClicked(e -> {
            double lat = mapPanel.yToLat(e.getY());
            double lon = mapPanel.xToLon(e.getX());
            Node nearest = graph.findNearestCity(lat, lon);
            if (nearest == null) return;

            if (clickState == 0) {
                // Set source
                selectedSource = nearest;
                selectedTarget = null;
                clickState = 1;
                syncCombo(sourceBox, nearest.id);
                syncCombo(targetBox, null);
                mapPanel.setSource(nearest);
                mapPanel.setTarget(null);
                clearResults();
            } else {
                // Set target
                if (selectedSource != null && nearest.id.equals(selectedSource.id)) return;
                selectedTarget = nearest;
                clickState = 0;
                syncCombo(targetBox, nearest.id);
                mapPanel.setTarget(nearest);
                clearResults();
            }
        });

        // Populate comboboxes
        for (Node city : graph.cities) {
            sourceBox.getItems().add(city.id);
            targetBox.getItems().add(city.id);
        }
        sourceBox.setButtonCell(new CityCell());
        sourceBox.setCellFactory(lv -> new CityCell());
        targetBox.setButtonCell(new CityCell());
        targetBox.setCellFactory(lv -> new CityCell());

        // Source combobox change
        sourceBox.setOnAction(e -> {
            String val = sourceBox.getValue();
            if (val == null) return;
            Node node = graph.nodes.get(val);
            if (node == null) return;
            if (selectedTarget != null && selectedTarget.id.equals(val)) {
                selectedTarget = null;
                syncCombo(targetBox, null);
                mapPanel.setTarget(null);
            }
            selectedSource = node;
            clickState = 1;
            mapPanel.setSource(node);
            clearResults();
        });

        // Target combobox change
        targetBox.setOnAction(e -> {
            String val = targetBox.getValue();
            if (val == null) return;
            Node node = graph.nodes.get(val);
            if (node == null) return;
            if (selectedSource != null && selectedSource.id.equals(val)) {
                syncCombo(targetBox, null);
                return;
            }
            selectedTarget = node;
            clickState = 0;
            mapPanel.setTarget(node);
            clearResults();
        });

        // Run button
        Button runBtn = new Button("Run Dijkstra");
        runBtn.setMaxWidth(Double.MAX_VALUE);
        runBtn.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-background-color:#2e7d32; -fx-text-fill:white;");
        runBtn.setOnAction(e -> runDijkstra());

        // Clear button
        Button clearBtn = new Button("Clear");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setStyle("-fx-font-size:12px;");
        clearBtn.setOnAction(e -> {
            selectedSource = null;
            selectedTarget = null;
            clickState = 0;
            syncCombo(sourceBox, null);
            syncCombo(targetBox, null);
            mapPanel.clearAll();
            clearResults();
        });

        // Path area
        pathArea.setEditable(false);
        pathArea.setPrefHeight(220);
        pathArea.setWrapText(true);
        pathArea.setPromptText("Path will appear here");
        pathArea.setStyle("-fx-font-size:11px;");

        // Right panel
        VBox right = new VBox(10);
        right.setPadding(new Insets(15));
        right.setPrefWidth(230);
        right.setStyle("-fx-background-color:#f5f5f5;");

        Label title = new Label("Gaza Strip");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");
        Label subtitle = new Label("Dijkstra Shortest Path");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#555;");

        Label srcLbl = new Label("Source City:");
        srcLbl.setStyle("-fx-font-weight:bold;");
        sourceBox.setMaxWidth(Double.MAX_VALUE);
        sourceBox.setPromptText("Select or click map");

        Label tgtLbl = new Label("Target City:");
        tgtLbl.setStyle("-fx-font-weight:bold;");
        targetBox.setMaxWidth(Double.MAX_VALUE);
        targetBox.setPromptText("Select or click map");


        Label pathLbl = new Label("Path:");
        pathLbl.setStyle("-fx-font-weight:bold;");

        Label distLbl = new Label("Total Distance:");
        distLbl.setStyle("-fx-font-weight:bold;");
        distanceLabel.setStyle("-fx-font-size:14px; -fx-text-fill:#1a237e;");



        right.getChildren().addAll(
                title, subtitle,
                new Separator(),
                srcLbl, sourceBox,
                tgtLbl, targetBox,
                new Separator(),
                runBtn, clearBtn,
                new Separator(),
                pathLbl, pathArea,
                distLbl, distanceLabel,
                new Separator()
        );

        BorderPane root = new BorderPane();
        root.setCenter(mapPanel);
        root.setRight(right);

        Scene scene = new Scene(root);
        stage.setTitle("Gaza Strip Dijkstra Shortest Path ");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void runDijkstra() {
        if (selectedSource == null) {
            alert("Please select a Source city.");
            return; }
        if (selectedTarget == null) {
            alert("Please select a Target city."); return;
        }
        if (selectedSource.id.equals(selectedTarget.id)) {
            alert("Source and Target must be different."); return;
        }

        List<Node> path = graph.dijkstra(selectedSource.id, selectedTarget.id);

        if (path.isEmpty()) {
            pathArea.setText("No path found between these cities.");
            distanceLabel.setText("--");
            mapPanel.setPath(null);
            return;
        }

        mapPanel.setPath(path);

        // Show only city names in path
        StringBuilder sb = new StringBuilder();
        int step = 1;
        for (Node n : path) {
            if (n.isCity) {
                sb.append(step++).append(". ").append(n.getDisplayName()).append("\n");
            }
        }
        pathArea.setText(sb.toString());
        distanceLabel.setText(String.format("%.3f km", graph.getPathDistance(path)));
    }

    private void clearResults() {
        pathArea.clear();
        distanceLabel.setText("--");
    }

    // Sync combobox without firing its listener
    private void syncCombo(ComboBox<String> box, String value) {
        box.setOnAction(null);
        if (value == null) box.getSelectionModel().clearSelection();
        else box.getSelectionModel().select(value);
        // Restore listener
        if (box == sourceBox) box.setOnAction(e -> {
            String val = sourceBox.getValue();
            if (val == null) return;
            Node node = graph.nodes.get(val);
            if (node == null) return;
            if (selectedTarget != null && selectedTarget.id.equals(val)) {
                selectedTarget = null;
                syncCombo(targetBox, null);
                mapPanel.setTarget(null);
            }
            selectedSource = node;
            clickState = 1;
            mapPanel.setSource(node);
            clearResults();
        });
        else box.setOnAction(e -> {
            String val = targetBox.getValue();
            if (val == null) return;
            Node node = graph.nodes.get(val);
            if (node == null) return;
            if (selectedSource != null && selectedSource.id.equals(val)) {
                syncCombo(targetBox, null);
                return;
            }
            selectedTarget = node;
            clickState = 0;
            mapPanel.setTarget(node);
            clearResults();
        });
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    static class CityCell extends ListCell<String> {
        @Override protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.replace("_", " "));
        }
    }

    public static void main(String[] args) { launch(args); }
}
