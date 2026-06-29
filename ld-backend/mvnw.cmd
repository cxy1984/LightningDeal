@echo off
REM ----------------------------------------------------------------------------
REM Apache Maven Wrapper
REM ----------------------------------------------------------------------------

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_HOME=%DIRNAME%

set MAVEN_JAR=%APP_HOME%.mvn\wrapper\maven-wrapper.jar

"%JAVA_HOME%/bin/java.exe" %MAVEN_OPTS% -classpath "%MAVEN_JAR%" "-Dmaven.multiModuleProjectDirectory=%APP_HOME%" org.apache.maven.wrapper.MavenWrapperMain %*
