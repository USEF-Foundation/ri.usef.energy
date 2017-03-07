#!groovy
@Library('dynamo-workflow-libs') _

pipeline {
  agent any
  tools {
    maven 'Maven'
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
