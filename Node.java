package com.example.algoproj3;

public class Node {
    public String id;
    public double lat;
    public double lon;
    public boolean isCity;

    public Node(String id, double lat, double lon, boolean isCity) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.isCity = isCity;
    }

    public String getDisplayName() {

        return id.replace("_", " ");
    }

    @Override
    public String toString() {
        return id;
    }
}
