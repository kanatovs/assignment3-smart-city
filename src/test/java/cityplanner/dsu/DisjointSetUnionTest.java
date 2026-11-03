package cityplanner.dsu;

import java.util.Random;

public final class DisjointSetUnionTest {
    private DisjointSetUnionTest() {
    }

    public static void main(String[] args) {
        initialGroupsAreSeparate();
        emptyStructureRejectsOperations();
        singleElementRemainsItsOwnGroup();
        negativeCountsAreRejected();
        unionsConnectGroupsTransitively();
        redundantAndSelfUnionsDoNotMerge();
        unionBySizeKeepsLargerGroupRoot();
        findCompressesBalancedPaths();
        countersIncludeInternalAndRedundantFinds();
        invalidCallsLeaveStateAndCountersUnchanged();
        randomOperationsMatchComponentLabels();
        System.out.println("DisjointSetUnionTest passed.");
    }

    private static void initialGroupsAreSeparate() {
        DisjointSetUnion dsu = new DisjointSetUnion(5);

        check(dsu.elementCount() == 5, "Every element must be counted.");
        check(dsu.componentCount() == 5, "Each element must start in its own group.");
        checkCounters(dsu, 0, 0, 0);
        for (int element = 0; element < dsu.elementCount(); element++) {
            check(dsu.find(element) == element, "An isolated element must be its own root.");
        }
        checkCounters(dsu, 5, 0, 0);
        check(dsu.componentCount() == 5, "Searching must not merge groups.");
    }

    private static void emptyStructureRejectsOperations() {
        DisjointSetUnion dsu = new DisjointSetUnion(0);

        check(dsu.elementCount() == 0, "The empty structure must have no elements.");
        check(dsu.componentCount() == 0, "The empty structure must have no groups.");
        expectIllegalArgument(() -> dsu.find(0));
        expectIllegalArgument(() -> dsu.find(-1));
        expectIllegalArgument(() -> dsu.union(0, 0));
        checkCounters(dsu, 0, 0, 0);
    }

    private static void singleElementRemainsItsOwnGroup() {
        DisjointSetUnion dsu = new DisjointSetUnion(1);

        check(dsu.find(0) == 0, "The only element must be its own root.");
        check(!dsu.union(0, 0), "A self-union must not merge groups.");
        check(dsu.elementCount() == 1, "A union must not change the element count.");
        check(dsu.componentCount() == 1, "The single group must remain intact.");
        checkCounters(dsu, 3, 1, 0);
    }

    private static void negativeCountsAreRejected() {
        expectIllegalArgument(() -> new DisjointSetUnion(-1));
        expectIllegalArgument(() -> new DisjointSetUnion(Integer.MIN_VALUE));
    }

    private static void unionsConnectGroupsTransitively() {
        DisjointSetUnion dsu = new DisjointSetUnion(6);

        check(dsu.union(0, 1), "Separate elements must merge.");
        check(dsu.union(2, 3), "Another pair must merge independently.");
        check(dsu.union(1, 2), "A union must merge the complete groups of its arguments.");
        check(dsu.union(4, 5), "The remaining pair must merge.");
        check(dsu.componentCount() == 2, "Four successful merges must leave two groups.");
        check(dsu.find(0) == dsu.find(3), "Connectivity must be transitive.");
        check(dsu.find(0) != dsu.find(4), "Unmerged groups must stay separate.");
        check(dsu.union(3, 5), "A union through non-root elements must merge their groups.");
        check(dsu.componentCount() == 1, "The final merge must leave one group.");
        int root = dsu.find(0);
        for (int element = 1; element < dsu.elementCount(); element++) {
            check(dsu.find(element) == root, "Every element must belong to the merged group.");
        }
        check(dsu.elementCount() == 6, "Merging must preserve the element count.");
    }

