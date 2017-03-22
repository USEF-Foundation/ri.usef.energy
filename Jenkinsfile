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
        withSonarQubeEnv('My SonarQube Server') {
          sh 'cd usef-build && mvn clean verify sonar:sonar deploy -Dsonar.host.url=$SONARQUBE_URL && cd ..'
        }
      }
    }

    // Currently disabled because of bug when running on different Jenkins agent/node from previous step. See https://groups.google.com/forum/#!msg/sonarqube/z_K_wz_8Vw8/-JJ0S-7ECAAJ
//    stage("Quality Gate"){
//      agent any
//      steps {
//        timeout(time: 1, unit: 'HOURS') { // Just in case something goes wrong, pipeline will be killed after a timeout
//          script {
//            def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
//            if (qg.status != 'OK') {
//              error "Pipeline aborted due to quality gate failure: ${qg.status}"
//            }
//          }
//        }
//      }
//    }

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
