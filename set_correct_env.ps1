# 设置正确的Java和Maven环境变量
Write-Host "开始配置环境变量..."

# 设置Java 8环境变量
$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_77"
Write-Host "已设置JAVA_HOME = $env:JAVA_HOME"

# 设置Maven 3.9.11环境变量
$env:MAVEN_HOME = "C:\Program Files\Apache\maven\apache-maven-3.9.11"
Write-Host "已设置MAVEN_HOME = $env:MAVEN_HOME"

# 重新构建PATH变量，确保Java和Maven路径在前面
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"
Write-Host "已更新PATH环境变量"

# 验证Java版本
Write-Host "\n验证Java版本:" -ForegroundColor Green
java -version

# 验证Maven版本
Write-Host "\n验证Maven版本:" -ForegroundColor Green
mvn -version

Write-Host "\n环境变量配置完成！" -ForegroundColor Green