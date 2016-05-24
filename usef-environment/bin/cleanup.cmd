@echo off
rem
rem Copyright 2015 USEF Foundation
rem
rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem
rem http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.
rem

echo.
echo ====================================================================================================
echo Cleanup USEF Environment
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
set NODES_DIRECTORY=%DIRNAME%..\nodes


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

rem if Maven does not exist, show an error on the screen.
call mvn -version > nul 2> nul || set MVN_ERROR=1
if defined MVN_ERROR (
  echo Maven can not be started. It can not be found on the Windows PATH. It looks like it is not installed.
  echo.
  echo If not installed, download and install Maven from the website:
  echo.
  echo    http://maven.apache.org/download.cgi
  echo.
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

echo.
echo NODES_DIRECTORY = %NODES_DIRECTORY%
echo H2_DATABASE_JAR  = %H2_DATABASE_JAR%

echo.

rem Check if H2 database is running. If so, shutdown H2 database.
for /f "tokens=5" %%a in ('netstat -aon ^|find ":9092 " ^|find /i " TCP " ') do set H2DB_PID=%%a
if NOT "x%H2DB_PID%" == "x" (
   echo Running H2 database with PID %H2DB_PID%. Closing H2 database now.
   echo.
   javaw -classpath %H2_DATABASE_JAR% org.h2.tools.Server -tcpShutdown "tcp://localhost" -tcpShutdownForce
)

pushd ..\..\usef-environment-tool

if "%1"=="-skipBuild" goto go-on
echo.
echo ====================================================================================================
echo Building the USEF environment tooling... 
echo One moment
echo ====================================================================================================
echo.



call mvn clean install -DskipTests --quiet
if %errorlevel% neq 0 goto error

:go-on


FOR %%F IN (target\usef-environment-tool-*-jar-with-dependencies.jar) DO SET USEF_TOOL=%%F
echo USEF_TOOL = %USEF_TOOL%
java -cp %USEF_TOOL% energy.usef.environment.tool.Cleanup

popd