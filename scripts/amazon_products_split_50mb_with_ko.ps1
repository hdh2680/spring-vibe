param(
  [Parameter(Mandatory = $false)]
  [string]$InputPath = "src/main/resources/static/docs/amazonProduct/amazon_products.csv",

  [Parameter(Mandatory = $false)]
  [string]$OutputDir = "src/main/resources/static/docs/amazonProduct/split",

  [Parameter(Mandatory = $false)]
  [int]$ChunkSizeMB = 50,

  [Parameter(Mandatory = $false)]
  [string]$NewColumnName = "product_name_ko",

  [Parameter(Mandatory = $false)]
  [string]$OutputPrefix = "amazon_products_ko_part"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path $InputPath)) {
  throw "Input file not found: $InputPath"
}

if ($ChunkSizeMB -lt 1) {
  throw "ChunkSizeMB must be >= 1"
}

if (-not (Test-Path $OutputDir)) {
  New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$inFull = (Resolve-Path $InputPath).Path
$chunkBytes = [int64]$ChunkSizeMB * 1024 * 1024

# UTF-8 without BOM for broad CSV compatibility.
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function New-ChunkWriter([int]$index, [string]$headerLine) {
  $fileName = "{0}{1:D3}.csv" -f $OutputPrefix, $index
  $outPath = Join-Path $OutputDir $fileName
  $fs = New-Object System.IO.FileStream($outPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write, [System.IO.FileShare]::Read)
  $sw = New-Object System.IO.StreamWriter($fs, $utf8NoBom)
  $sw.NewLine = "`n"
  $sw.WriteLine($headerLine)
  # StreamWriter buffers; flush so Position reflects reality.
  $sw.Flush()
  return @{ Path = $outPath; FileStream = $fs; Writer = $sw }
}

$sr = New-Object System.IO.StreamReader($inFull, $utf8NoBom)
$chunk = $null
$chunkIndex = 0
$linesWritten = 0

try {
  $header = $sr.ReadLine()
  if ($null -eq $header) {
    throw "Empty CSV: $InputPath"
  }

  $outHeader = "$header,$NewColumnName"

  while (($line = $sr.ReadLine()) -ne $null) {
    if ($null -eq $chunk) {
      $chunkIndex++
      $chunk = New-ChunkWriter $chunkIndex $outHeader
    }

    # Append a new trailing column value (blank). This is safe because we only add a new last column.
    $chunk.Writer.WriteLine("$line,")
    $linesWritten++

    # Only check size periodically for speed.
    if (($linesWritten % 2000) -eq 0) {
      $chunk.Writer.Flush()
      if ($chunk.FileStream.Position -ge $chunkBytes) {
        $chunk.Writer.Dispose()
        $chunk.FileStream.Dispose()
        $chunk = $null
      }
    }
  }

  if ($null -ne $chunk) {
    $chunk.Writer.Flush()
  }
}
finally {
  if ($null -ne $chunk) {
    $chunk.Writer.Dispose()
    $chunk.FileStream.Dispose()
  }
  $sr.Dispose()
}

Write-Host ("Split done. chunks={0}, outputDir={1}" -f $chunkIndex, (Resolve-Path $OutputDir).Path)

