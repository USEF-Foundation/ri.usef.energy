def buildClosure = {
    slackSend color: "#439FE0", message: "Build Started: ${env.JOB_NAME} ${env.BUILD_NUMBER}"
    env.PATH = "${tool 'Maven'}/bin:${env.PATH}"
    sh 'cd usef-build && mvn clean install'
    slackSend color: "#00A000", message: "Build End: ${env.JOB_NAME} ${env.BUILD_NUMBER}"
}

def buildParameterMap = [:]
buildParameterMap['appName'] = 'ri.usef-dynamo.nl'
buildParameterMap['buildClosure'] = buildClosure

buildAndDeployGeneric(buildParameterMap)
