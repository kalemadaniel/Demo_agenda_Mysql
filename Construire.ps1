$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$javac = Get-Command javac -ErrorAction SilentlyContinue
$jdk = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) { $env:JAVA_HOME } elseif ($javac) { Split-Path (Split-Path $javac.Source) } else {
    Get-ChildItem 'C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path (Join-Path $_.FullName 'bin\javac.exe') } | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (!$jdk) { throw 'Installez un JDK Java 8 ou plus recent, puis definissez JAVA_HOME.' }
New-Item -ItemType Directory -Force 'build-modern\classes','dist\lib' | Out-Null
$sources = Get-ChildItem 'src' -Recurse -Filter '*.java' | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }
$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines((Join-Path $PSScriptRoot 'build-modern\sources.txt'), $sources, $utf8)
& "$jdk\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -d 'build-modern\classes' '@build-modern/sources.txt'
if ($LASTEXITCODE -ne 0) { throw 'La compilation a echoue.' }
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'build-modern\MANIFEST.MF'), "Manifest-Version: 1.0`r`nMain-Class: kalema.collection.Agenda`r`nClass-Path: lib/mysql-connector-java-5.1.23-bin.jar`r`n`r`n", $utf8)
Copy-Item 'lib\mysql-connector-java-5.1.23-bin.jar' 'dist\lib' -Force
& "$jdk\bin\jar.exe" cfm 'dist\agenda.jar' 'build-modern\MANIFEST.MF' -C 'build-modern\classes' .
if ($LASTEXITCODE -ne 0) { throw 'La creation du JAR a echoue.' }
Write-Host 'Compilation terminee : dist\agenda.jar'
