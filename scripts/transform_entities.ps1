# 批量改造 Entity：删除 mybatisflex 注解
$entityDir = 'd:\Desktop\自定义文件夹\HeartBeat-jsonschema\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity'
$files = Get-ChildItem -Path $entityDir -Recurse -Filter '*.java' | Where-Object { $_.FullName -notmatch '\\example\\' -and $_.Name -notmatch 'DO\.java$' }

foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw -Encoding UTF8
    $original = $content

    # 删除 mybatisflex import
    $content = $content -replace '(?ms)import\s+com\.mybatisflex\.annotation\.[A-Za-z]+;\r?\n', ''
    # 删除类上的 @Table
    $content = $content -replace '(?m)^\s*@Table\([^)]*\)\r?\n', ''
    # 删除字段上的 @Id / @Column
    $content = $content -replace '(?m)^\s*@Id\([^)]*\)\r?\n', ''
    $content = $content -replace '(?m)^\s*@Column\([^)]*\)\r?\n', ''

    if ($content -ne $original) {
        $utf8NoBom = New-Object System.Text.UTF8Encoding $False
        [System.IO.File]::WriteAllText($f.FullName, $content, $utf8NoBom)
        Write-Host "[Entity] $($f.Name) -> 处理完成"
    }
}
Write-Host '=== Entity 阶段完成 ==='