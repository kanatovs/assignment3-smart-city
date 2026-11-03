package cityplanner.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WeightedGraph {
    private final int vertexCount;
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<Edge> edges;

    public WeightedGraph(int vertexCount, boolean directed) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("Vertex count must not be negative.");
        }
        this.vertexCount = vertexCount;
        this.directed = directed;
        this.adjacency = new ArrayList<>(vertexCount);
        this.edges = new ArrayList<>();
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            adjacency.add(new ArrayList<>());
        }
    }

    public int vertexCount() {
        return vertexCount;
    }

    public int edgeCount() {
        return edges.size();
    }

    public boolean isDirected() {
        return directed;
    }

    public void addEdge(int from, int to, int weight) {
        validateVertex(from);
        validateVertex(to);

        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adjacency.get(from).add(edge);
        if (!directed && from != to) {
            adjacency.get(to).add(new Edge(to, from, weight));
        }
    }

    /** Returns a live, read-only view of logical edges in insertion order. */
    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    /** Returns a live, read-only view of edges leaving the given vertex. */
    public List<Edge> edgesFrom(int vertex) {
        validateVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    private void validateVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount) {
            throw new IllegalArgumentException("Vertex is outside the graph: " + vertex);
        }
    }
}
