# Smart City Infrastructure and Route Planning

A Java console project for Design and Analysis of Algorithms, Assignment 3.
The city is modeled as a graph to study infrastructure costs, connectivity,
and shortest routes.

## Project status

The project includes a weighted graph model, a disjoint set union structure,
graph and DSU tests, and build commands. The console entry point currently
prints the project title. MST, connectivity, and shortest-path algorithms and
experiments have not been implemented yet.

## Build and run

### Requirements

- JDK 17 or newer, including both `java` and `javac`.
- Windows PowerShell 5.1 or newer for the convenience script.
- No external Java libraries, Maven, or Gradle are required.

The script uses `JAVA_HOME` when it is set. Otherwise, it finds `javac` on
`PATH` and uses `java` from the same directory. This avoids mixing a Java 17
compiler with an older Java runtime. A `JAVA_HOME` pointing to a JRE is not
sufficient.

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\project.ps1 check
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\project.ps1 run
```

Both commands rebuild the generated classes from scratch. `check` compiles the
application and tests with all compiler warnings enabled and treated as errors,
verifies application startup, and runs every `*Test.java` class under
`src/test/java`. Each test class has a `main` method and a package matching its
directory. A failed test stops the command with a nonzero exit code.

`run` compiles and starts the application without running tests. Commands also
work from other directories when the script is called by its full path.

`-ExecutionPolicy Bypass` permits the local script in this PowerShell process
only; it does not change the machine or user execution policy. It is needed on
the current development machine because its default policy blocks `.ps1` files.

To select a JDK explicitly for the current PowerShell session:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\project.ps1 check
```

For the current source files, the equivalent commands on Linux or macOS are:

```sh
mkdir -p build/classes build/test-classes
find src/main/java -name '*.java' | sort > build/main-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -Werror \
  -d build/classes @build/main-sources.txt
find src/test/java -name '*.java' | sort > build/test-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -Werror \
  -cp build/classes -d build/test-classes @build/test-sources.txt
java -cp build/classes:build/test-classes cityplanner.graph.WeightedGraphTest
java -cp build/classes:build/test-classes cityplanner.dsu.DisjointSetUnionTest
java -cp build/classes cityplanner.Main
```

These Unix commands have not yet been executed on a Unix system.

Current application output:

```text
Smart City Infrastructure and Route Planning
```

## Overview

The graph model uses vertices for city districts, edges for connections, and
weights for construction costs or travel distances. It supports directed and
undirected graphs. Minimum spanning trees require undirected graphs, while
the directed connectivity tasks use one-way connections. Shortest-path
experiments will use non-negative edge weights.

## Implementation

Current source layout:

```text
src/main/java/cityplanner/Main.java                   Console entry point
src/main/java/cityplanner/graph/Edge.java             Immutable edge value
src/main/java/cityplanner/graph/WeightedGraph.java    Adjacency-list graph
src/main/java/cityplanner/dsu/DisjointSetUnion.java   Groups with union and find
src/test/java/cityplanner/graph/WeightedGraphTest.java Graph correctness checks
src/test/java/cityplanner/dsu/DisjointSetUnionTest.java DSU correctness checks
scripts/project.ps1                                 Build, test, and run commands
```

`Main` prints the project title. The PowerShell script compiles the source files
with `javac` and runs the application with `java`, using tools from the same JDK.

### Graph representation

`WeightedGraph(vertexCount, directed)` creates a fixed set of vertices numbered
from `0` to `vertexCount - 1`. An empty graph is allowed; a negative vertex count
is rejected. `addEdge(from, to, weight)` checks both vertex IDs before changing
the graph. Invalid IDs cause `IllegalArgumentException` without adding an edge.

`Edge` is an immutable Java record with `from`, `to`, and `weight` values.
Weights use `int` and may be zero or negative. Negative weights are valid graph
data; a shortest-path algorithm that cannot handle them must reject them.

An edge is stored once in the logical edge list returned by `edges()`.
For an undirected edge, adjacency contains both orientations. A self-loop is
stored only once in both the logical list and its adjacency list. Parallel
edges, including identical edges, are allowed and count separately in
`edgeCount()`. All lists preserve insertion order.

`edgesFrom(vertex)` returns outgoing adjacency entries, each with that vertex
as `from`. Both it and `edges()` return read-only live views: callers cannot
change the lists, but later additions to the graph are visible through an
existing view. The mutable graph is intended for single-threaded use.

For example, this graph has two logical edges and one isolated vertex:

```java
WeightedGraph graph = new WeightedGraph(4, false);
graph.addEdge(0, 1, 7);
graph.addEdge(1, 2, 4);
```

`edgesFrom(1)` contains `(1, 0, 7)` and `(1, 2, 4)`, while `edgesFrom(3)` is empty.

