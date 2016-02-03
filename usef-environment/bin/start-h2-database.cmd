@echo off

echo.
echo ====================================================================================================
echo Starting H2 database
echo ====================================================================================================
echo.

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

set H2_DATABASE_JAR=%JBOSS_HOME%\modules\system\layers\base\com\h2database\h2\main\h2-1.4.190.jar
set H2DB_PID=

rem if JBoss Wildfly does not exist, show an error on the screen.
if NOT defined JBOSS_HOME (
  echo The environment variable JBOSS_HOME can not be found. JBoss Wildfly is not installed or the
  echo environment variable is not set.
  echo.
  echo If not installed, download and install JBoss Wildfly version 8.1.0 from the website:
  echo.
  echo    http://wildfly.org/downloads
  echo.
  exit /b 1
)

for /f "tokens=5" %%a in ('netstat -aon ^|find ":9092 .*LISTENING" ^|find /i " TCP " ') do set H2DB_PID=%%a
if defined H2DB_PID (
   echo.
   echo There is already a process running with PID %H2DB_PID% on tcp port 9092. This is
   echo probably the H2 database. Use the task manager to close this process or use the
   echo usef-stop-enviroment script to stop the USEF environment.
   echo.
   echo Or execute the following command to stop the H2 Database manually:
   echo    javaw -classpath %H2_DATABASE_JAR% org.h2.tools.Server -tcpShutdown "tcp://localhost" -tcpShutdownForce 
   echo.
   exit /b 1
)

call java -version > nul 2> nul || set JAVA_ERROR=1
if defined JAVA_ERROR (
  echo Java can not be started. It can not be found on the Windows PATH. It looks like it is not installed.
  echo.
  echo If not installed, download and install Java from the website:
  echo.
  echo    http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk
  echo.
  exit /b 1
)

echo.
echo H2_DATABASE_JAR  = %H2_DATABASE_JAR%
echo.

start java -server -Xmx1024m -Xms256m -classpath %H2_DATABASE_JAR% org.h2.tools.Server -tcp -tcpPort 9092 -tcpAllowOthers -web -webAllowOthers -webPort 8082

echo.
echo ====================================================================================================
echo The USEF database has been started. Use the start-usef-environment script to start the USEF
echo environment.
echo ====================================================================================================
