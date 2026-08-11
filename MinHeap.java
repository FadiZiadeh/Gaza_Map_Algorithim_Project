package com.example.algoproj3;

public class MinHeap {

    private double[] heapDist;   // distances array
    private int[]    heapIdx;    // node indices array
    private int      heapSize;   // current number of elements
    private int      capacity;   // max number of elements

    public MinHeap(int capacity) {
        this.capacity = capacity;
        this.heapDist = new double[capacity];
        this.heapIdx  = new int[capacity];
        this.heapSize = 0;
    }


    public boolean isEmpty() {
        return heapSize == 0;
    }

    public int size() {
        return heapSize;
    }

    public void push(double dist, int nodeIndex) {
        if (heapSize == capacity) {
            // Double capacity if full
            resize();
        }
        // Place new element at the end
        heapDist[heapSize] = dist;
        heapIdx[heapSize]  = nodeIndex;
        heapSize++;

        // Bubble up to restore heap property
        bubbleUp(heapSize - 1);
    }

    public double[] pop() {
        if (isEmpty()) return null;

        // Save the minimum (always at root = index 0)
        double minDist = heapDist[0];
        int    minIdx  = heapIdx[0];

        // Move last element to root
        heapSize--;
        heapDist[0] = heapDist[heapSize];
        heapIdx[0]  = heapIdx[heapSize];

        // Bubble down to restore heap property
        bubbleDown(0);

        return new double[]{minDist, minIdx};
    }


    public double[] peek() {
        if (isEmpty()) return null;
        return new double[]{heapDist[0], heapIdx[0]};
    }


    private void bubbleUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;  // parent index formula

            if (heapDist[i] < heapDist[parent]) {
                // Child is smaller than parent → swap
                swap(i, parent);
                i = parent;  // move up
            } else {
                break;  // heap property satisfied, stop
            }
        }
    }


    private void bubbleDown(int i) {
        while (true) {
            int left     = 2 * i + 1;  // left child formula
            int right    = 2 * i + 2;  // right child formula
            int smallest = i;           // assume parent is smallest

            // Is left child smaller than current smallest?
            if (left < heapSize && heapDist[left] < heapDist[smallest]) {
                smallest = left;
            }

            // Is right child smaller than current smallest?
            if (right < heapSize && heapDist[right] < heapDist[smallest]) {
                smallest = right;
            }

            if (smallest != i) {
                // A child is smaller → swap and continue down
                swap(i, smallest);
                i = smallest;
            } else {
                break;  // heap property satisfied, stop
            }
        }
    }


    private void swap(int i, int j) {
        double tempDist = heapDist[i];
        int    tempIdx  = heapIdx[i];

        heapDist[i] = heapDist[j];
        heapIdx[i]  = heapIdx[j];

        heapDist[j] = tempDist;
        heapIdx[j]  = tempIdx;
    }


    private void resize() {
        capacity *= 2;
        double[] newDist = new double[capacity];
        int[]    newIdx  = new int[capacity];

        for (int i = 0; i < heapSize; i++) {
            newDist[i] = heapDist[i];
            newIdx[i]  = heapIdx[i];
        }

        heapDist = newDist;
        heapIdx  = newIdx;
    }
}