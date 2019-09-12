node {
  try { 
    stage('Clone') {
        checkout scm
    }
    
    withMaven(maven: 'M3', jdk: 'jdk-oracle-8', options: [artifactsPublisher(disabled: true), junitPublisher(disabled: false)] ) {
        stage('Build and Test') {
            sh "mvn clean test"
        }

        stage ('sonar cloud') {
          sh "mvn -DskipTests sonar:sonar -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.projectKey=usethesource_vallang -Dsonar.organization=usethesource  -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=${VALLANG_SONAR_CLOUD}"
        }

        stage('QA') {
            sh "mvn clean compile -P checker-framework"
        }

        stage('Deploy') {
            if (env.BRANCH_NAME == "master" || env.BRANCH_NAME == "jenkins-deploy" || env.BRANCH_NAME == "remove-annotions-ci") {
                sh "mvn clean -DskipTests deploy"
            }
        }
    }
    
    if (currentBuild.previousBuild.result == "FAILURE") { 
  	  slackSend (color: '#5cb85c', channel: "#usethesource", message: "BUILD BACK TO NORMAL:  <${env.BUILD_URL}|${env.JOB_NAME} [${env.BUILD_NUMBER}]>")
    }
  } catch(e) {
  	  slackSend (color: '#d9534f', channel: "#usethesource", message: "FAILED: <${env.BUILD_URL}|${env.JOB_NAME} [${env.BUILD_NUMBER}]>")
      throw e
  }
}
