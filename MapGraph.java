package com.example.algoproj3;

import java.io.*;
import java.util.*;

public class MapGraph {

    public Map<String, Node> nodes = new LinkedHashMap<>();
    public Map<String, List<String>> adjacency = new HashMap<>();
    public List<Node> cities = new ArrayList<>();

    private Set<String> changedNodes = new HashSet<>();

    public void loadFromFile(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        String mode = "";

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("CITIES")) {
                mode = "CITIES"; continue;
            } else if (line.startsWith("INTERSECTIONS")) {
                mode = "INTERSECTIONS"; continue;
            } else if (line.startsWith("EDGES")) {
                mode = "EDGES"; continue;
            }

            String[] parts = line.split("\\s+");

            if (mode.equals("CITIES") && parts.length >= 3) {
                String id = parts[0];
                double lat = Double.parseDouble(parts[1]);
                double lon = Double.parseDouble(parts[2]);
                Node node = new Node(id, lat, lon, true);
                nodes.put(id, node);
                cities.add(node);
                adjacency.put(id, new ArrayList<>());

            } else if (mode.equals("INTERSECTIONS") && parts.length >= 3) {
                String id = parts[0];
                double lat = Double.parseDouble(parts[1]);
                double lon = Double.parseDouble(parts[2]);
                Node node = new Node(id, lat, lon, false);
                nodes.put(id, node);
                adjacency.put(id, new ArrayList<>());

            } else if (mode.equals("EDGES") && parts.length >= 2) {
                String a = parts[0];
                String b = parts[1];
                if (nodes.containsKey(a) && nodes.containsKey(b)) {
                    if (!adjacency.get(a).contains(b)) adjacency.get(a).add(b);
                    if (!adjacency.get(b).contains(a)) adjacency.get(b).add(a);
                }
            }
        }
        br.close();
    }


    public double haversine(Node a, Node b) {
        final double R = 6371.0; // radius of the earth
        double dLat = Math.toRadians(b.lat - a.lat);
        double dLon = Math.toRadians(b.lon - a.lon);
        double s = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.lat)) * Math.cos(Math.toRadians(b.lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.sqrt(s));
    } //Why not just subtract coordinates?
    // Because Earth is a sphere, not flat. Two points at lat=31 that are 1 degree apart are a different distance than two points at lat=0 that are 1 degree apart.
    // Haversine accounts for this curvature.
// why do we convert them to radians bcz haversine uses trigonometric function

    public List<Node> dijkstra(String srcId, String tgtId) {
        if (!nodes.containsKey(srcId) || !nodes.containsKey(tgtId))
            return new ArrayList<>();

        //build index mapping node id → array index
        int n = nodes.size();
        String[] idArray = nodes.keySet().toArray(new String[0]);
        Map<String, Integer> indexOf = new HashMap<>();
        for (int i = 0; i < n; i++)
            indexOf.put(idArray[i], i);

        // dist[] shortest distance found so far to each node
        double[] dist = new double[n];

        // pred[] predecessor index on shortest path
        int[] pred = new int[n];

        //reset only nodes changed in last query
        for (String id : changedNodes) {
            int i = indexOf.get(id);
            dist[i] = Double.MAX_VALUE;
            pred[i] = -1;
        }
        changedNodes.clear();

        //initalization
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(pred, -1);

        int srcIdx = indexOf.get(srcId);
        int tgtIdx = indexOf.get(tgtId);

        //source node dist = zero
        dist[srcIdx] = 0.0;
        changedNodes.add(srcId);

        // Create MinHeap with capacity n*2
        // (lazy deletion may cause more than n pushes)
        MinHeap heap = new MinHeap(n * 2);
        heap.push(0.0, srcIdx);

        // MAIN DIJKSTRA LOOP
        while (!heap.isEmpty()) {

            // Get the node with smallest distance
            double[] top = heap.pop();
            double d = top[0];
            int u = (int) top[1];

            //skip outdated entries
            if (d > dist[u]) continue;

            //found target
            if (u == tgtIdx) break;

            //get neighbors of current node
            String uid = idArray[u];
            Node uNode = nodes.get(uid);
            List<String> neighbors = adjacency.get(uid);

            // Relax each edge
            for (int k = 0; k < neighbors.size(); k++) {
                String vid = neighbors.get(k);
                int v = indexOf.get(vid);
                Node vNode = nodes.get(vid);

                //compute edge weight via haversine
                double w = haversine(uNode, vNode);
                double newDist = dist[u] + w;

                //here i check if i found shorter path than v
                if (newDist < dist[v]) {
                    dist[v] = newDist;
                    pred[v] = u;
                    changedNodes.add(vid);
                    heap.push(newDist, v);
                }
            }
        }

        //path reconstruction
        List<Node> path = new ArrayList<>();

        if (dist[tgtIdx] == Double.MAX_VALUE)
            return path;// no path exists

        // follow pred backwards from the target to the source
        int cur = tgtIdx;
        while (cur != -1) {
            path.add(0, nodes.get(idArray[cur]));
            cur = pred[cur];
        }

        return path;
    } //We follow it backwards from target to source, adding to front of list each time (add(0, ...)), so the final list goes source → target.


    public double getPathDistance(List<Node> path) {
        double total = 0.0;
        for (int i = 0; i < path.size() - 1; i++)
            total += haversine(path.get(i), path.get(i + 1)); // just adds up haversine distance between each consecutive pair of nodes in the path.
        return total;
    }

    public Node findNearestCity(double lat, double lon) {
        Node dummy = new Node("_", lat, lon, false);
        Node nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Node city : cities) {
            double d = haversine(dummy, city);
            if (d < minDist) { minDist = d; nearest = city; }
        }
        return (minDist < 3.0) ? nearest : null;
    }
}
//Smallest gap between neighboring cities ≈ 2-3km
//→ threshold must be less than half that gap
//→ 3km works without causing wrong selections
//
//Canvas is 760px wide covering ~38km
//→ 1 pixel ≈ 0.05km
//→ 3km = about 60 pixels of tolerance
//→ comfortable clicking area