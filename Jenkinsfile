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
        // send build started notifications
        sendNotifications 'STARTED'
      }
    }

    stage ('Build') {
      steps {
        script {
          //env.PATH = "${tool 'Maven'}/bin:${env.PATH}" //TODO: change me into descriptive form
          sh env
          sh 'cd usef-build && mvn clean deploy && cd ..' //TODO: change me into descriptive form
        }
      }
    }

    stage ('Docker Build & Push') { //TODO: remove me
      steps {
        script {
          def branchName = env.BRANCH_NAME?.replaceAll("origin/", "")?.replaceAll("/", "_") //TODO: change me into descriptive form
          def artifactName = branchName == "master"? "" : branchName + "-" //TODO: change me into descriptive form
          artifactName += env.BUILD_TIMESTAMP //TODO: change me into descriptive form
          def dockerImage = "${myArtifact}:${artifactName}" //TODO: change me into descriptive form
          def dockerImageUrl = "${registryServer}/${dockerImage}" //TODO: change me into descriptive form
          def registryServer = env.REGISTRY_SERVER //TODO: change me into descriptive form
          echo "registryServer: " + registryServer
          echo "dockerImage: " + dockerImage
        }

        //docker.withRegistry("https://${registryServer}") {
        //  def pcImg = docker.build("usefdynamo/ri.usef-dynamo.nl:ci-${env.BUILD_TIMESTAMP}") //TODO: make variable
        //  pcImg.push();
        //}
      }
    }

    stage ("Deploy to dynamo-int") {
      steps {
        //deploy(workingDir, 'ri.usef-dynamo.nl', env.NAMESPACE_INT, dockerImageUrl)
      }
    }

    stage ("Deploy to dynamo-acc") {
      steps {
        //deploy(workingDir, 'ri.usef-dynamo.nl', env.NAMESPACE_ACC, dockerImageUrl)
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
        //deploy(workingDir, 'ri.usef-dynamo.nl', env.NAMESPACE_PRD, dockerImageUrl)
      }
    }
  }
  post {
    always {
      sendNotifications currentBuild.result
    }
  }
}
