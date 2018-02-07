#!/bin/bash

#Stop script on errors
set -e
#Verbose
set -x

docker run -it -v ${HOME}/.m2/repository:/repository -v ${PWD}:/workspace -e USER=$USER -e USERID=$UID usef/maven mvn clean install

cd ..

