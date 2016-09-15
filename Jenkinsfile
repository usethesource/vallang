node {
  try { 
    def mvnHome = tool 'M3'
    env.JAVA_HOME="${tool 'jdk-oracle-8'}"
    env.PATH="${env.JAVA_HOME}/bin:${mvnHome}/bin:${env.PATH}"
    
    stage 'Clone'
    checkout scm
    
    stage 'Build and Test'
    sh "mvn -B clean install"
    
    stage 'Deploy'
    sh "mvn -s ${env.HOME}/usethesource-maven-settings.xml -B deploy"
    
    if (currentBuild.previousBuild.result == "FAILURE") { 
  	  slackSend (color: '#5cb85c', message: "BUILD BACK TO NORMAL:  <${env.BUILD_URL}|${env.JOB_NAME} [${env.BUILD_NUMBER}]>")
    }
      
    build job: '../rascal-eclipse/master', wait: false
  } catch(e) {
  	  slackSend (color: '#d9534f', message: "FAILED: <${env.BUILD_URL}|${env.JOB_NAME} [${env.BUILD_NUMBER}]>")
      throw e
  }
}