#!groovy
@Library('dynamo-workflow-libs') _

pipeline {
    agent none
    environment {
        GITUSER = credentials('jenkins-dynamo')
    }
    options {
        // Only keep the 10 most recent builds
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    stages {

        stage('Build') {
            agent any
            tools {
                maven 'Maven'
            }
            steps {
                withSonarQubeEnv('My SonarQube Server') {
                    script {
                        if (env.BRANCH_NAME == "master") {
                            sh 'git remote update'
                            sh 'git fetch'
                            sh 'git checkout --track origin/$BRANCH_NAME'

                            def pom = readMavenPom file: 'usef-build/pom.xml'
                            env.devVersion = pom.version
                            env.version = pom.version.replace("-SNAPSHOT", ".${currentBuild.number}")
                            sh "mvn -f usef-build/pom.xml versions:set -DnewVersion=$version"
                            sh 'mvn -f usef-build/pom.xml clean deploy'

                            sh 'git tag -a $version -m "New release"'
                            sh 'git push https://${GITUSER_USR}:${GITUSER_PSW}@github.com/Alliander/ri.usef.energy.git $version'
                        } else {
                            sh 'mvn -f usef-build/pom.xml clean deploy'
                        }
                    }
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