### Disjoint Set Union

`DisjointSetUnion(elementCount)` starts with one group per element, numbered
from `0` to `elementCount - 1`. Zero elements are allowed; a negative count is
rejected. The structure stores a parent and a group size in two integer arrays.

`find(element)` returns the root representing its group. It first follows parent
links to the root, then makes every element on that path point directly to the
root. This path compression shortens later searches. Both passes are iterative.

`union(first, second)` finds both roots and attaches the smaller group below
the larger group. This union-by-size rule prevents long chains. A successful
merge returns `true` and reduces `componentCount()` by one. If both elements
already share a group, it returns `false` and leaves the group count unchanged.
Root IDs are implementation details and can change after a merge.

Both arguments are validated before a union changes any parents or counters.
Invalid element IDs cause `IllegalArgumentException`. The arrays are private;
callers change group membership through `union`. Even `find` can change parents
through compression, so this structure is intended for single-threaded use.

The structure records these counters for later performance measurements:

| Counter | Meaning |
| --- | --- |
| `findCalls()` | Valid `find` calls, including the two searches inside each valid `union` |
| `unionCalls()` | Valid merge attempts, including attempts on an already connected pair |
| `parentTraversals()` | Parent links followed in both the root search and compression pass |

A search starting `h` links below its root counts `2h` parent traversals.
A root search counts zero traversals. Invalid calls do not change the counters.
Construction does not count as a search or merge; counters start at zero.

## Complexity Analysis

The graph representation requires `O(V + E)` storage. Each undirected non-loop
edge contributes two adjacency entries, which does not change this bound.

| Graph operation | Time |
| --- | --- |
| Create a graph | `O(V)` |
| Add an edge | Amortized `O(1)` |
| Read vertex count, edge count, or direction | `O(1)` |
| Obtain an edge or adjacency view | `O(1)` |
| Iterate all logical edges | `O(E)` |
| Iterate outgoing edges from a vertex | `O(outgoing adjacency entries)` |

DSU initialization takes `O(V)` time and its arrays require `O(V)` space.
`find` and `union` take amortized `O(alpha(V))` time with path compression and
union by size, where `alpha` is the inverse Ackermann function and grows very
slowly. This is an amortized bound over a sequence, not a constant-time guarantee
for each call. A single search can still take `O(log V)` time. Counts and counter
getters take `O(1)` time.

Union by size limits tree depth by attaching a smaller group below a larger
one. Path compression then reduces the depth of visited elements. The iterative
implementation uses `O(1)` extra space for a search or merge.

MST, connectivity, and shortest-path algorithms have not been implemented yet,
so their complexity analysis is not available.

## Experimental Setup

The assignment requires graphs with `(V, E)` equal to `(10, 30)`, `(100, 500)`,
and `(1000, 5000)`. Experiments will use fixed random seeds and five measured
runs per case, timed with `System.nanoTime()`.

Graph generation, warm-up, and console output will stay outside the timed
algorithm call. Generation policies, operation counter definitions, timer
boundaries, JDK version, and machine details will be recorded with the results.

## Results

No measurements have been collected. Raw runs and summary tables will be stored
in `results/tables/`; plots will be stored in `results/plots/` and embedded here.
The report will include execution time against both vertex count and edge count.

## Comparison and Discussion

Pending measured results. The discussion will compare Kruskal and Prim,
explain DSU optimizations, and examine SCC, topological ordering, and Dijkstra
as the graphs grow. Since the required cases increase both V and E together,
the two required plots will not independently isolate their effects.

## Testing

`WeightedGraphTest` contains 11 test scenarios covering directed and undirected
adjacency, empty and single-vertex graphs, isolated vertices, parallel edges,
repeated weights, self-loops, zero and negative weights, and integer weight
boundaries. It also checks invalid input without partial graph changes, rejects
mutation through returned views, and verifies that live views reflect additions.

Checks throw `AssertionError` on failure and do not depend on Java's optional
`assert` flag. The PowerShell `check` command compiles all test sources into
`build/test-classes`, runs each test entry point, and also checks console startup.

`DisjointSetUnionTest` contains 11 scenarios covering initial groups, empty and
single-element inputs, successful and repeated unions, invalid indices, union
by size, path compression, and exact operation counter values. It also compares
900 generated operations across three fixed seeds with an independent reference
model that stores a group label for each element, then checks the complete final
partition. Invalid union arguments are checked before any compression or counter
change can occur.

Required cases include small and disconnected graphs, cycles, multiple valid
MSTs, unreachable vertices, repeated edge weights, and a single vertex.

## Conclusion

Pending implementation and experiments. The final conclusion will explain
which algorithms suit infrastructure planning, directed connectivity analysis,
dependency ordering, and route planning, using the measured results and
correctness checks.
