# Dockerfile for building the RI with Maven which needs Libsodium.
# 
# *Building*
#
# docker build -t usef/maven .
#
# *Running*
#
# docker run -it -v ${HOME}/.m2/repository:/repository -v ${PWD}:/workspace -e USER=$USER -e USERID=$UID usef/maven mvn install
# or use buildWithDocker.{bat|sh}
#
# Tip:
# Create an alias in your bashrc as follows: alias mvn-alliander='docker run -it -v ${HOME}/.m2/repository:/repository -v ${PWD}:/workspace -e USER=$USER -e USERID=$UID usef/maven mvn install $@'
FROM debian:buster as libsodium

ENV LIBSODIUM_VERSION 1.0.11

RUN apt-get update && apt-get install -y maven \
    openjdk-8-jre curl make gcc g++ && \
    mkdir -p /tmpbuild/libsodium && \
    cd /tmpbuild/libsodium && \
    curl -L https://download.libsodium.org/libsodium/releases/old/libsodium-$LIBSODIUM_VERSION.tar.gz -o libsodium-$LIBSODIUM_VERSION.tar.gz && \
    tar xfvz libsodium-$LIBSODIUM_VERSION.tar.gz && \
    cd /tmpbuild/libsodium/libsodium-$LIBSODIUM_VERSION/ && \
    ./configure && \
    make && make check && \
    make install 

FROM debian:buster
ENV LIBSODIUM_VERSION 1.0.11

RUN apt-get update && apt-get install -y maven openjdk-8-jdk

COPY --from=libsodium /tmpbuild/libsodium/libsodium-$LIBSODIUM_VERSION/src/libsodium /usr/local/
ADD settings.xml /usr/share/maven/conf/settings.xml

WORKDIR /workspace

