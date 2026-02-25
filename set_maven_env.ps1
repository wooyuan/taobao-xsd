# 设置Maven环境变量
$env:MAVEN_HOME = "C:\Program Files\Apache\maven\apache-maven-3.9.11"
$env:Path = "$env:MAVEN_HOME\bin;$env:Path"

# 显示设置的环境变量
Write-Host "MAVEN_HOME设置为: $env:MAVEN_HOME"
Write-Host "PATH中包含: $env:MAVEN_HOME\bin"

# 验证Maven版本
Write-Host "\n验证Maven版本:"
mvn -version