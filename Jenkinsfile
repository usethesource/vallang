node {
  def mvnHome = tool 'M3'

  stage 'Clone'
  checkout scm

  stage 'Build and Test'
<<<<<<< HEAD
  sh "${mvnHome}/bin/mvn -B clean install"

  stage 'Deploy'
  sh "${mvnHome}/bin/mvn -s /var/jenkins_home/usethesource-maven-settings.xml -B deploy"

  stage 'Archive'
  step([$class: 'ArtifactArchiver', artifacts: '**/target/*.jar', fingerprint: true])
  // step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
=======
  def mvnHome = tool 'M3'

  sh "${mvnHome}/bin/mvn -Dmaven.repo.local=/var/jenkins_home/repo -B clean install"

  stage 'Archive'
  step([$class: 'ArtifactArchiver', artifacts: '**/target/*.jar', fingerprint: true])
  step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
>>>>>>> 9490468... new Jenkinsfile
}
