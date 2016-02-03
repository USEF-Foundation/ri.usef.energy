Param
 (
  [Parameter(Mandatory=$True)]
  [ValidateNotNull()]
  [ValidateSet("agr","brp","cro","dso")]
  $role,
  [Parameter(Mandatory=$True)]
  [ValidateNotNull()]
  $instance
)

${spec-version} = "      spec-version: ""2014:I"""
${public-keys} = "        - public-keys: ""cs1.GuuL6Z0iQlqpylcy4KmcQ6Y+C9Ljtq8BcKhCzJxBL5Y="""
${role-line} = "      ${role}-role:"
${domain-name} = "    - domain-name: ""${role}${instance}.usef-example.com"""
${hostname} = "          hostname: ""localhost:8080/${role}${instance}"""
Add-Content -Path "C:\Users\brouwee\.usef\participants_dns_info.yaml" ${domain-name}
Add-Content -Path "C:\Users\brouwee\.usef\participants_dns_info.yaml" ${spec-version}
Add-Content -Path "C:\Users\brouwee\.usef\participants_dns_info.yaml" ${role-line}
Add-Content -Path "C:\Users\brouwee\.usef\participants_dns_info.yaml" ${public-keys}
Add-Content -Path "C:\Users\brouwee\.usef\participants_dns_info.yaml" ${hostname}

$env:NOPAUSE = "TRUE"

${folder} = (Get-Item -Path ".\" -Verbose).FullName

${lower} = "$role".ToLower()
${upper} = "$role".ToUpper()
${camel} = [Regex]::Replace(${lower} , '\b.', {  $args[0].Value.ToUpper() })

${template} = "${folder}/usef-deployment-${role}"
${deployment} = "${folder}/usef-deployment-${role}${instance}"

${resources} = "src\main\resources"
${webapp} = "src\main\webapp"
${java} = "src\main\java"
${meta-inf} = "src\main\resources\META-INF"
${web-inf} = "${webapp}\WEB-INF"

${meta-inf-template} = "${template}\${meta-inf}"
${meta-inf-template} = "${template}\src\main\resources\META-INF"

$artifactId = "<artifactId>usef-deployment-${lower}</artifactId>"
$clonedArtifactId = "<artifactId>usef-deployment-${lower}${instance}</artifactId>"

$finalName = "<finalName>${lower}</finalName>"
$clonedfinalName = "<finalName>${lower}${instance}</finalName>"

$war = "$deployment/target/${lower}${instance}.war"
${queue-in} = "IN_QUEUE_${upper}"
${jndi-in} = "usefInQueue${camel}" 
${queue-out} = "OUT_QUEUE_${upper}" 
${jndi-out} = "usefOutQueue${camel}" 
${queue-not-sent} = "NOT_SENT_QUEUE_${upper}" 
${jndi-not-sent} = "usefNotSentQueue${camel}" 




If (Test-Path ${deployment}){
	Remove-Item ${deployment} -recurse
}

New-Item -ItemType Directory -Path ${deployment}\${java}
New-Item -ItemType Directory -Path ${deployment}\${meta-inf}
New-Item -ItemType Directory -Path ${deployment}\${web-inf}

(get-content ${template}/pom.xml) | foreach-object {$_ -replace "${artifactId}", "${clonedArtifactId}" -replace "${finalName}", "${clonedFinalName}"} | set-content ${deployment}/pom.xml
(get-content ${template}/${meta-inf}/create-script.sql) | foreach-object {$_ -replace "${upper}", "${upper}${instance}"} | set-content ${deployment}/${meta-inf}/create-script.sql
(get-content ${template}/${meta-inf}/drop-script.sql) | foreach-object {$_ -replace "${upper}", "${upper}${instance}"} | set-content ${deployment}/${meta-inf}/drop-script.sql
(get-content ${template}/${meta-inf}/load-script.sql) | foreach-object {$_ -replace "${upper}", "${upper}${instance}"} | set-content ${deployment}/${meta-inf}/load-script.sql
(get-content ${template}/${meta-inf}/persistence.xml) | foreach-object {$_ -replace "<property name=""hibernate.default_schema"" value=""${upper}"" />", "<property name=""hibernate.default_schema"" value=""${upper}${instance}"" />"} | set-content ${deployment}/${meta-inf}/persistence.xml
(get-content ${template}/${resources}/logback.xml) | foreach-object {$_ -replace "/${lower}/", "/${lower}${instance}/" -replace "LogBack${camel}.xml", "LogBack${camel}${instance}.xml" }| set-content ${deployment}/${resources}/logback.xml
(get-content ${template}/${web-inf}/jboss-deployment-structure.xml) | set-content ${deployment}/${web-inf}/jboss-deployment-structure.xml
(get-content ${template}/${web-inf}/jboss-ejb3.xml) | foreach-object {$_ -replace "${jndi-in}", "${jndi-in}${instance}" -replace "${jndi-out}", "${jndi-out}${instance}"} | set-content ${deployment}/${web-inf}/jboss-ejb3.xml
(get-content ${template}/${web-inf}/jboss-web.xml) | foreach-object {$_ -replace "<context-root>/${lower}</context-root>","<context-root>/${lower}${instance}</context-root>" -replace "${jndi-in}", "${jndi-in}${instance}" -replace "${jndi-out}", "${jndi-out}${instance}"} | set-content ${deployment}/${web-inf}/jboss-web.xml
(get-content ${template}/${web-inf}/queue-config-jms.xml) | foreach-object {$_ -replace "${queue-not-sent}", "${queue-not-sent}${instance}" -replace "${jndi-not-sent}","${jndi-not-sent}${instance}"  -replace "${queue-in}", "${queue-in}${instance}" -replace "${jndi-in}", "${jndi-in}${instance}" -replace "${queue-out}", "${queue-out}${instance}" -replace "${jndi-out}", "${jndi-out}${instance}" } | set-content ${deployment}/${web-inf}/queue-config-jms.xml
(get-content ${template}/${web-inf}/web.xml) | set-content ${deployment}/${web-inf}/web.xml
(get-content ${template}/${webapp}/not-found.html) | set-content ${deployment}/${webapp}/not-found.html

Push-Location -Path ${deployment}
mvn clean install
Pop-Location

C:/builds/Tools/wildfly-8.1.0.Final/bin/jboss-cli.bat --connect --command="deploy ${war} --force"
