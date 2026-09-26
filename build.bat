@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "C:\Users\Abdurahim Nasser\OneDrive\banking system"
java -Dmaven.multiModuleProjectDirectory="C:\Users\Abdurahim Nasser\OneDrive\banking system" -cp .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain clean package -DskipTests