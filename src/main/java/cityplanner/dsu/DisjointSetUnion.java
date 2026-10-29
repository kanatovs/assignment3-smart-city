package cityplanner.dsu;

public final class DisjointSetUnion {
    private final int[] parent;
    private final int[] size;
    private int componentCount;
    private long findCalls;
    private long unionCalls;
    private long parentTraversals;

    public DisjointSetUnion(int elementCount) {
        if (elementCount < 0) {
            throw new IllegalArgumentException("Element count must not be negative.");
        }
        parent = new int[elementCount];
        size = new int[elementCount];
        componentCount = elementCount;
        for (int element = 0; element < elementCount; element++) {
            parent[element] = element;
            size[element] = 1;
        }
    }

    public int elementCount() {
        return parent.length;
    }

    public int componentCount() {
        return componentCount;
    }

    public int find(int element) {
        validateElement(element);
        findCalls++;

        int root = element;
        while (root != parent[root]) {
            root = parent[root];
            parentTraversals++;
        }

        while (element != root) {
            int next = parent[element];
            parent[element] = root;
            element = next;
            parentTraversals++;
        }
        return root;
    }

    public boolean union(int first, int second) {
        validateElement(first);
        validateElement(second);
        unionCalls++;

        int firstRoot = find(first);
        int secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }
        if (size[firstRoot] < size[secondRoot]) {
            int temporary = firstRoot;
            firstRoot = secondRoot;
            secondRoot = temporary;
        }

        parent[secondRoot] = firstRoot;
        size[firstRoot] += size[secondRoot];
        componentCount--;
        return true;
    }

    public long findCalls() {
        return findCalls;
    }

    public long unionCalls() {
        return unionCalls;
    }

    /** Counts parent links followed in both passes of find. */
    public long parentTraversals() {
        return parentTraversals;
    }

    private void validateElement(int element) {
        if (element < 0 || element >= parent.length) {
            throw new IllegalArgumentException("Element is outside the DSU: " + element);
        }
    }
}
