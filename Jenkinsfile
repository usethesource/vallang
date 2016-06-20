node {
  def mvnHome = tool 'M3'

  stage 'Clone'
  checkout scm

  stage 'Build and Test'
  sh "${mvnHome}/bin/mvn -B clean install"

  stage 'Deploy'
  sh "${mvnHome}/bin/mvn -s /var/jenkins_home/usethesource-maven-settings.xml -B deploy"

  stage 'Archive'
  step([$class: 'ArtifactArchiver', artifacts: '**/target/*.jar', fingerprint: true])
  // step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
}
