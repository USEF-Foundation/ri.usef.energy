#!groovy
@Library('dynamo-workflow-libs') _

pipeline {
  agent none
  environment {
    MAVEN_OPTS='-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true'
  }
  options {
    // Only keep the 10 most recent builds
    buildDiscarder(logRotator(numToKeepStr:'10'))
  }
  stages {

    stage ('Start') {
      agent any
      steps {
        sh 'env'
      }
    }

    stage ('Build') {
      agent any
      tools {
        maven 'Maven'
      }
      steps {
        script {
          sh 'cd usef-build && mvn clean deploy && cd ..'
        }
      }
    }

    stage('SonarQube analysis') {
      agent none
      tools {
        maven 'Maven'
      }
      steps {
        script {
          withSonarQubeEnv('My SonarQube Server') {
            sh 'cd usef-build && mvn clean verify sonar:sonar && cd ..'
          }
          //TODO: add waitForQualityGate to hold the pipeline until sonarqube finished scanning
        }
      }
    }

  }
  post {
    failure {
      sendNotifications 'FAILURE'
    }
    unstable {
      sendNotifications 'UNSTABLE'
    }
  }
}
