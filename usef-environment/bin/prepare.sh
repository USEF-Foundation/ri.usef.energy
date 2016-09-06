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

cd ../../usef-environment-tool
environment_tool_jar="`find target -name 'usef-environment-tool-*-jar-with-dependencies.jar'`"
if [ -z "${environment_tool_jar}" ]; then
    echo ""
    echo "===================================================================================================="
    echo "Building the USEF environment tooling..."
    echo "One moment"
    echo "===================================================================================================="
    echo ""

    mvn clean install -DskipTests --quiet
    environment_tool_jar=`find target -name 'usef-environment-tool-*-jar-with-dependencies.jar'`
else
    echo "Found environment-tool ${entvironment_tool_jar}"
fi

if [ "$1" != "-skipBuild" ]; then
    echo ""
    echo "===================================================================================================="
    echo "Building USEF..."
    echo "===================================================================================================="
    echo ""

    cd ../usef-build

    mvn clean install -DskipTests $*
    if [ $? -ne 0 ]; then
      exit 1
    fi

    cd ../usef-environment-tool
    echo ""
    echo ""
    echo ""
fi

echo "===================================================================================================="
echo "Generating USEF environment - global configuration"
echo "===================================================================================================="
java -cp ${environment_tool_jar} energy.usef.environment.tool.GenerateGlobalEnvironment
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "===================================================================================================="
echo "Generating USEF environment - domains"
echo "===================================================================================================="
java -cp ${environment_tool_jar} energy.usef.environment.tool.GenerateDomains
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo ""
echo "===================================================================================================="
echo "Ready building USEF environment. Use the start-h2-database script to start the USEF database."
echo "===================================================================================================="
