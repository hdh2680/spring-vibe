param(
  [string]$Root = "."
)

$ErrorActionPreference = "Stop"

# Returns $true if file starts with UTF-8 BOM (EF BB BF)
function Has-Utf8Bom([string]$Path) {
  $fs = [System.IO.File]::OpenRead($Path)
  try {
    if ($fs.Length -lt 3) { return $false }
    $b0 = $fs.ReadByte(); $b1 = $fs.ReadByte(); $b2 = $fs.ReadByte()
    return ($b0 -eq 0xEF -and $b1 -eq 0xBB -and $b2 -eq 0xBF)
  } finally {
    $fs.Dispose()
  }
}

$rootFull = (Resolve-Path $Root).Path
$files = Get-ChildItem -Path $rootFull -Recurse -File |
  Where-Object {
    $_.FullName -notmatch "\\\\target\\\\" -and
    $_.FullName -notmatch "\\\\\\.git\\\\" -and
    $_.Extension -in @(".java", ".kt", ".xml", ".yml", ".yaml", ".properties", ".md", ".html", ".css", ".js")
  }

$bad = @()
foreach ($f in $files) {
  if (Has-Utf8Bom $f.FullName) {
    $bad += $f.FullName
  }
}

if ($bad.Count -gt 0) {
  Write-Host "UTF-8 BOM detected in the following files:" -ForegroundColor Red
  $bad | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
  exit 1
}

Write-Host "OK: no UTF-8 BOM found." -ForegroundColor Green
exit 0