param(
    [string]$ResultsDirectory,
    [string]$PlotsDirectory
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$projectRoot = Split-Path -Parent $PSScriptRoot
$culture = [System.Globalization.CultureInfo]::InvariantCulture
if (-not $ResultsDirectory) {
    $ResultsDirectory = Join-Path $projectRoot 'results/tables'
}
if (-not $PlotsDirectory) {
    $PlotsDirectory = Join-Path $projectRoot 'results/plots'
}
$ResultsDirectory = [System.IO.Path]::GetFullPath($ResultsDirectory)
$PlotsDirectory = [System.IO.Path]::GetFullPath($PlotsDirectory)

function Read-Integer([string]$Value, [string]$Name) {
    $parsed = 0L
    if (-not [long]::TryParse($Value, [System.Globalization.NumberStyles]::Integer, $culture, [ref]$parsed)) {
        throw "Invalid integer in ${Name}: $Value"
    }
    return $parsed
}

function Read-Number([string]$Value, [string]$Name) {
    $parsed = 0.0
    if (-not [double]::TryParse($Value, [System.Globalization.NumberStyles]::Float, $culture, [ref]$parsed) -or
        [double]::IsNaN($parsed) -or [double]::IsInfinity($parsed) -or $parsed -lt 0) {
        throw "Invalid nonnegative finite number in ${Name}: $Value"
    }
    return $parsed
}

function Require-Columns($Rows, [string[]]$Names, [string]$File) {
    if ($Rows.Count -eq 0) {
        throw "No data rows in $File. Run the complete benchmark first."
    }
    foreach ($name in $Names) {
        if (@($Rows[0].PSObject.Properties.Name) -cnotcontains $name) {
            throw "Missing column '$name' in $File."
        }
    }
}

function Case-Key($Row) {
    $vertices = Read-Integer $Row.vertices 'vertices'
    $edges = Read-Integer $Row.edges 'edges'
    $seed = Read-Integer $Row.seed 'seed'
    return "$($Row.scenario)|$vertices|$edges|$seed|$($Row.directed)|$($Row.algorithm)"
}

function Xml([string]$Value) {
    return [System.Security.SecurityElement]::Escape($Value)
}

function Coordinate([double]$Value) {
    return $Value.ToString('0.###', $culture)
}

function Exact-Number([double]$Value) {
    return $Value.ToString('R', $culture)
}

function Axis-Label([double]$Value) {
    if ($Value -ne 0 -and ($Value -lt 0.001 -or $Value -ge 1000)) {
        return $Value.ToString('0.###E+0', $culture)
    }
    return $Value.ToString('0.###', $culture)
}

$scenarios = @(
    [pscustomobject]@{ Name = 'connected-undirected'; Title = 'Connected undirected graph'; Seed = 41100L;
        Directed = 'false'; Algorithms = @('Kruskal', 'Prim', 'Dijkstra') },
    [pscustomobject]@{ Name = 'dag-directed'; Title = 'Directed acyclic graph (DAG)'; Seed = 41200L;
        Directed = 'true'; Algorithms = @('TopologicalSort', 'Kosaraju', 'Dijkstra') },
    [pscustomobject]@{ Name = 'cyclic-directed'; Title = 'Directed graph with a cycle'; Seed = 41300L;
        Directed = 'true'; Algorithms = @('TopologicalSort', 'Kosaraju', 'Dijkstra') }
)
$sizes = @(@(10, 30), @(100, 500), @(1000, 5000))
$expected = @{}
foreach ($scenario in $scenarios) {
    for ($index = 0; $index -lt $sizes.Count; $index++) {
        foreach ($algorithm in $scenario.Algorithms) {
            $key = "$($scenario.Name)|$($sizes[$index][0])|$($sizes[$index][1])|$($scenario.Seed + $index)|$($scenario.Directed)|$algorithm"
            $expected[$key] = $true
        }
    }
}

$rawPath = Join-Path $ResultsDirectory 'all-runs.csv'
$summaryPath = Join-Path $ResultsDirectory 'all-summary.csv'
$raw = @(Import-Csv -LiteralPath $rawPath)
$summary = @(Import-Csv -LiteralPath $summaryPath)
$caseColumns = @('scenario', 'vertices', 'edges', 'seed', 'directed', 'algorithm')
Require-Columns $raw ($caseColumns + @('run', 'elapsed_ns')) $rawPath
Require-Columns $summary ($caseColumns + @('measured_runs', 'average_elapsed_ns', 'average_elapsed_ms')) $summaryPath
if ($raw.Count -ne 135 -or $summary.Count -ne 27) {
    throw 'The complete capture must contain 135 raw runs and 27 summary rows.'
}

$rawGroups = @{}
foreach ($row in $raw) {
    $key = Case-Key $row
    if (@($expected.Keys) -cnotcontains $key) {
        throw "Unexpected raw algorithm/input group: $key"
    }
    $number = Read-Integer $row.run 'run'
    $elapsed = Read-Integer $row.elapsed_ns 'elapsed_ns'
    if ($number -lt 1 -or $number -gt 5 -or $elapsed -lt 0) {
        throw 'Raw rows require run IDs 1 through 5 and nonnegative elapsed nanoseconds.'
    }
    if (-not $rawGroups.ContainsKey($key)) {
        $rawGroups[$key] = @()
    }
    $rawGroups[$key] += [pscustomobject]@{ Run = $number; Nanoseconds = $elapsed }
}

$measurements = @()
$seen = @{}
foreach ($row in $summary) {
    $key = Case-Key $row
    if (@($expected.Keys) -cnotcontains $key -or $seen.ContainsKey($key)) {
        throw "Unexpected or duplicate summary group: $key"
    }
    $seen[$key] = $true
    if (-not $rawGroups.ContainsKey($key)) {
        throw "No raw rows for summary group: $key"
    }
    $samples = @($rawGroups[$key])
    $numbers = @($samples.Run | Sort-Object -Unique)
    if ((Read-Integer $row.measured_runs 'measured_runs') -ne 5 -or $samples.Count -ne 5 -or
        ($numbers -join ',') -ne '1,2,3,4,5') {
        throw "Each summary group requires five distinct raw run IDs: $key"
    }
    $meanNs = Read-Number $row.average_elapsed_ns 'average_elapsed_ns'
    $meanMs = Read-Number $row.average_elapsed_ms 'average_elapsed_ms'
    $total = 0.0
    foreach ($sample in $samples) {
        $total += $sample.Nanoseconds
    }
    $rawMean = $total / 5.0
    $toleranceNs = [Math]::Max(0.000001, [Math]::Abs($rawMean) * 0.000000000001)
    if ([Math]::Abs($meanNs - $rawMean) -gt $toleranceNs -or
        [Math]::Abs($meanMs - $meanNs / 1000000.0) -gt $toleranceNs / 1000000.0) {
        throw "Summary mean or time units disagree with raw intervals: $key"
    }
    $range = $samples.Nanoseconds | Measure-Object -Minimum -Maximum
    $measurements += [pscustomobject]@{
        Scenario = $row.scenario; Algorithm = $row.algorithm
        Vertices = Read-Integer $row.vertices 'vertices'; Edges = Read-Integer $row.edges 'edges'
        Mean = $meanMs; Minimum = $range.Minimum / 1000000.0; Maximum = $range.Maximum / 1000000.0
    }
}
if ($seen.Count -ne $expected.Count -or $rawGroups.Count -ne $expected.Count) {
    throw 'The capture must cover every prescribed algorithm/input group.'
}

$colors = @{ Kruskal = '#2563eb'; Prim = '#dc2626'; Dijkstra = '#15803d';
    TopologicalSort = '#9333ea'; Kosaraju = '#c2410c' }

function Add-Marker([System.Text.StringBuilder]$Svg, [string]$Algorithm, [double]$X, [double]$Y) {
    $xText = Coordinate $X
    $yText = Coordinate $Y
    $color = $colors[$Algorithm]
    switch ($Algorithm) {
        'Kruskal' { [void]$Svg.AppendLine("<circle cx=`"$xText`" cy=`"$yText`" r=`"5`" fill=`"$color`"/>") }
        'Prim' { [void]$Svg.AppendLine("<rect x=`"$(Coordinate ($X - 5))`" y=`"$(Coordinate ($Y - 5))`" width=`"10`" height=`"10`" fill=`"$color`"/>") }
        'Dijkstra' {
            $points = "${xText},$(Coordinate ($Y - 6)) $(Coordinate ($X + 6)),${yText} ${xText},$(Coordinate ($Y + 6)) $(Coordinate ($X - 6)),${yText}"
            [void]$Svg.AppendLine("<polygon points=`"$points`" fill=`"$color`"/>")
        }
        'TopologicalSort' {
            $points = "${xText},$(Coordinate ($Y - 6)) $(Coordinate ($X + 6)),$(Coordinate ($Y + 5)) $(Coordinate ($X - 6)),$(Coordinate ($Y + 5))"
            [void]$Svg.AppendLine("<polygon points=`"$points`" fill=`"$color`"/>")
        }
        'Kosaraju' {
            [void]$Svg.AppendLine("<path d=`"M $(Coordinate ($X - 5)) $yText H $(Coordinate ($X + 5)) M $xText $(Coordinate ($Y - 5)) V $(Coordinate ($Y + 5))`" stroke=`"$color`" stroke-width=`"3`"/>")
        }
    }
}

function Create-Plot([string]$Dimension, [string]$Label) {
    $svg = New-Object System.Text.StringBuilder
    [void]$svg.AppendLine('<?xml version="1.0" encoding="UTF-8"?>')
    [void]$svg.AppendLine('<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="1130" viewBox="0 0 1200 1130" role="img" aria-labelledby="title description">')
    [void]$svg.AppendLine("<title id=`"title`">Execution time versus $(Xml $Label)</title>")
    [void]$svg.AppendLine('<desc id="description">Arithmetic means of five measured runs. Whiskers show the observed minimum and maximum, not a confidence interval. The horizontal axis is logarithmic; each scenario panel has its own linear millisecond scale. Points are connected by straight segments. Source data: all-summary.csv and all-runs.csv.</desc>')
    [void]$svg.AppendLine('<rect width="1200" height="1130" fill="#ffffff"/>')
    [void]$svg.AppendLine('<g font-family="Arial, sans-serif" fill="#172033">')
    [void]$svg.AppendLine("<text x=`"38`" y=`"43`" font-size=`"27`" font-weight=`"bold`">Execution time versus $(Xml $Label)</text>")
    [void]$svg.AppendLine('<text x="38" y="72" font-size="15">Mean of 5 measured runs; whiskers: min-max. Three warm-up runs are excluded.</text>')
    [void]$svg.AppendLine('<text x="38" y="96" font-size="14" fill="#4b5563">Logarithmic horizontal axis; independent linear time scales in the three panels.</text>')
    $left = 130.0
    $right = 1145.0
    $width = $right - $left
    for ($panel = 0; $panel -lt $scenarios.Count; $panel++) {
        $scenario = $scenarios[$panel]
        $panelTop = 130.0 + $panel * 320.0
        $top = $panelTop + 30.0
        $bottom = $panelTop + 220.0
        $height = $bottom - $top
        $rows = @($measurements | Where-Object { $_.Scenario -ceq $scenario.Name })
        $xValues = @($rows | ForEach-Object { $_.$Dimension } | Sort-Object -Unique)
        $xMinimum = [Math]::Log10($xValues[0])
        $xMaximum = [Math]::Log10($xValues[-1])
        $observedMax = ($rows.Maximum | Measure-Object -Maximum).Maximum
        $target = if ($observedMax -gt 0) { $observedMax * 1.1 } else { 0.001 }
        $magnitude = [Math]::Pow(10.0, [Math]::Floor([Math]::Log10($target / 4.0)))
        $scaled = $target / (4.0 * $magnitude)
        $nice = if ($scaled -le 1) { 1.0 } elseif ($scaled -le 2) { 2.0 } elseif ($scaled -le 2.5) { 2.5 } elseif ($scaled -le 5) { 5.0 } else { 10.0 }
        $step = $nice * $magnitude
        $tickCount = [int][Math]::Ceiling($target / $step)
        $yMaximum = $step * $tickCount
        [void]$svg.AppendLine("<g data-scenario=`"$(Xml $scenario.Name)`">")
        [void]$svg.AppendLine("<text x=`"38`" y=`"$(Coordinate $panelTop)`" font-size=`"18`" font-weight=`"bold`">$(Xml $scenario.Title)</text>")
        for ($legend = 0; $legend -lt $scenario.Algorithms.Count; $legend++) {
            $algorithm = $scenario.Algorithms[$legend]
            $legendX = 650.0 + $legend * 165.0
            $legendY = $panelTop - 6.0
            [void]$svg.AppendLine("<line x1=`"$(Coordinate ($legendX - 16))`" y1=`"$(Coordinate $legendY)`" x2=`"$(Coordinate ($legendX + 16))`" y2=`"$(Coordinate $legendY)`" stroke=`"$($colors[$algorithm])`" stroke-width=`"2`"/>")
            Add-Marker $svg $algorithm $legendX $legendY
            [void]$svg.AppendLine("<text x=`"$(Coordinate ($legendX + 23))`" y=`"$(Coordinate ($legendY + 5))`" font-size=`"13`">$(Xml $algorithm)</text>")
        }
        for ($tick = 0; $tick -le $tickCount; $tick++) {
            $value = $tick * $step
            $y = $bottom - $height * $value / $yMaximum
            [void]$svg.AppendLine("<line x1=`"$(Coordinate $left)`" y1=`"$(Coordinate $y)`" x2=`"$(Coordinate $right)`" y2=`"$(Coordinate $y)`" stroke=`"#e5e7eb`"/>")
            [void]$svg.AppendLine("<text x=`"$(Coordinate ($left - 13))`" y=`"$(Coordinate ($y + 5))`" text-anchor=`"end`" font-size=`"13`">$(Axis-Label $value)</text>")
        }
        foreach ($value in $xValues) {
            $x = $left + $width * ([Math]::Log10($value) - $xMinimum) / ($xMaximum - $xMinimum)
            [void]$svg.AppendLine("<line x1=`"$(Coordinate $x)`" y1=`"$(Coordinate $top)`" x2=`"$(Coordinate $x)`" y2=`"$(Coordinate $bottom)`" stroke=`"#e5e7eb`"/>")
            [void]$svg.AppendLine("<text x=`"$(Coordinate $x)`" y=`"$(Coordinate ($bottom + 23))`" text-anchor=`"middle`" font-size=`"13`">$value</text>")
        }
        [void]$svg.AppendLine("<path d=`"M $(Coordinate $left) $(Coordinate $top) V $(Coordinate $bottom) H $(Coordinate $right)`" stroke=`"#6b7280`" fill=`"none`"/>")
        $centerY = ($top + $bottom) / 2.0
        [void]$svg.AppendLine("<text x=`"38`" y=`"$(Coordinate $centerY)`" transform=`"rotate(-90 38 $(Coordinate $centerY))`" text-anchor=`"middle`" font-size=`"14`">Execution time (ms)</text>")
        [void]$svg.AppendLine("<text x=`"$(Coordinate (($left + $right) / 2))`" y=`"$(Coordinate ($bottom + 48))`" text-anchor=`"middle`" font-size=`"14`">$(Xml $Label) (logarithmic axis)</text>")
        foreach ($algorithm in $scenario.Algorithms) {
            $series = @($rows | Where-Object { $_.Algorithm -ceq $algorithm } | Sort-Object -Property $Dimension)
            $points = @()
            foreach ($row in $series) {
                $x = $left + $width * ([Math]::Log10($row.$Dimension) - $xMinimum) / ($xMaximum - $xMinimum)
                $y = $bottom - $height * $row.Mean / $yMaximum
                $points += "$(Coordinate $x),$(Coordinate $y)"
            }
            [void]$svg.AppendLine("<polyline points=`"$($points -join ' ')`" stroke=`"$($colors[$algorithm])`" stroke-width=`"2`" fill=`"none`"/>")
            foreach ($row in $series) {
                $x = $left + $width * ([Math]::Log10($row.$Dimension) - $xMinimum) / ($xMaximum - $xMinimum)
                $y = $bottom - $height * $row.Mean / $yMaximum
                $minimumY = $bottom - $height * $row.Minimum / $yMaximum
                $maximumY = $bottom - $height * $row.Maximum / $yMaximum
                [void]$svg.AppendLine("<g class=`"measurement`" data-algorithm=`"$(Xml $algorithm)`" data-vertices=`"$($row.Vertices)`" data-edges=`"$($row.Edges)`" data-mean-ms=`"$(Exact-Number $row.Mean)`" data-min-ms=`"$(Exact-Number $row.Minimum)`" data-max-ms=`"$(Exact-Number $row.Maximum)`">")
                [void]$svg.AppendLine("<title>$(Xml $algorithm): V=$($row.Vertices), E=$($row.Edges); mean=$(Exact-Number $row.Mean) ms, min=$(Exact-Number $row.Minimum) ms, max=$(Exact-Number $row.Maximum) ms</title>")
                [void]$svg.AppendLine("<path d=`"M $(Coordinate $x) $(Coordinate $maximumY) V $(Coordinate $minimumY) M $(Coordinate ($x - 4)) $(Coordinate $maximumY) H $(Coordinate ($x + 4)) M $(Coordinate ($x - 4)) $(Coordinate $minimumY) H $(Coordinate ($x + 4))`" fill=`"none`" stroke=`"$($colors[$algorithm])`" stroke-width=`"1.3`" opacity=`"0.7`"/>")
                Add-Marker $svg $algorithm $x $y
                [void]$svg.AppendLine('</g>')
            }
        }
        [void]$svg.AppendLine('</g>')
    }
    [void]$svg.AppendLine('<text x="38" y="1082" font-size="14" fill="#4b5563">V and E increase together in these inputs; the two plots do not isolate their individual effects.</text>')
    [void]$svg.AppendLine('<text x="38" y="1106" font-size="13" fill="#4b5563">Whiskers describe five observations, not uncertainty bounds. Times include operation counters.</text>')
    [void]$svg.AppendLine('</g></svg>')
    return $svg.ToString().Replace("`r`n", "`n")
}

# Build both documents before writing so validation failures leave existing plots intact.
$verticesPlot = Create-Plot 'Vertices' 'Vertices V'
$edgesPlot = Create-Plot 'Edges' 'Edges E'
[void][System.IO.Directory]::CreateDirectory($PlotsDirectory)
$encoding = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Join-Path $PlotsDirectory 'time-vs-vertices.svg'), $verticesPlot, $encoding)
[System.IO.File]::WriteAllText((Join-Path $PlotsDirectory 'time-vs-edges.svg'), $edgesPlot, $encoding)
Write-Output "Plots saved to $PlotsDirectory"
Write-Output 'Validated 135 raw runs and 27 means; each SVG contains 27 measured points and min-max whiskers.'
