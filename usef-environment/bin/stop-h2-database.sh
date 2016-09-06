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
echo "Stopping USEF database"
echo "===================================================================================================="
echo ""

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

H2DB=$(checkTcpPort 9092)
if [ -n "$H2DB" ]; then
   echo "Running H2 database. Stopping H2 database now."
   echo ""
   java -classpath $H2_DATABASE_JAR org.h2.tools.Server -tcpShutdown "tcp://localhost" -tcpShutdownForce
else
   echo "No running H2 database detected. No need to stop it."
fi
