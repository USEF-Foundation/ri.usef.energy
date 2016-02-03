#!/bin/bash

#Stop script on errors
set -e
#Verbose
set -x


mvn clean install

cd ..

