# 设置正确的Java和Maven环境变量
$env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_77"
$env:MAVEN_HOME = "C:\Program Files\Apache\maven\apache-maven-3.9.11"
$env:Path = "$env:MAVEN_HOME\bin;$env:JAVA_HOME\bin;$env:Path"

# 执行Maven打包命令，忽略SSL验证
mvn clean package -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true