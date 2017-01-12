#!/usr/bin/env groovy
node {
	

   def mvnHome
   /** Should be the repository name of the project without .git. 
   in the future it should be come from the enviroment of jenkins
   */
   def DISPLAY_NAME = "gitprojectname"
   
   
   stage('Preparation') { // for display purposes
   	  sh 'env > env.txt'
        for(String  envtext : readFile('env.txt').split("\r?\n")) {
        println envtext
      }
      
      // wieder in ein Fehler gelaufen
      // https://issues.jenkins-ci.org/browse/JENKINS-36578
      println env.JOB_NAME
      def displayname = getDisplayName()
      if(displayname) {
        DISPLAY_NAME = displayname
      }
      println "DISPLAY_NAME:${DISPLAY_NAME}" 
       // Get some code from a GitHub repository
      git("ssh://jenkins@git:29418/${DISPLAY_NAME}.git")
      // Get the Maven tool.
      // ** NOTE: This 'M3' Maven tool must be configured
      // **       in the global configuration.           
      mvnHome = tool 'M3'
      
   
   }
   stage('Build') {
      // Run the maven build
      try {
          if (isUnix()) {
             sh "'${mvnHome}/bin/mvn' -Dadditionalparam=-Xdoclint:none -Dmaven.test.failure.ignore clean package "
          } else {
             bat(/"${mvnHome}\bin\mvn" -Dadditionalparam=-Xdoclint:none -Dmaven.test.failure.ignore clean package/)
          }
      }
      catch (e) {
        makeGitBlitTicket(DISPLAY_NAME, e)
      }
   }
   
  stage('UnitTest') {
      try {
          if (isUnix()) {
             sh "'${mvnHome}/bin/mvn' -Dadditionalparam=-Xdoclint:none test "
          } else {
             bat(/"${mvnHome}\bin\mvn" -Dadditionalparam=-Xdoclint:none test/)
          }
      }
      catch (e) {
        makeGitBlitTicket(DISPLAY_NAME, e)
      }
  	  
  }   
  stage('SonarQube analysis') {
      sh "'${mvnHome}/bin/mvn' org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar"
  }
   stage('Results') {
      junit '**/target/surefire-reports/TEST-*.xml'
      archive 'target/*.jar'
   }
}

@NonCPS
def getTicketId(text, DISPLAY_NAME) {
  def matcher = text =~ /tickets\?r=${DISPLAY_NAME}\.git&h=(.+)/
  if(matcher.find()) {
  	  return matcher.group(1)
  }
  return null
}

@NonCPS
def getDisplayName() {
    def rueck = null
    def m = env.JOB_NAME =~ /^([\w-]+)\s+.*$/
    if (m) {
      rueck = m.group(1)
    }
      
    return rueck
}

def makeGitBlitTicket(DISPLAY_NAME, e) {
            sh "git checkout -b jenkinsbuild${env.BUILD_ID}"
            //...add a single commit...
            // log file would be nice, but:
            // https://issues.jenkins-ci.org/browse/JENKINS-28119
            writeFile(file:'.jenkinserror', text:"Build " + env.BUILD_ID + ":" + e.toString())
            sh "git add .jenkinserror"
            sh "git commit -m \"jenkind error report commit with build ${env.BUILD_ID} on ${env.JENKINS_URL}\" .jenkinserror"
            // workaround https://issues.jenkins-ci.org/browse/JENKINS-26133
            def rueck = sh(script:"git push ssh://jenkins@git:29418/${DISPLAY_NAME}.git HEAD:refs/for/new%t=jenkinsbuilderror${env.BUILD_ID},r=sjatgutzmann,cc=jenkins > .gitblitticket", returnStdout:true).trim() 
            // read ticket id from server output
            def id = getTicketId(rueck, DISPLAY_NAME)
            if(id) {
                println "create new ticket on gitblit with the id ${id}"
                sh "git fetch"
                sh "git branch -u origin/ticket/${id}"
                sh "git branch -D jenkinsbuilderror${env.BUILD_ID}"
            } else {
            	def gitblitticket = readFile('.gitblitticket').trim()
            	println "gitblitticket:${gitblitticket}"
            	if(gitblitticket) {
            		id = getTicketId(gitblitticket, DISPLAY_NAME)
            	} 
            	if(!id) {
            		error("Could not get Ticket Id from gitblit with rueck=${rueck} | and DISPLAY_NAME:${DISPLAY_NAME}")
            	}
            }
}