@echo off

echo.
echo ====================================================================================================
echo Starting USEF Environment
echo ====================================================================================================
echo.

@if not "%ECHO%" == ""  echo %ECHO%
set DIRNAME=%USEF_HOME%\usef-environment\bin\


if "%1" == "" (
  set NODE_NAME=localhost
) else (
  set NODE_NAME=%1
)

set H2_DATABASE_JAR=%JBOSS_HOME%\modules\system\layers\base\com\h2database\h2\main\h2-1.4.190.jar
set NODES_DIRECTORY=%DIRNAME%..\nodes
set DOMAIN_DIRECTORY=%NODES_DIRECTORY%\%NODE_NAME%
set CONFIG_DIRECTORY=%DOMAIN_DIRECTORY%\configuration
set STANDALONE_CONF=%DIRNAME%\usef-standalone.conf.bat


for /f "tokens=5" %%a in ('netstat -aon ^|find ":8080 .*LISTENING" ^|find /i " TCP " ') do set HTTP_PID=%%a
if defined HTTP_PID (
  echo.
  echo There is already a process running with PID %HTTP_PID% on tcp port 8080. Use the task manager 
  echo to close this process.
  echo.
  echo When the USEF environment is running, use the stop-usef-environment script.
  echo.
  exit /b 1
)

for /f "tokens=5" %%a in ('netstat -aon ^|find ":8443 .*LISTENING" ^|find /i " TCP " ') do set HTTP_PID=%%a
if defined HTTP_PID (
  echo.
  echo There is already a process running with PID %HTTP_PID% on tcp port 8443. Use the task manager 
  echo to close this process.
  echo.
  echo When the USEF environment is running, use the stop-usef-environment script.
  echo.
  exit /b 1
)

for /f "tokens=5" %%a in ('netstat -aon ^|find ":9990 .*LISTENING" ^|find /i " TCP " ') do set HTTP_PID=%%a
if defined HTTP_PID (
  echo.
  echo There is already a process running with PID %HTTP_PID% on tcp port 9990. Use the task manager 
  echo to close this process.
  echo.
  echo When the USEF environment is running, use the stop-usef-environment script.
  echo.
  exit /b 1
)

for /f "tokens=5" %%a in ('netstat -aon ^|find ":9993 .*LISTENING" ^|find /i " TCP " ') do set HTTP_PID=%%a
if defined HTTP_PID (
  echo.
  echo There is already a process running with PID %HTTP_PID% on tcp port 9993. Use the task manager 
  echo to close this process.
  echo.
  echo When the USEF environment is running, use the stop-usef-environment script.
  echo.
  exit /b 1
)

for /f "tokens=5" %%a in ('netstat -aon ^|find ":9092" ^|find /i " TCP " ') do set H2DB_PID=%%a
if NOT defined H2DB_PID (
   echo.
   echo The H2 Database is not running. Please, execute the script start-h2-database to
   echo run the H2 database.
   echo.
   exit /b 1
)

call java -version > nul 2> nul || set JAVA_ERROR=1
if defined JAVA_ERROR (
  echo Java can not be started. It can not be found on the Windows PATH. It looks like it is not installed.
  echo.
  echo If not installed, download and install Java JDK 7 from the website:
  echo.
  echo    http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk
  echo.
  exit /b 1
)

SET NSUPDATE_FILE=%NODES_DIRECTORY%\usef_bind.nsupdate
if exist %NSUPDATE_FILE% (
    call nsupdate -v  %NSUPDATE_FILE% 2> nul || set NSUPDATE_ERROR=1
    if defined NSUPDATE_ERROR (
      echo Warning: DNS entries couldn't be updated!.
      echo Please check whether BIND is running and nsupdate is on the PATH.
      echo.
    ) else (
      rem remove the nsupdate and participants_dns_info.yaml files after success for security reasons
      del %NSUPDATE_FILE%
    )
)

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

rem check if the wildfly properties are availble in the configuration folder.
if NOT exist "%DOMAIN_DIRECTORY%\configuration\wildfly.properties" (
  echo The USEF environment has not been generated yet, or the node %NODE_NAME% is unknown.
  echo Please, run the prepare script before you start the USEF environment.
  echo.
  exit /b 1
)

echo.
echo DOMAIN_DIRECTORY = %DOMAIN_DIRECTORY%
echo CONFIG_DIRECTORY = %CONFIG_DIRECTORY%
echo H2_DATABASE_JAR  = %H2_DATABASE_JAR%
echo.

echo Starting JBoss Wildfly
call %JBOSS_HOME%\bin\standalone.bat -Djboss.server.config.dir=%CONFIG_DIRECTORY% -c=standalone-usef.xml --properties=file:\\%CONFIG_DIRECTORY%\wildfly.properties
