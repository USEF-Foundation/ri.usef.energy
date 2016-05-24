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

set H2_DATABASE_JAR=%JBOSS_HOME%\modules\system\layers\base\com\h2database\h2\main\h2-1.4.190.jar
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
