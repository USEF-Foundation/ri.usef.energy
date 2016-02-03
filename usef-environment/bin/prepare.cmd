@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

rem Get the program name before using shift as the command modify the variable ~nx0
if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=standalone.bat"
)

call java -version > nul 2> nul || set JAVA_ERROR=1
if defined JAVA_ERROR (
  echo Java can not be started. It can not be found on the Windows PATH. It looks like it is not installed.
  echo.
  echo If not installed, download and install Java from the website:
  echo.
  echo    http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk
  echo.
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

pushd ..\..\usef-environment-tool

FOR %%F IN (target\usef-environment-tool-*-jar-with-dependencies.jar) DO SET USEF_TOOL=%%F
if exist %USEF_TOOL% goto buildUSEF
echo.
echo ====================================================================================================
echo Building the USEF environment tooling... 
echo One moment
echo ====================================================================================================
echo.

call mvn clean install -DskipTests --quiet
if %errorlevel% neq 0 goto error

:buildUSEF
if "%1"=="-skipBuild" goto generateEnvironment
echo.
echo ====================================================================================================
echo Building USEF... 
echo ====================================================================================================
echo.

cd ..\usef-build

call mvn clean install -DskipTests %*
if %errorlevel% neq 0 goto error

cd ..\usef-environment-tool
echo.
echo.
echo.

:generateEnvironment

echo ====================================================================================================
echo Generating USEF environment - global configuration
echo ====================================================================================================
FOR %%F IN (target\usef-environment-tool-*-jar-with-dependencies.jar) DO SET USEF_TOOL=%%F
echo USEF_TOOL = %USEF_TOOL%

java -cp %USEF_TOOL% energy.usef.environment.tool.GenerateGlobalEnvironment
if %errorlevel% neq 0 goto end

echo.
echo ====================================================================================================
echo Generating USEF environment - domains
echo ====================================================================================================
java -cp %USEF_TOOL% energy.usef.environment.tool.GenerateDomains
if %errorlevel% neq 0 goto end

echo.
echo.
echo ====================================================================================================
echo Ready building USEF environment. Use the start-h2-database script to start the USEF database.
echo ====================================================================================================

goto end

:error
echo.
echo Something went wrong building the USEF environtment tooling.
echo.
popd
exit /b 1

:end
popd
