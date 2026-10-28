# Smart City Infrastructure and Route Planning

A Java console project for Design and Analysis of Algorithms, Assignment 3.
The city is modeled as a graph to study infrastructure costs, connectivity,
and shortest routes.

## Project status

The project includes a weighted graph model, graph tests, and build commands.
The console entry point currently prints the project title. Graph algorithms
and experiments have not been implemented yet.

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
src/test/java/cityplanner/graph/WeightedGraphTest.java Graph correctness checks
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

Graph algorithms have not been implemented yet, so their complexity analysis
is not available.

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
There are no algorithm tests yet.

Required cases include small and disconnected graphs, cycles, multiple valid
MSTs, unreachable vertices, repeated edge weights, and a single vertex.

## Conclusion

Pending implementation and experiments. The final conclusion will explain
which algorithms suit infrastructure planning, directed connectivity analysis,
dependency ordering, and route planning, using the measured results and
correctness checks.
