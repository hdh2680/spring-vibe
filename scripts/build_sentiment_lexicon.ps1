$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$docs = Join-Path $root "src/main/resources/static/docs"

$srcScored = Join-Path $docs "SentiWord_Dict.txt"
$srcPos = Join-Path $docs "pos_pol_word.txt"
$srcNeg = Join-Path $docs "neg_pol_word.txt"
$srcNeu = Join-Path $docs "obj_unknown_pol_word.txt"
$out = Join-Path $docs "sentiment_lexicon.tsv"

foreach ($p in @($srcScored, $srcPos, $srcNeg, $srcNeu)) {
  if (!(Test-Path $p)) {
    throw "Missing source file: $p"
  }
}

function Get-TermLinesAfterPolarityHeader([string]$path) {
  $POL = "$([char]0xADF9)$([char]0xC131)"   # "극성"
  $SRC = "$([char]0xCD9C)$([char]0xCC98)"   # "출처"
  $WORD = "$([char]0xB2E8)$([char]0xC5B4)"  # "단어"
  $COUNT = "$([char]0xC218)"                # "수"

  $lines = Get-Content -Encoding utf8 $path
  $start = -1
  for ($i = 0; $i -lt $lines.Count; $i++) {
    $t = $lines[$i].Trim()
    if ($t -like "$POL*") {
      $start = $i + 1
      break
    }
  }
  if ($start -lt 0) {
    # Fallback: treat all lines as candidates but skip obvious headers.
    $start = 0
  }
  for ($i = $start; $i -lt $lines.Count; $i++) {
    $t = $lines[$i].Trim()
    if ($t.Length -eq 0) { continue }
    if ($t -like "$SRC*") { continue }
    if ($t -like "$WORD*$COUNT*:" ) { continue }
    if ($t -like "$POL*:" ) { continue }
    if ($t -match '^\d+\)' ) { continue }
    if ($t -match '^->' ) { continue }
    Write-Output $t
  }
}

$scoreByTerm = [System.Collections.Generic.Dictionary[string,int]]::new([System.StringComparer]::Ordinal)
$sourceByTerm = [System.Collections.Generic.Dictionary[string,string]]::new([System.StringComparer]::Ordinal)

function Add-Term([string]$term, [int]$score, [string]$source) {
  if ([string]::IsNullOrWhiteSpace($term)) { return }
  $term = $term.Trim()
  if ($term.Length -eq 0) { return }
  if ($term.Contains("`t")) { $term = $term -replace "`t", " " }

  if ($source -eq "knu_scored") {
    $scoreByTerm[$term] = $score
    $sourceByTerm[$term] = $source
    return
  }

  if ($scoreByTerm.ContainsKey($term)) {
    $prevScore = $scoreByTerm[$term]
    $prevSource = $sourceByTerm[$term]
    if ($prevSource -eq "knu_scored") {
      return
    }
    if ($prevScore -eq $score) {
      return
    }
    if ($prevScore -eq 0 -and $score -ne 0) {
      $scoreByTerm[$term] = $score
      $sourceByTerm[$term] = $source
      return
    }
    if ($score -eq 0 -and $prevScore -ne 0) {
      return
    }
    # Conflict (e.g. pos vs neg): neutralize to 0 for safety.
    $scoreByTerm[$term] = 0
    $sourceByTerm[$term] = "conflict"
    return
  }

  $scoreByTerm.Add($term, $score)
  $sourceByTerm.Add($term, $source)
}

# 1) Scored lexicon (tab-separated: term \t score)
Get-Content -Encoding utf8 $srcScored | ForEach-Object {
  $line = $_
  if ([string]::IsNullOrWhiteSpace($line)) { return }
  $parts = $line -split "`t"
  if ($parts.Count -lt 2) { return }
  $term = $parts[0].Trim()
  $scoreStr = $parts[1].Trim()
  $score = 0
  if (![int]::TryParse($scoreStr, [ref]$score)) { return }
  Add-Term -term $term -score $score -source "knu_scored"
}

# 2) Polarity word lists (+1/-1/0)
Get-TermLinesAfterPolarityHeader $srcPos | ForEach-Object { Add-Term -term $_ -score 1 -source "pos_list" }
Get-TermLinesAfterPolarityHeader $srcNeg | ForEach-Object { Add-Term -term $_ -score -1 -source "neg_list" }
Get-TermLinesAfterPolarityHeader $srcNeu | ForEach-Object { Add-Term -term $_ -score 0 -source "neutral_list" }

$terms = $scoreByTerm.Keys | Sort-Object
$outLines = New-Object System.Collections.Generic.List[string] ($terms.Count + 5)
$outLines.Add("# term<TAB>score<TAB>source")
$ts = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
$outLines.Add("# generated_at=$ts")
$outLines.Add("# sources=SentiWord_Dict.txt,pos_pol_word.txt,neg_pol_word.txt,obj_unknown_pol_word.txt")

foreach ($t in $terms) {
  $s = $scoreByTerm[$t]
  $src = $sourceByTerm[$t]
  $outLines.Add("$t`t$s`t$src")
}

$outLines | Set-Content -Encoding utf8 $out
Write-Host "Wrote: $out (terms=$($terms.Count))"
