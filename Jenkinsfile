#!groovy
@Library('dynamo-workflow-libs') _

pipeline {
  agent any
  tools {
    maven 'Maven'
  }
  environment {
    MAVEN_OPTS='-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true'
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
        sh '''
          $JAVA_HOME/bin/keytool -import -v -trustcacerts \
          -alias server-alias -file server.cer \
          -keystore $JAVA_HOME/lib/security/cacerts -keypass changeit \
          -storepass changeit
          '''
      }
    }

    stage ('Build') {
      steps {
        script {
          sh 'export MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"'
          sh 'cd usef-build && mvn clean deploy -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true && cd ..'
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
