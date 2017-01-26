node {
	
	def java = tool "java8"
	def gradle = tool "Gradle 3.2.1"

	stage('Fetch Repository') {
		sshagent(["jenkins"]) {
		    git url: "ssh://jenkins@gerrit.flavia-it.de:29418/FlaviaHomepageJobConfigurator"
		    def changeBranch = "change-${GERRIT_CHANGE_NUMBER}-${GERRIT_PATCHSET_NUMBER}"
		    sh "git fetch origin ${GERRIT_REFSPEC}:${changeBranch}"
		    sh "git checkout ${changeBranch}"
        }  
	}

	stage('Build') {
		withEnv(["PATH=$java/bin:${env.PATH}", "JAVA_HOME=$java", "PATH=$gradle/bin:${env.PATH}"]) {
			sh '''
				gradle clean
				gradle test
				gradle jfxjar
			'''
		}
	}

	stage('Post Build') {
		sh '''
			rm -r JobConfigurator
			mkdir JobConfigurator
			cp GUI/build/jfx/app/JobConfiguratorGUI.jar JobConfigurator
			cp -r GUI/build/jfx/app/lib JobConfigurator
		'''
		archiveArtifacts 'JobConfigurator/'
	}

}