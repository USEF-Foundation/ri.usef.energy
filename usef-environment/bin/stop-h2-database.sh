#!/bin/sh

echo ""
echo "===================================================================================================="
echo "Stopping USEF database"
echo "===================================================================================================="
echo ""

H2_DATABASE_JAR=$JBOSS_HOME/modules/system/layers/base/com/h2database/h2/main/h2-1.3.172.jar

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
