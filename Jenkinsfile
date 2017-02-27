node {
        stage "build USEF-RI"
        slackSend color: "#439FE0", message: "Build Started: ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        sh 'export'
    	env.PATH = "${tool 'Maven'}/bin:${env.PATH}"
    	checkout scm
    	sh 'cd usef-build && mvn clean install'
        slackSend color: "#00A000", message: "Build End: ${env.JOB_NAME} ${env.BUILD_NUMBER}"
}
