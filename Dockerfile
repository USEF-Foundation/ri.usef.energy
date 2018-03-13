FROM maven:3.5.2-jdk-8

RUN apt-get update && apt-get install -y \
      gcc \
      make \
      net-tools

# Build libsodium from source
WORKDIR /tmp/libsodium

ARG SODIUM_VERSION=1.0.10
ARG SODIUM_BASE_URL=https://download.libsodium.org/libsodium/releases/old
ARG SODIUM_SHA=71b786a96dd03693672b0ca3eb77f4fb08430df307051c0d45df5353d22bc4be

RUN  curl -SLo libsodium.tar.gz \
      ${SODIUM_BASE_URL}/libsodium-${SODIUM_VERSION}.tar.gz \
  && echo "${SODIUM_SHA} *libsodium.tar.gz" | sha256sum -c - \
  && tar -xzf libsodium.tar.gz --strip-components 1 \
  && ./configure && make && make check && make install \
  && rm -rf /tmp/libsodium

ENV LD_LIBRARY_PATH "/usr/local/lib:${LD_LIBRARY_PATH}"

# Initialise WildFly server implementation used by USEF environment
ENV JBOSS_HOME /usr/share/wildfly

WORKDIR ${JBOSS_HOME}

ARG WILDFLY_VERSION=10.0.0.Final
ARG WILDFLY_BASE_URL=http://download.jboss.org/wildfly/${WILDFLY_VERSION}
ARG WILDFLY_SHA=e00c4e4852add7ac09693e7600c91be40fa5f2791d0b232e768c00b2cb20a84b

RUN  curl -SLo wildfly.tar.gz \
      ${WILDFLY_BASE_URL}/wildfly-${WILDFLY_VERSION}.tar.gz \
  && echo "${WILDFLY_SHA} *wildfly.tar.gz" | sha256sum -c - \
  && tar -xzf wildfly.tar.gz --strip-components 1 \
  && rm wildfly.tar.gz

WORKDIR ${JBOSS_HOME}/modules/system/layers/base/com/h2database/h2/main

# An alternative version of the H2 SQL database library is specified in the USEF
# manual
ARG H2_VERSION_DATE=2015-10-11
ARG H2_VERSION=1.4.190
ARG H2_BASE_URL=http://www.h2database.com
ARG H2_SHA=7881f308debe6d587219db3610b699af21d5e4b50ccb6fccac563382772a09c8

RUN curl -SLo h2.zip ${H2_BASE_URL}/h2-${H2_VERSION_DATE}.zip \
  && echo "${H2_SHA} *h2.zip" | sha256sum -c - \
  && unzip -q h2.zip h2/bin/h2-${H2_VERSION}.jar \
  && rm h2.zip \
  # Replace bundled H2 version with required version
  && mv h2/bin/h2-${H2_VERSION}.jar h2-${H2_VERSION}.jar \
  && rm -rf h2 && rm h2-1.3.173.jar \
  # Redirect the resource source path to the new .jar file
  && sed -i 's/1\.3\.173/'${H2_VERSION}'/g' module.xml

RUN apt-get purge -y \
      gcc \
      make

ENV USEF_HOME /usr/src/app

COPY . ${USEF_HOME}

WORKDIR ${USEF_HOME}/usef-environment/bin

# Build reference implementation
RUN ./prepare.sh

# H2 database is initialised asynchronously so wait an arbitrary length before
# running the environment
ENTRYPOINT ./start-h2-database.sh && sleep 1 && ./start-usef-environment.sh

# 8082: H2 SQL database console
# 8443: Port to access USEF endpoints
# 9990: WildFly management console
EXPOSE 8082 8443 9990
