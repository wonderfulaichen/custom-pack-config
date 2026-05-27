@echo off
echo Checking JAR contents...
powershell -Command "& {$jar = [System.IO.Compression.ZipFile]::OpenRead('build\libs\datapack_config_mod-1.0.0-1.20.1.jar'); $jar.Entries | Select-Object FullName | Format-List}"
pause
