param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$projectRoot = Split-Path -Parent $PSScriptRoot
$plotScript = Join-Path $PSScriptRoot 'plot-results.ps1'
$sourceDirectory = Join-Path $projectRoot 'results/tables'
$culture = [System.Globalization.CultureInfo]::InvariantCulture
$temporaryRoot = Join-Path $projectRoot ('build/plot-check-' + [guid]::NewGuid().ToString('N'))
$inputDirectory = Join-Path $temporaryRoot 'input'
$firstOutput = Join-Path $temporaryRoot 'first output'
$zeroOutput = Join-Path $temporaryRoot 'zero output'
$invalidOutputs = @('bad-runs', 'bad-mean', 'bad-number', 'bad-coverage') |
    ForEach-Object { Join-Path $temporaryRoot $_ }
$plotFiles = @('time-vs-vertices.svg', 'time-vs-edges.svg')

function Check([bool]$Condition, [string]$Message) {
    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-Plot([string]$InputPath, [string]$OutputPath, [bool]$ShouldPass) {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $log = & powershell -NoProfile -ExecutionPolicy Bypass -File $plotScript `
            -ResultsDirectory $InputPath -PlotsDirectory $OutputPath 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($ShouldPass) {
        Check ($exitCode -eq 0) "Plot command failed: $log"
    } else {
        Check ($exitCode -ne 0) 'Malformed input unexpectedly passed plotting validation.'
        Check (-not (Test-Path -LiteralPath $OutputPath)) 'Invalid input must not create an output directory.'
    }
}

function Reset-Input {
    foreach ($file in @('all-runs.csv', 'all-summary.csv')) {
        Copy-Item -LiteralPath (Join-Path $sourceDirectory $file) -Destination (Join-Path $inputDirectory $file) -Force
    }
}

[void][System.IO.Directory]::CreateDirectory($inputDirectory)
try {
    Invoke-Plot $sourceDirectory $firstOutput $true
    $summary = @(Import-Csv -LiteralPath (Join-Path $sourceDirectory 'all-summary.csv'))
    $raw = @(Import-Csv -LiteralPath (Join-Path $sourceDirectory 'all-runs.csv'))
    foreach ($file in $plotFiles) {
        $path = Join-Path $firstOutput $file
        [xml]$document = Get-Content -LiteralPath $path -Raw
        $points = @($document.SelectNodes('//*[local-name()="g" and @class="measurement"]'))
        Check ($points.Count -eq 27) 'Every plot must contain exactly 27 mean markers.'
        foreach ($point in $points) {
            $scenario = $point.ParentNode.GetAttribute('data-scenario')
            $algorithm = $point.GetAttribute('data-algorithm')
            $vertices = $point.GetAttribute('data-vertices')
            $edges = $point.GetAttribute('data-edges')
            $matching = @($summary | Where-Object {
                $_.scenario -ceq $scenario -and $_.algorithm -ceq $algorithm -and
                $_.vertices -eq $vertices -and $_.edges -eq $edges
            })
            Check ($matching.Count -eq 1) 'Each SVG marker must identify one saved summary row.'
            $mean = [double]::Parse($point.GetAttribute('data-mean-ms'), $culture)
            Check ($mean -eq [double]::Parse($matching[0].average_elapsed_ms, $culture)) 'SVG mean differs from saved data.'
            $samples = @($raw | Where-Object {
                $_.scenario -ceq $scenario -and $_.algorithm -ceq $algorithm -and
                $_.vertices -eq $vertices -and $_.edges -eq $edges
            } | ForEach-Object { [double]::Parse($_.elapsed_ns, $culture) })
            $range = $samples | Measure-Object -Minimum -Maximum
            Check ([double]::Parse($point.GetAttribute('data-min-ms'), $culture) -eq $range.Minimum / 1000000.0) 'Minimum whisker differs from raw data.'
            Check ([double]::Parse($point.GetAttribute('data-max-ms'), $culture) -eq $range.Maximum / 1000000.0) 'Maximum whisker differs from raw data.'
        }
        $bytes = [System.IO.File]::ReadAllBytes($path)
        Check (-not ($bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191)) 'SVG must use UTF-8 without a BOM.'
        Check (-not ([System.IO.File]::ReadAllText($path).Contains("`r"))) 'SVG must use LF line endings.'
    }

    $before = @($plotFiles | ForEach-Object { (Get-FileHash -LiteralPath (Join-Path $firstOutput $_) -Algorithm SHA256).Hash })
    Invoke-Plot $sourceDirectory $firstOutput $true
    $after = @($plotFiles | ForEach-Object { (Get-FileHash -LiteralPath (Join-Path $firstOutput $_) -Algorithm SHA256).Hash })
    Check (($before -join ',') -eq ($after -join ',')) 'Plot regeneration from the same saved data must be deterministic.'

    Reset-Input
    $rows = @(Import-Csv -LiteralPath (Join-Path $inputDirectory 'all-runs.csv'))
    $rows[0].run = '5'
    $rows | Export-Csv -LiteralPath (Join-Path $inputDirectory 'all-runs.csv') -NoTypeInformation -Encoding UTF8
    Invoke-Plot $inputDirectory $invalidOutputs[0] $false

    Reset-Input
    $rows = @(Import-Csv -LiteralPath (Join-Path $inputDirectory 'all-summary.csv'))
    $rows[0].average_elapsed_ns = ([double]::Parse($rows[0].average_elapsed_ns, $culture) + 1).ToString('R', $culture)
    $rows | Export-Csv -LiteralPath (Join-Path $inputDirectory 'all-summary.csv') -NoTypeInformation -Encoding UTF8
    Invoke-Plot $inputDirectory $invalidOutputs[1] $false

    Reset-Input
    $rows = @(Import-Csv -LiteralPath (Join-Path $inputDirectory 'all-summary.csv'))
    $rows[0].average_elapsed_ms = 'NaN'
    $rows | Export-Csv -LiteralPath (Join-Path $inputDirectory 'all-summary.csv') -NoTypeInformation -Encoding UTF8
    Invoke-Plot $inputDirectory $invalidOutputs[2] $false

    Reset-Input
    $rows = @(Import-Csv -LiteralPath (Join-Path $inputDirectory 'all-summary.csv'))
    $rows | Select-Object -Skip 1 | Export-Csv -LiteralPath (Join-Path $inputDirectory 'all-summary.csv') -NoTypeInformation -Encoding UTF8
    Invoke-Plot $inputDirectory $invalidOutputs[3] $false

    Reset-Input
    $rows = @(Import-Csv -LiteralPath (Join-Path $inputDirectory 'all-runs.csv'))
    foreach ($row in $rows) { $row.elapsed_ns = '0' }
    $rows | Export-Csv -LiteralPath (Join-Path $inputDirectory 'all-runs.csv') -NoTypeInformation -Encoding UTF8
    $rows = @(Import-Csv -LiteralPath (Join-Path $inputDirectory 'all-summary.csv'))
    foreach ($row in $rows) { $row.average_elapsed_ns = '0'; $row.average_elapsed_ms = '0' }
    $rows | Export-Csv -LiteralPath (Join-Path $inputDirectory 'all-summary.csv') -NoTypeInformation -Encoding UTF8
    Invoke-Plot $inputDirectory $zeroOutput $true
    foreach ($file in $plotFiles) {
        [xml]$document = Get-Content -LiteralPath (Join-Path $zeroOutput $file) -Raw
        $points = @($document.SelectNodes('//*[local-name()="g" and @class="measurement"]'))
        Check ($points.Count -eq 27) 'A zero-duration capture must still plot every marker.'
        foreach ($point in $points) {
            Check ($point.GetAttribute('data-mean-ms') -eq '0') 'Zero-duration means must remain zero.'
        }
        Check (-not ($document.OuterXml -match '(NaN|Infinity)')) 'A zero-duration chart must have finite coordinates.'
    }
    Write-Output 'Plot checks passed: data and whiskers, valid XML, encoding, deterministic regeneration, invalid inputs, and zero durations.'
} finally {
    # Remove only known files in this run's own directory; never delete recursively.
    foreach ($directory in @($firstOutput, $zeroOutput) + $invalidOutputs) {
        if (Test-Path -LiteralPath $directory) {
            foreach ($file in $plotFiles) {
                $path = Join-Path $directory $file
                if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path -Force }
            }
            Remove-Item -LiteralPath $directory -Force
        }
    }
    foreach ($file in @('all-runs.csv', 'all-summary.csv')) {
        $path = Join-Path $inputDirectory $file
        if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path -Force }
    }
    Remove-Item -LiteralPath $inputDirectory -Force
    Remove-Item -LiteralPath $temporaryRoot -Force
}