    private static void redundantAndSelfUnionsDoNotMerge() {
        DisjointSetUnion dsu = new DisjointSetUnion(3);

        check(dsu.union(0, 1), "The initial merge must succeed.");
        check(!dsu.union(0, 1), "Repeating a merge must return false.");
        check(!dsu.union(1, 0), "Reversing an existing merge must return false.");
        check(!dsu.union(2, 2), "An isolated element must not merge with itself.");
        check(dsu.componentCount() == 2, "Redundant unions must preserve the group count.");
        check(dsu.find(0) != dsu.find(2), "A self-union must not join another group.");
    }

    private static void unionBySizeKeepsLargerGroupRoot() {
        DisjointSetUnion dsu = new DisjointSetUnion(7);
        dsu.union(4, 5);
        dsu.union(4, 6);
        dsu.union(0, 1);
        int largerRoot = dsu.find(4);
        int smallerRoot = dsu.find(0);

        check(largerRoot != smallerRoot, "The two groups must start separate.");
        check(dsu.union(1, 5), "Groups of sizes two and three must merge.");
        check(dsu.find(smallerRoot) == largerRoot,
                "The larger group's root must survive even when its group is the second argument.");
        check(dsu.find(0) == largerRoot, "The smaller group's elements must reach the larger root.");
        check(dsu.find(6) == largerRoot, "The larger group's elements must retain their root.");
        check(dsu.componentCount() == 3, "The merged group and two isolated elements must remain.");
    }

    private static void findCompressesBalancedPaths() {
        DeepPath path = createDeepPath();
        DisjointSetUnion dsu = path.dsu();
        long findsBefore = dsu.findCalls();
        long unionsBefore = dsu.unionCalls();
        long traversalsBefore = dsu.parentTraversals();

        check(dsu.find(path.element()) == path.root(), "The deep element must reach its group root.");
        check(dsu.parentTraversals() - traversalsBefore == 4,
                "Two passes over a path of length two must count four parent traversals.");
        long traversalsAfterFirstSearch = dsu.parentTraversals();
        check(dsu.find(path.element()) == path.root(), "Compression must preserve group membership.");
        check(dsu.parentTraversals() - traversalsAfterFirstSearch == 2,
                "The compressed path must contain only one link in each pass.");
        check(dsu.findCalls() == findsBefore + 2, "Both explicit searches must be counted.");
        check(dsu.unionCalls() == unionsBefore, "Compression must not count as a union.");
        check(dsu.componentCount() == 2, "Compression must not merge the isolated element.");
    }

    private static void countersIncludeInternalAndRedundantFinds() {
        DisjointSetUnion dsu = new DisjointSetUnion(4);

        dsu.find(3);
        checkCounters(dsu, 1, 0, 0);
        check(dsu.union(0, 1), "The first merge must succeed.");
        checkCounters(dsu, 3, 1, 0);
        int firstRoot = dsu.find(0);
        int secondRoot = dsu.find(1);
        check(firstRoot == secondRoot, "The merged pair must have one root.");
        // Exactly one member is a non-root, regardless of which root wins a size tie.
        checkCounters(dsu, 5, 1, 2);
        check(!dsu.union(0, 1), "A redundant merge must return false.");
        checkCounters(dsu, 7, 2, 4);
        check(!dsu.union(2, 2), "A self-union must return false.");
        checkCounters(dsu, 9, 3, 4);
        check(dsu.union(2, 3), "The remaining isolated pair must merge.");
        checkCounters(dsu, 11, 4, 4);
        check(dsu.componentCount() == 2, "Only successful unions must reduce the group count.");
    }

