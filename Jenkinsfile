def buildClosure = {
    sh "./mvn clean install -DBUILD_NUMBER=${env.BUILD_NUMBER} -DBUILD_TIMESTAMP=${env.BUILD_TIMESTAMP} -DBRANCH_NAME=${env.BRANCH_NAME}"
}

def buildParameterMap = [:]
buildParameterMap['appName'] = 'usef-ri'
buildParameterMap['buildClosure'] = buildClosure

buildAndDeployGeneric(buildParameterMap)