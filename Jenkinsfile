node {
        stage "build USEF-RI"
        sh 'echo $ARTIFACTORY_USERNAME $ARTIFACTORY_PASSWORD'
    	env.PATH = "${tool 'Maven'}/bin:${env.PATH}"
    	checkout scm
    	sh 'cd usef-build && mvn clean install'
}