    private static void invalidCallsLeaveStateAndCountersUnchanged() {
        DeepPath path = createDeepPath();
        DisjointSetUnion dsu = path.dsu();
        long findsBefore = dsu.findCalls();
        long unionsBefore = dsu.unionCalls();
        long traversalsBefore = dsu.parentTraversals();
        int componentsBefore = dsu.componentCount();

        for (int invalid : new int[] {-1, dsu.elementCount(), Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            expectIllegalArgument(() -> dsu.find(invalid));
            expectIllegalArgument(() -> dsu.union(invalid, path.element()));
            expectIllegalArgument(() -> dsu.union(path.element(), invalid));
        }
        checkCounters(dsu, findsBefore, unionsBefore, traversalsBefore);
        check(dsu.componentCount() == componentsBefore, "Invalid calls must preserve the group count.");
        check(dsu.find(path.element()) == path.root(), "Invalid calls must preserve group membership.");
        check(dsu.parentTraversals() - traversalsBefore == 4,
                "An invalid second argument must not compress the valid first argument's path.");
        check(dsu.find(4) != path.root(), "Invalid unions must leave the isolated element separate.");
    }

    private static void randomOperationsMatchComponentLabels() {
        for (long seed : new long[] {17L, 2026L, 987654321L}) {
            int elementCount = 48;
            DisjointSetUnion dsu = new DisjointSetUnion(elementCount);
            int[] labels = new int[elementCount];
            for (int element = 0; element < elementCount; element++) {
                labels[element] = element;
            }
            Random random = new Random(seed);
            int expectedComponents = elementCount;
            long expectedFindCalls = 0;
            long expectedUnionCalls = 0;

            for (int step = 0; step < 300; step++) {
                int first = random.nextInt(elementCount);
                int second = random.nextInt(elementCount);
                if (random.nextBoolean()) {
                    boolean shouldMerge = labels[first] != labels[second];
                    check(dsu.union(first, second) == shouldMerge,
                            "Union result disagrees with component labels at step " + step + ".");
                    expectedFindCalls += 2;
                    expectedUnionCalls++;
                    if (shouldMerge) {
                        int oldLabel = labels[second];
                        int newLabel = labels[first];
                        for (int element = 0; element < elementCount; element++) {
                            if (labels[element] == oldLabel) {
                                labels[element] = newLabel;
                            }
                        }
                        expectedComponents--;
                    }
                } else {
                    boolean connected = dsu.find(first) == dsu.find(second);
                    expectedFindCalls += 2;
                    check(connected == (labels[first] == labels[second]),
                            "Connectivity disagrees with component labels at step " + step + ".");
                }
                check(dsu.componentCount() == expectedComponents,
                        "Group count disagrees with component labels at step " + step + ".");
            }

            int[] roots = new int[elementCount];
            for (int element = 0; element < elementCount; element++) {
                roots[element] = dsu.find(element);
                expectedFindCalls++;
                check(roots[element] >= 0 && roots[element] < elementCount,
                        "A root must be a valid element.");
                check(labels[roots[element]] == labels[element], "A root must belong to its element's group.");
            }
            for (int first = 0; first < elementCount; first++) {
                for (int second = 0; second < elementCount; second++) {
                    check((roots[first] == roots[second]) == (labels[first] == labels[second]),
                            "Final groups must match the component-label model.");
                }
            }
            check(dsu.elementCount() == elementCount, "Random operations must preserve the element count.");
            check(dsu.findCalls() == expectedFindCalls, "Every random search must be counted.");
            check(dsu.unionCalls() == expectedUnionCalls, "Every random union attempt must be counted.");
        }
    }

    private static DeepPath createDeepPath() {
        DisjointSetUnion dsu = new DisjointSetUnion(5);
        dsu.union(0, 1);
        dsu.union(2, 3);
        int firstRoot = dsu.find(0);
        int secondRoot = dsu.find(2);
        int firstLeaf = firstRoot == 0 ? 1 : 0;
        int secondLeaf = secondRoot == 2 ? 3 : 2;
        dsu.union(firstRoot, secondRoot);
        int root = dsu.find(firstRoot);

        // The losing size-two group leaves its leaf two links below the surviving root.
        int deepElement = root == firstRoot ? secondLeaf : firstLeaf;
        return new DeepPath(dsu, deepElement, root);
    }

    private static void checkCounters(DisjointSetUnion dsu, long finds, long unions, long traversals) {
        check(dsu.findCalls() == finds, "The find-call counter is incorrect.");
        check(dsu.unionCalls() == unions, "The union-call counter is incorrect.");
        check(dsu.parentTraversals() == traversals, "The parent-traversal counter is incorrect.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException.");
    }

    private record DeepPath(DisjointSetUnion dsu, int element, int root) {
    }
}
