param(
    [ValidateSet('check', 'run')]
    [string]$Task = 'check'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

if ($env:JAVA_HOME) {
    $jdkBin = Join-Path $env:JAVA_HOME 'bin'
} else {
    $compilerCommand = Get-Command javac -CommandType Application -ErrorAction SilentlyContinue
    if (-not $compilerCommand) {
        throw 'JDK 17 or newer is required. Set JAVA_HOME to the JDK installation directory.'
    }
    $jdkBin = Split-Path -Parent $compilerCommand.Source
}

$javac = Join-Path $jdkBin 'javac.exe'
$java = Join-Path $jdkBin 'java.exe'
if (-not (Test-Path -LiteralPath $javac -PathType Leaf) -or
    -not (Test-Path -LiteralPath $java -PathType Leaf)) {
    throw 'The selected Java installation must contain both bin/javac.exe and bin/java.exe.'
}

$compilerVersion = & $javac --version
if ($LASTEXITCODE -ne 0) {
    throw 'Could not determine the JDK version. JDK 17 or newer is required.'
}
if ($compilerVersion -notmatch '^javac (\d+)' -or [int]$Matches[1] -lt 17) {
    throw "JDK 17 or newer is required; found $compilerVersion."
}

$sourceDirectory = Join-Path $projectRoot 'src/main/java'
$classesDirectory = [System.IO.Path]::GetFullPath((Join-Path $projectRoot 'build/classes'))
$testSourceDirectory = Join-Path $projectRoot 'src/test/java'
$testClassesDirectory = [System.IO.Path]::GetFullPath((Join-Path $projectRoot 'build/test-classes'))
$sourceFiles = @(Get-ChildItem -LiteralPath $sourceDirectory -Recurse -Filter '*.java' |
    Sort-Object FullName | Select-Object -ExpandProperty FullName)
if ($sourceFiles.Count -eq 0) {
    throw 'No Java source files were found.'
}

foreach ($outputDirectory in @($classesDirectory, $testClassesDirectory)) {
    if (-not (Test-Path -LiteralPath $outputDirectory)) {
        continue
    }
    $resolvedOutputDirectory = (Resolve-Path -LiteralPath $outputDirectory).ProviderPath
    $expectedPrefix = [System.IO.Path]::GetFullPath($projectRoot) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolvedOutputDirectory.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'The generated classes directory must be inside the project.'
    }
    $outputDirectories = @((Get-Item -LiteralPath (Split-Path -Parent $outputDirectory)),
        (Get-Item -LiteralPath $outputDirectory))
    $outputDirectories += @(Get-ChildItem -LiteralPath $outputDirectory -Recurse -Force -Directory)
    if ($outputDirectories | Where-Object { $_.Attributes -band [System.IO.FileAttributes]::ReparsePoint }) {
        throw 'The generated classes directory must not contain directory links.'
    }
    Remove-Item -LiteralPath $resolvedOutputDirectory -Recurse -Force
}
New-Item -ItemType Directory -Path $classesDirectory -Force | Out-Null
& $javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d $classesDirectory @sourceFiles
if ($LASTEXITCODE -ne 0) {
    throw 'Java compilation failed.'
}

if ($Task -eq 'run') {
    & $java -cp $classesDirectory cityplanner.Main
    if ($LASTEXITCODE -ne 0) {
        throw 'Application execution failed.'
    }
    exit 0
}

$output = & $java -cp $classesDirectory cityplanner.Main
if ($LASTEXITCODE -ne 0) {
    throw 'Application startup failed.'
}
if (@($output).Count -ne 1 -or $output -ne 'Smart City Infrastructure and Route Planning') {
    throw 'The startup smoke check returned unexpected output.'
}

$testSourceFiles = @(Get-ChildItem -LiteralPath $testSourceDirectory -Recurse -Filter '*.java' |
    Sort-Object FullName)
$testEntryPoints = @($testSourceFiles | Where-Object { $_.Name -like '*Test.java' })
if ($testEntryPoints.Count -eq 0) {
    throw 'No executable test classes were found under src/test/java.'
}

New-Item -ItemType Directory -Path $testClassesDirectory -Force | Out-Null
$testSourcePaths = @($testSourceFiles | Select-Object -ExpandProperty FullName)
& $javac --release 17 -encoding UTF-8 -Xlint:all -Werror -cp $classesDirectory -d $testClassesDirectory @testSourcePaths
if ($LASTEXITCODE -ne 0) {
    throw 'Java test compilation failed.'
}

$testClassPath = $classesDirectory + [System.IO.Path]::PathSeparator + $testClassesDirectory
$testRootPrefix = [System.IO.Path]::GetFullPath($testSourceDirectory) + [System.IO.Path]::DirectorySeparatorChar
foreach ($testEntryPoint in $testEntryPoints) {
    $relativePath = $testEntryPoint.FullName.Substring($testRootPrefix.Length)
    $testClass = $relativePath.Substring(0, $relativePath.Length - '.java'.Length) -replace '[\\/]', '.'
    & $java -cp $testClassPath $testClass
    if ($LASTEXITCODE -ne 0) {
        throw "Test class failed: $testClass"
    }
}

Write-Output "Build passed with $compilerVersion (target: Java 17, warnings treated as errors)."
Write-Output 'Startup smoke check passed.'
Write-Output "$($testEntryPoints.Count) test class(es) passed."
