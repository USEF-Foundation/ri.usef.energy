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
echo "Starting USEF Environment"
echo "===================================================================================================="
echo ""

DIRNAME="$USEF_HOME/usef-environment/bin"

H2_DATABASE_JAR=$JBOSS_HOME/modules/system/layers/base/com/h2database/h2/main/h2-1.4.190.jar
if [ "$1" = "" ]; then
  NODE_NAME=localhost
else
  NODE_NAME="$1"
fi
NODES_DIRECTORY="$DIRNAME/../nodes"
DOMAIN_DIRECTORY="$NODES_DIRECTORY/$NODE_NAME"

export RUN_CONF=$DIRNAME/usef-standalone.conf

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

HTTP_TCP_PORT=$(checkTcpPort 8080)
if [ -n "$HTTP_TCP_PORT" ]; then
  echo ""
  echo "There is already a process running on tcp port 8080. Use the task manager"
  echo "to close this process."
  echo ""
  echo "When the USEF environment is running, use the stop-usef-environment script."
  echo ""
  exit 1
fi

HTTPS_TCP_PORT=$(checkTcpPort 8443)
if [ -n "$HTTPS_TCP_PORT" ]; then
  echo ""
  echo "There is already a process running on tcp port 8443. Use the task manager"
  echo "to close this process."
  echo ""
  echo "When the USEF environment is running, use the stop-usef-environment script."
  echo ""
  exit 1
fi

HTTP_MNGT_TCP_PORT=$(checkTcpPort 9990)
if [ -n "$HTTP_MNGT_TCP_PORT" ]; then
  echo ""
  echo "There is already a process running on tcp port 9990. Use the task manager"
  echo "to close this process."
  echo ""
  echo "When the USEF environment is running, use the stop-usef-environment script."
  echo ""
  exit 1
fi

HTTPS_MNGT_TCP_PORT=$(checkTcpPort 9993)
if [ -n "$HTTPS_MNGT_TCP_PORT" ]; then
  echo ""
  echo "There is already a process running on tcp port 9993. Use the task manager"
  echo "to close this process."
  echo ""
  echo "When the USEF environment is running, use the stop-usef-environment script."
  echo ""
  exit 1
fi

H2DB=$(checkTcpPort 9092)
if [ -z "$H2DB" ]; then
   echo ""
   echo "The H2 Database is not running. Please, execute the script start-h2-database to"
   echo "run the H2 database."
   echo ""
   exit 1
fi

java -version >/dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "Java can not be started. It can not be found on the PATH. It looks like it is not installed."
  echo ""
  echo "If not installed, download and install Java JDK 8 from the website:"
  echo ""
  echo "    http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk"
  echo ""
  exit 1
fi

NSUPDATE_FILE=$NODES_DIRECTORY/usef_bind.nsupdate
if [ -e "$NSUPDATE_FILE" ]; then
    nsupdate -v $NSUPDATE_FILE 2>/dev/null
    if [ $? -ne 0 ]; then
      echo "Warning: DNS entries couldn't be updated!"
      echo "Please check whether BIND is running and nsupdate is on the PATH."
      echo ""
    else
      # remove the nsupdate and participants_dns_info.yaml files after success for security reasons
      rm $NSUPDATE_FILE
    fi
fi

if [ -z "$JBOSS_HOME" ]; then
  echo "The environment variable JBOSS_HOME can not be found."
  echo "JBoss Wildfly is not installed or the environment variable is not set."
  echo ""
  echo "If not installed, download and install JBoss Wildfly version 10.0.0 Final from the website:"
  echo ""
  echo "    http://wildfly.org/downloads"
  echo ""
  exit 1
fi

# check if the wildfly properties are availble in the configuration folder.
if [ ! -e "$DOMAIN_DIRECTORY/configuration/wildfly.properties" ]; then
  echo "The USEF environment has not been generated yet."
  echo "Please, run the prepare script before you start the USEF environment."
  echo ""
  exit 1
fi

echo ""
echo "DOMAIN_DIRECTORY = $DOMAIN_DIRECTORY"
echo "H2_DATABASE_JAR  = $H2_DATABASE_JAR"
echo ""

echo "Starting JBoss Wildfly"
$JBOSS_HOME/bin/standalone.sh -Djboss.server.config.dir=$DOMAIN_DIRECTORY/configuration -c=standalone-usef.xml --properties=file://$DOMAIN_DIRECTORY/configuration/wildfly.properties
