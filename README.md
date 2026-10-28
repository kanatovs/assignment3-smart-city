# Smart City Infrastructure and Route Planning

A Java console project for Design and Analysis of Algorithms, Assignment 3.
The city is modeled as a graph to study infrastructure costs, connectivity,
and shortest routes.

## Project status

The application currently prints the project title. Build commands and a startup
check are available. Graph algorithms, algorithm tests, and experiments have not
been implemented yet.

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

Both commands rebuild the generated classes from scratch. `check` enables all
compiler warnings, treats warnings as errors, and verifies application startup.
`run` starts the application after compilation. Commands also work from other
directories when the script is called by its full path.

`-ExecutionPolicy Bypass` permits the local script in this PowerShell process
only; it does not change the machine or user execution policy. It is needed on
the current development machine because its default policy blocks `.ps1` files.

To select a JDK explicitly for the current PowerShell session:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\project.ps1 check
```

For the current foundation, the equivalent commands on Linux or macOS are:

```sh
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -Xlint:all -Werror \
  -d build/classes src/main/java/cityplanner/Main.java
java -cp build/classes cityplanner.Main
```

These Unix commands have not yet been executed on a Unix system. They will be
updated when more source files are added.

Current application output:

```text
Smart City Infrastructure and Route Planning
```

## Overview

The planned graph model uses vertices for city districts, edges for connections,
and weights for construction costs or travel distances. Minimum spanning trees
will use undirected graphs. Connectivity experiments will use directed graphs.
Shortest-path experiments will use non-negative edge weights.

## Implementation

Current source layout:

```text
src/main/java/cityplanner/Main.java   Console entry point
scripts/project.ps1                 Build, startup check, and run commands
```

`Main` prints the project title. The PowerShell script compiles the source files
with `javac` and runs the application with `java`, using tools from the same JDK.

## Complexity Analysis

The algorithms have not been implemented yet. Their time and space complexity
analysis is not available.

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

The current check compiles the application with all compiler warnings enabled
and verifies the startup output. There are no algorithm tests yet.

Required cases include small and disconnected graphs, cycles, multiple valid
MSTs, unreachable vertices, repeated edge weights, and a single vertex.

## Conclusion

Pending implementation and experiments. The final conclusion will explain
which algorithms suit infrastructure planning, directed connectivity analysis,
dependency ordering, and route planning, using the measured results and
correctness checks.
