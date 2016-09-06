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
echo "Cleanup USEF Environment"
echo "===================================================================================================="
echo ""

DIRNAME=`pwd`

H2_DATABASE_JAR=$JBOSS_HOME/modules/system/layers/base/com/h2database/h2/main/h2-1.4.190.jar
NODES_DIRECTORY=$DIRNAME/../domain

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

mvn -version > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "Maven can not be started. It can not be found on the PATH. It looks like it is not installed."
  echo ""
  echo "If not installed, download and install Maven from the website:"
  echo ""
  echo "    http://maven.apache.org/download.cgi"
  echo ""
  exit 1
fi

if [ -z "$JBOSS_HOME" ]; then
  echo "The environment variable JBOSS_HOME can not be found. JBoss Wildfly is not installed or the"
  echo "environment variable is not set."
  echo ""
  echo "If not installed, download and install JBoss Wildfly version 10.0.0 from the website:"
  echo ""
  echo "    http://wildfly.org/downloads"
  echo ""
  exit 1
fi

echo ""
echo "NODES_DIRECTORY = $NODES_DIRECTORY"
echo "H2_DATABASE_JAR  = $H2_DATABASE_JAR"
echo ""

H2DB=$(checkTcpPort 9092)
if [ -n "$H2DB" ]; then
   echo "Running H2 database. Closing H2 database now."
   echo ""
   java -classpath $H2_DATABASE_JAR org.h2.tools.Server -tcpShutdown "tcp://localhost" -tcpShutdownForce
fi

cd ../../usef-environment-tool

if [ -z "$1" ] || ["$1" == "-skipBuild" ]; then
    echo ""
    echo "===================================================================================================="
    echo "Building the USEF environment tooling..."
    echo "One moment"
    echo "===================================================================================================="
    echo ""

    mvn clean install -DskipTests --quiet
    if [ $? -ne 0 ]; then
      exit 1
    fi
fi

java -cp target/usef-environment-tool-*-jar-with-dependencies.jar energy.usef.environment.tool.Cleanup
