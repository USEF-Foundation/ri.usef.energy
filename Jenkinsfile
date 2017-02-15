node {
        stage "build USEF-RI"
        sh 'export'
    	env.PATH = "${tool 'Maven'}/bin:${env.PATH}"
    	checkout scm
    	sh 'cd usef-build && mvn clean install'
}
