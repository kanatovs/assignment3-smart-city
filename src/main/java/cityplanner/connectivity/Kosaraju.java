package cityplanner.connectivity;

import cityplanner.graph.Edge;
import cityplanner.graph.WeightedGraph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class Kosaraju {
    private long firstPassEdgeExaminations;
    private long reversalEdgeExaminations;
    private long secondPassEdgeExaminations;

    public SccResult findStronglyConnectedComponents(WeightedGraph graph) {
        Objects.requireNonNull(graph, "Graph must not be null.");
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Strongly connected components require a directed graph.");
        }

        firstPassEdgeExaminations = 0;
        reversalEdgeExaminations = 0;
        secondPassEdgeExaminations = 0;

        boolean[] visited = new boolean[graph.vertexCount()];
        List<Integer> finishOrder = findFinishOrder(graph, visited);
        WeightedGraph reversed = new WeightedGraph(graph.vertexCount(), true);
        for (Edge edge : graph.edges()) {
            reversed.addEdge(edge.to(), edge.from(), edge.weight());
            reversalEdgeExaminations++;
        }

        Arrays.fill(visited, false);
        List<List<Integer>> components = new ArrayList<>();
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        for (int index = finishOrder.size() - 1; index >= 0; index--) {
            int start = finishOrder.get(index);
            if (visited[start]) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            visited[start] = true;
            pending.push(start);
            while (!pending.isEmpty()) {
                int vertex = pending.pop();
                component.add(vertex);
                for (Edge edge : reversed.edgesFrom(vertex)) {
                    secondPassEdgeExaminations++;
                    if (!visited[edge.to()]) {
                        visited[edge.to()] = true;
                        pending.push(edge.to());
                    }
                }
            }
            components.add(component);
        }

        return new SccResult(components);
    }

    private List<Integer> findFinishOrder(WeightedGraph graph, boolean[] visited) {
        List<Integer> finishOrder = new ArrayList<>();
        int[] nextEdge = new int[graph.vertexCount()];
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) {
                continue;
            }
            visited[start] = true;
            pending.push(start);
            while (!pending.isEmpty()) {
                int vertex = pending.peek();
                List<Edge> outgoing = graph.edgesFrom(vertex);
                if (nextEdge[vertex] < outgoing.size()) {
                    Edge edge = outgoing.get(nextEdge[vertex]++);
                    firstPassEdgeExaminations++;
                    if (!visited[edge.to()]) {
                        visited[edge.to()] = true;
                        pending.push(edge.to());
                    }
                } else {
                    finishOrder.add(pending.pop());
                }
            }
        }
        return finishOrder;
    }

    public long firstPassEdgeExaminations() {
        return firstPassEdgeExaminations;
    }

    public long reversalEdgeExaminations() {
        return reversalEdgeExaminations;
    }

    public long secondPassEdgeExaminations() {
        return secondPassEdgeExaminations;
    }

    public long edgeExaminations() {
        return firstPassEdgeExaminations + reversalEdgeExaminations + secondPassEdgeExaminations;
    }
}
