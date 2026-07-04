# 扫描心跳项目 Java 文件的注释缺口
# 输出：每行一个文件  file | classCount | hasClassComment | fieldTotal | fieldCommented | methodTotal | methodCommented

$ErrorActionPreference = 'Stop'
$root = 'd:\Desktop\自定义文件夹\HeartBeat-jsonschema'
$outFile = 'C:\TEMP\comment-scan.txt'
if (-not (Test-Path 'C:\TEMP')) { New-Item -ItemType Directory -Path 'C:\TEMP' | Out-Null }

# 收集所有 java 文件（排除 test、target、.gen）
$files = New-Object System.Collections.Generic.List[System.IO.FileInfo]
$enumerator = [System.IO.Directory]::EnumerateFiles($root, '*.java', [System.IO.SearchOption]::AllDirectories)
foreach ($fp in $enumerator) {
    if ($fp -notmatch '\\target\\' -and $fp -notmatch '\.gen\\' -and $fp -notmatch '\\src\\test\\') {
        $files.Add(([System.IO.FileInfo]::new($fp)))
    }
}

$results = New-Object System.Collections.Generic.List[string]
$results.Add('file|classCount|hasClassComment|fieldTotal|fieldCommented|methodTotal|methodCommented')

Write-Host "Found files count: $($files.Count)"
$first3 = $files | Select-Object -First 3
foreach ($f in $first3) { Write-Host "  sample: $($f.FullName) (length=$($f.Length))" }

foreach ($f in $files) {
    try {
        $rel = $f.FullName.Substring($root.Length)
        $txt = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
        if ([string]::IsNullOrWhiteSpace($txt)) { continue }

        # 类/接口/枚举/record 定义
        $classMatches = [regex]::Matches($txt, '(?m)^\s*(?:public\s+)?(?:abstract\s+|final\s+|static\s+)*(?:class|interface|enum|record)\s+[A-Z][A-Za-z0-9_]+')
        $classCount = $classMatches.Count
        if ($classCount -eq 0) { continue }

        # 类注释：前 12 行内出现 /// 或 /* 或 *
        $first12 = ($txt -split "`n" | Select-Object -First 12) -join "`n"
        $hasClassComment = $false
        if ($first12 -match '///|/\*|\*\s') { $hasClassComment = $true }

        # 字段（私有/保护/公共，实例字段，不含 static final 常量，不含方法签名）
        $fieldMatches = [regex]::Matches($txt, '(?m)^\s*(private|protected|public)\s+[A-Za-z<>?, _\[\]]+?\s+[a-z][A-Za-z0-9_]*\s*(=|;)')
        $fieldTotal = $fieldMatches.Count
        $fieldCommented = 0
        foreach ($m in $fieldMatches) {
            $pos = $m.Index
            $upTo = $txt.Substring(0, $pos)
            $prevLine = ($upTo -split "`n")[-1]
            if ($prevLine -match '^\s*(///|//|\*|/\\*)') { $fieldCommented++ }
        }

        # 方法签名
        $methodMatches = [regex]::Matches($txt, '(?m)^\s*(?:public|protected|private|static|abstract|final|native|synchronized|@[\w.]+\s*)+[A-Za-z<>?, _\[\]]+\s+[a-z][A-Za-z0-9_]*\s*\(')
        $methodTotal = $methodMatches.Count
        $methodCommented = 0
        foreach ($m in $methodMatches) {
            $pos = $m.Index
            $upTo = $txt.Substring(0, $pos)
            $prevLines = ($upTo -split "`n") | Select-Object -Last 3
            $has = $false
            foreach ($pl in $prevLines) {
                if ($pl -match '^\s*(///|//|/\*|\*)') { $has = $true; break }
            }
            if ($has) { $methodCommented++ }
        }

        $line = "$rel|$classCount|$hasClassComment|$fieldTotal|$fieldCommented|$methodTotal|$methodCommented"
        $results.Add($line)
    } catch {
        $results.Add("ERR|$($f.FullName)|$($_.Exception.Message)")
    }
}

[System.IO.File]::WriteAllText($outFile, ($results -join "`n"), [System.Text.Encoding]::UTF8)
Write-Host "Wrote $($results.Count) lines to $outFile"
