@echo off

set H2_DATABASE_JAR=%JBOSS_HOME%\modules\system\layers\base\com\h2database\h2\main\h2-1.3.172.jar
set H2DB_PID=

rem Check if H2 database is running. If so, shutdown H2 database.
for /f "tokens=5" %%a in ('netstat -aon ^|find ":9092 " ^|find /i " TCP " ') do set H2DB_PID=%%a
if NOT "x%H2DB_PID%" == "x" (
   echo Running H2 database with PID %H2DB_PID%. Closing H2 database now.
   echo.
   javaw -classpath %H2_DATABASE_JAR% org.h2.tools.Server -tcpShutdown "tcp://localhost" -tcpShutdownForce
) else (
   echo No running H2 database detected. No need to stop it.
)
