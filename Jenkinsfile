node {
        stage "build USEF-RI"
    	env.PATH = "${tool 'Maven'}/bin:${env.PATH}"
    	checkout scm
    	sh 'cd usef-build && mvn clean install'
}