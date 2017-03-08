#!groovy
@Library('dynamo-workflow-libs') _

pipeline {
  agent any
  tools {
    maven 'Maven'
  }
  environment {
    //MAVEN_OPTS='-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true'
    MAVEN_OPTS='-Djavax.net.ssl.trustStore=$JENKINS_HOME/.keystore/cacerts -Djavax.net.ssl.keyStorePassword=changeit'
  }
  options {
    // Only keep the 10 most recent builds
    buildDiscarder(logRotator(numToKeepStr:'10'))
  }
  stages {

    stage ('Start') {
      steps {
        sendNotifications 'STARTED'
        sh 'env'
        sh 'ls /usr/lib/jvm/'
        //sh '''
        //  $JAVA_HOME/bin/keytool -import -v -trustcacerts \
        //  -alias server-alias -file server.cer \
        //  -keystore $JAVA_HOME/lib/security/cacerts -keypass changeit \
        //  -storepass changeit
        //  '''
      }
    }

    stage ('Build') {
      steps {
        script {
          sh 'cd usef-build && mvn clean deploy && cd ..'
        }
      }
    }

  }
  post {
    always {
      sendNotifications currentBuild.result
    }
  }
}
