#!groovy
@Library('dynamo-workflow-libs') _

pipeline {
  agent any
  options {
    // Only keep the 10 most recent builds
    buildDiscarder(logRotator(numToKeepStr:'10'))
  }
  stages {

    stage ('Start') {
      steps {
        // send build started notifications
        sendNotifications 'STARTED'
      }
    }

    stage ('Build') {
      steps {
        script {
          env.PATH = "${tool 'Maven'}/bin:${env.PATH}" //TODO: change me into desciptive form
          sh 'cd usef-build && mvn clean deploy && cd ..' //TODO: change me into desciptive form
        }
      }
    }

    stage ('Docker Build & Push') {
      steps {
        script {
          def branchName = env.BRANCH_NAME?.replaceAll("origin/", "")?.replaceAll("/", "_") //TODO: change me into desciptive form
          def artifactName = branchName == "master"? "" : branchName + "-" //TODO: change me into desciptive form
          artifactName += env.BUILD_TIMESTAMP //TODO: change me into desciptive form
          def dockerImage = "${myArtifact}:${artifactName}" //TODO: change me into desciptive form
          def dockerImageUrl = "${registryServer}/${dockerImage}" //TODO: change me into desciptive form
          def registryServer = env.REGISTRY_SERVER //TODO: change me into desciptive form
          echo "registryServer: " + registryServer
          echo "dockerImage: " + dockerImage
        }

        docker.withRegistry("https://${registryServer}") {
          def pcImg = docker.build("${dockerImage}")
          pcImg.push();
        }
      }
    }

    stage ("Deploy to dynamo-int") {
      steps {
        script {
          def namespaceInt = env.NAMESPACE_INT //change me into desciptive form
          def appName = 'ri.usef-dynamo.nl' //TODO: change me into desciptive form
        }
        //deploy(workingDir, appName, namespaceInt, dockerImageUrl)
      }
    }

    stage ("Deploy to dynamo-acc") {
      steps {
        script {
          def namespaceAcc = env.NAMESPACE_ACC //TODO: change me into desciptive form
          def appName = 'ri.usef-dynamo.nl' //TODO: change me into desciptive form
        }
        //deploy(workingDir, appName, namespaceAcc, dockerImageUrl)
      }
    }

    stage ("Approval for deploy to PRD") {
      steps {
        timeout(time:5, unit:'DAYS') {
          input "Deploy to dynamo-prd?"
        }
      }
    }

    stage ("Deploy to dynamo-prd") {
      steps {
        script {
          def namespacePrd = env.NAMESPACE_PRD //change me into desciptive form //TODO: change me into desciptive form
          def appName = 'ri.usef-dynamo.nl' //TODO: change me into desciptive form
        }
        //deploy(workingDir, appName, namespacePrd, dockerImageUrl)
      }
    }
  }
  post {
    always {
      sendNotifications currentBuild.result
    }
  }
}
