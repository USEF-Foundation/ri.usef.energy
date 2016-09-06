#
# Copyright 2015-2016 USEF Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#!/bin/sh

echo ""
echo "===================================================================================================="
echo "Starting H2 database"
echo "===================================================================================================="
echo ""

DIRNAME=`pwd`

H2_DATABASE_JAR=$JBOSS_HOME/modules/system/layers/base/com/h2database/h2/main/h2-1.4.190.jar

checkTcpPort(){
  local tcpPort=":$1"
  case "`uname`" in
    CYGWIN*)
        echo "Cygwin is not supported for now. Try to use another Linux variant, like MacOS, FreeBSD, Ubuntu or Solaris."
        exit 1
        ;;
    Darwin*)
        echo `netstat -anp tcp | awk -v tcpPort="$tcpPort" '$6 == "LISTEN" && $4 ~ "\$tcpPort"'`
        ;;
    FreeBSD*)
        echo `netstat -lnt | awk -v tcpPort="$tcpPort" '$6 == "LISTEN" && $4 ~ $tcpPort'`
        ;;
    Linux*)
        echo `netstat -lnt | awk -v tcpPort="$tcpPort" '$6 == "LISTEN" && $4 ~ tcpPort "$"'`
        ;;
    SunOS*)
        echo `netstat -lnt | awk -v tcpPort="$tcpPort" '$6 == "LISTEN" && $4 ~ $tcpPort'`
        ;;
  esac 
}

if [ -z "$JBOSS_HOME" ]; then
  echo "The environment variable JBOSS_HOME can not be found. JBoss Wildfly is not installed or the"
  echo "environment variable is not set."
  echo ""
  echo "If not installed, download and install JBoss Wildfly version 8.1.0 from the website:"
  echo ""
  echo "    http://wildfly.org/downloads"
  echo ""
  exit 1
fi

H2DB=$(checkTcpPort 9092)
if [ -n "$H2DB" ]; then
   echo ""
   echo "There is already a process running on tcp port 9092. This is"
   echo "probably the H2 database. Use the task manager to close this process or use the"
   echo "usef-stop-enviroment script to stop the USEF environment."
   echo ""
   echo "Or execute the following command to stop the H2 Database manually:"
   echo "   javaw -classpath $H2_DATABASE_JAR org.h2.tools.Server -tcpShutdownForce ""tcp://localhost"""
   echo ""
   exit 1
fi

java -version >/dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "Java can not be started. It can not be found on the PATH. It looks like it is not installed."
  echo ""
  echo "If not installed, download and install Java from the website:"
  echo ""
  echo "    http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk"
  echo ""
  exit 1
fi

echo ""
echo "H2_DATABASE_JAR  = $H2_DATABASE_JAR"
echo ""

java -classpath $H2_DATABASE_JAR org.h2.tools.Server -tcp -tcpPort 9092 -tcpAllowOthers -web -webAllowOthers -webPort 8082 &
echo ""

echo ""
echo ""
echo "===================================================================================================="
echo "The USEF database has been started. Use the start-usef-environment script to start the USEF"
echo "environment."
echo "===================================================================================================="
