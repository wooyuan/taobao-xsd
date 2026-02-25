@echo off
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_77
set MAVEN_HOME=C:\Program Files\Apache\maven\apache-maven-3.9.11
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%
mvn clean package -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true