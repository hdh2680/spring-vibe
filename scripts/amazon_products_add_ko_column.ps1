param(
  [Parameter(Mandatory = $false)]
  [string]$InputPath = "src/main/resources/static/docs/amazonProduct/amazon_products.csv",

  [Parameter(Mandatory = $false)]
  [string]$OutputPath = "src/main/resources/static/docs/amazonProduct/amazon_products_ko.csv",

  [Parameter(Mandatory = $false)]
  [string]$ColumnName = "product_name_ko",

  # If set, the new column will be filled with the original title value (not translated).
  [Parameter(Mandatory = $false)]
  [switch]$FillWithTitle
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path $InputPath)) {
  throw "Input file not found: $InputPath"
}

$inFull = (Resolve-Path $InputPath).Path
$outFull = $OutputPath

$outDir = Split-Path -Parent $outFull
if ($outDir -and -not (Test-Path $outDir)) {
  New-Item -ItemType Directory -Path $outDir | Out-Null
}

# Use UTF-8 without BOM to match typical CSV expectations.
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$sr = New-Object System.IO.StreamReader($inFull, $utf8NoBom)
$sw = New-Object System.IO.StreamWriter($outFull, $false, $utf8NoBom)

try {
  $header = $sr.ReadLine()
  if ($null -eq $header) {
    throw "Empty CSV: $InputPath"
  }

  $sw.WriteLine("$header,$ColumnName")

  # This is a simple append because we add an extra trailing column.
  # It avoids heavy CSV parsing for large files. If FillWithTitle is enabled,
  # we extract the title column via minimal parsing per line.
  if (-not $FillWithTitle) {
    while (($line = $sr.ReadLine()) -ne $null) {
      if ($line.Length -eq 0) {
        $sw.WriteLine("")
        continue
      }
      $sw.WriteLine("$line,")
    }
  }
  else {
    # Minimal CSV split to extract the 2nd column (title). Handles quotes.
    function Get-SecondCsvField([string]$csvLine) {
      $i = 0
      $fieldIndex = 0
      $len = $csvLine.Length
      $inQuotes = $false
      $sb = New-Object System.Text.StringBuilder

      while ($i -lt $len) {
        $ch = $csvLine[$i]
        if ($inQuotes) {
          if ($ch -eq '"') {
            if (($i + 1) -lt $len -and $csvLine[$i + 1] -eq '"') {
              [void]$sb.Append('"')
              $i += 2
              continue
            }
            $inQuotes = $false
            $i++
            continue
          }
          [void]$sb.Append($ch)
          $i++
          continue
        }

        if ($ch -eq '"') {
          $inQuotes = $true
          $i++
          continue
        }

        if ($ch -eq ',') {
          $fieldIndex++
          if ($fieldIndex -eq 2) {
            # We have completed reading the 2nd field.
            return $sb.ToString()
          }
          $sb.Clear() | Out-Null
          $i++
          continue
        }

        [void]$sb.Append($ch)
        $i++
      }

      return $sb.ToString()
    }

    while (($line = $sr.ReadLine()) -ne $null) {
      if ($line.Length -eq 0) {
        $sw.WriteLine("")
        continue
      }
      $title = Get-SecondCsvField $line
      # CSV-escape the filled value.
      $filled = $title -replace '"','""'
      $sw.WriteLine("$line,""$filled""")
    }
  }
}
finally {
  $sr.Dispose()
  $sw.Dispose()
}

Write-Host "Wrote: $outFull"

