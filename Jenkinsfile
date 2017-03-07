#!groovy
@Library('dynamo-workflow-libs') _

pipeline {
  agent any
  options {
    // Only keep the 10 most recent builds
    buildDiscarder(logRotator(numToKeepStr:'10'))
  }
  stages {

    def appName = 'ri.usef-dynamo.nl'

    def branchName = env.BRANCH_NAME?.replaceAll("origin/", "")?.replaceAll("/", "_") //change me into desciptive form
    def artifactName = branchName == "master"? "" : branchName + "-" //change me into desciptive form
    artifactName += env.BUILD_TIMESTAMP //change me into desciptive form
    def dockerImage = "${myArtifact}:${artifactName}" //change me into desciptive form
    def dockerImageUrl = "${registryServer}/${dockerImage}" //change me into desciptive form
    def registryServer = env.REGISTRY_SERVER //change me into desciptive form

    def namespaceInt = env.NAMESPACE_INT //change me into desciptive form
    def namespaceAcc = env.NAMESPACE_ACC //change me into desciptive form
    def namespacePrd = env.NAMESPACE_PRD //change me into desciptive form

    stage ('Start') {
      steps {
        // send build started notifications
        //sendNotifications 'STARTED'
      }
    }

    stage ('Build') {
      steps {
        env.PATH = "${tool 'Maven'}/bin:${env.PATH}" //change me into desciptive form
        sh 'cd usef-build && mvn clean deploy && cd ..' //change me into desciptive form
      }
    }

    stage ('Docker Build & Push') {
      steps {
        docker.withRegistry("https://${registryServer}") {
          def pcImg = docker.build("${dockerImage}")
          pcImg.push();
        }
      }
    }

    stage ("Deploy to ${namespaceInt}") {
      steps {
        //deploy(workingDir, appName, namespaceInt, dockerImageUrl)
      }
    }

    stage ("Deploy to ${namespaceAcc}") {
      steps {
        //deploy(workingDir, appName, namespaceAcc, dockerImageUrl)
      }
    }

    stage ("Approval for deploy to PRD") {
      steps {
        timeout(time:5, unit:'DAYS') {
          input "Deploy to ${namespacePrd}?"
        }
      }
    }

    stage ("Deploy to ${namespacePrd}") {
      steps {
        //deploy(workingDir, appName, namespacePrd, dockerImageUrl)
      }
    }
  }
  post {
    always {
      //sendNotifications currentBuild.result
    }
  }
}



//def buildClosure = {
//    slackSend color: "#439FE0", message: "Build Started: ${env.JOB_NAME} ${env.BUILD_NUMBER}"
//    env.PATH = "${tool 'Maven'}/bin:${env.PATH}"
//    sh 'cd usef-build && mvn clean deploy && cd ..'
//    slackSend color: "#00A000", message: "Build End: ${env.JOB_NAME} ${env.BUILD_NUMBER}"
//}

//def buildParameterMap = [:]
//buildParameterMap['appName'] = 'ri.usef-dynamo.nl'
//buildParameterMap['buildClosure'] = buildClosure
//
//buildAndDeployGeneric(buildParameterMap)
