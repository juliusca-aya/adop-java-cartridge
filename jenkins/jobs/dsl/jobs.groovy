def ProjectFolderName= "${PROJECT_NAME}/SchoolSchedule"

def generateBuildPipelineView = buildPipelineView(ProjectFolderName + "/Java_Build_Pipeline_View")
def generateBuildJob = freeStyleJob(ProjectFolderName + "/Build_Java_Project")
def generateSonarJob = freeStyleJob(ProjectFolderName + "/Sonar_Java_Project")
def generateDeployJob = freeStyleJob(ProjectFolderName + "/Deploy_Java_Project")
def generateSeleniumJob = freeStyleJob(ProjectFolderName + "/Selenium_Java_Project")
//def generateDeployJob = freeStyleJob(ProjectFolderName + "/Deploy_Java_Project")


folder('SchoolSchedule'){
	description ('Parent Folder')
}

generateBuildPipelineView.with {
	title('Java_Build_Pipeline_View')
		displayedBuilds(5)
		selectedJob(ProjectFolderName + "/Build_Java_Project")
		alwaysAllowManualTrigger()
		showPipelineParameters()
		refreshFrequency(5)
	}	





generateBuildJob.with {
  
       properties {
        copyArtifactPermissionProperty {
               projectNames('Deploy_Java_Project')
        	}
       }
  
	scm {
		git {
			remote {
				url("git@gitlab:${WORKSPACE_NAME}/SchoolSchedule.git")
              	credentials("adop-jenkins-master")
					}
			branch('*/master')
			}
      
		}
  
	wrappers {
		preBuildCleanup()
		timestamps()
 
			}
  
  triggers {
        gitlabPush {
            buildOnMergeRequestEvents(true)
            buildOnPushEvents(true)
            enableCiSkip(true)
            setBuildDescription(true)
            addNoteOnMergeRequest(true)
            rebuildOpenMergeRequest('never')
            addVoteOnMergeRequest(false)
            useCiFeatures(false)
            acceptMergeRequestOnSuccess()
        }
    }
  
  
	steps {
		maven {
			mavenInstallation('ADOP Maven')
			goals('''
				clean
				package
				''')
			}
			}
  
	publishers {
      
          	archiveArtifacts { 
                        pattern('target/*.war') 
                        onlyIfSuccessful(true) 
                }

			downstreamParameterized {
					trigger('Sonar_Java_Project') {
						condition('UNSTABLE_OR_BETTER')
						parameters {
								predefinedProps([CUSTOM_WORKSPACE: '$WORKSPACE'])
							}
						}
					}
				}
			}


generateSonarJob.with{
  
  
  configure {
    it / 'builders' << 'hudson.plugins.sonar.SonarRunnerBuilder' {
      properties 'sonar.projectKey=teamtae.org.tae \n sonar.projectName=SchoolSchedule \n sonar.projectVersion=1.0 \n sonar.sources=. \n sonar.language=java \n sonar.sourceEncoding=UTF-8 \n'
      javaOpts ''
      jdk '(Inherit From Job)'
      project ''
      task ''
    }
  }

  parameters{
    stringParam('CUSTOM_WORKSPACE')}
	customWorkspace('$CUSTOM_WORKSPACE')
	publishers{
		downstreamParameterized {
			trigger('Deploy_Java_Project') {
				condition('UNSTABLE_OR_BETTER')
				parameters {
						currentBuild()
							}
						}
					}
				}
			}

generateDeployJob.with{

  	wrappers {
        preBuildCleanup()
        sshAgent('ec2-user(ssh-key)')
    }
   label('ansible')
  
   multiscm {
        git {
            remote {
                url('git@gitlab:${WORKSPACE_NAME}/ansible-playbook.git')
              	credentials("adop-jenkins-master")
            }
            extensions {
                relativeTargetDirectory('ansibleplaybook_folder')
            }
          	branch('*/master')
        }
        git {
            remote {
                url('git@gitlab:teamtae/dockerfile_repo.git')
              	credentials("adop-jenkins-master")
            }
            extensions {
                relativeTargetDirectory('dockerfile_folder')
            }
          	branch('*/master')
        }
        git {
            remote {
                url('git@gitlab:teamtae/Pemfile.git')
              	credentials("adop-jenkins-master")
            }
            extensions {
                relativeTargetDirectory('pemfile_folder')
            }
          	branch('*/master')
        }
    }
  
  
 
  
  	steps {
        copyArtifacts('Build_Java_Project') {
            includePatterns('target/*.war')
            buildSelector {
                latestSuccessful(true)
            }
        }
     
    	shell('cd dockerfile_folder && chmod 400 teamtae.pem && scp -i teamtae.pem Dockerfile ../target/SchoolSchedule.war ec2-user@52.42.226.149:~/ && cd ../ansibleplaybook_folder && ansible-playbook playbook.yml -i hosts -u ec2-user && rm -rf teamtae.pem')
    }
  
  	publishers{
      
      downstream('Selenium_Java_Project', 'UNSTABLE')

		//downstreamParameterized {
		//	trigger('Selenium_Java_Project') {
		//		condition('UNSTABLE_OR_BETTER')
		//		parameters {
		//				currentBuild()
		//					}
		//				}
		//			}
				}
  
  

}

generateSeleniumJob.with{

  	scm {
		git {
			remote {
				url("git@gitlab:${WORKSPACE_NAME}/selenium.git")
              	credentials("adop-jenkins-master")
					}
			branch('*/master')
			}
      
		}
   	wrappers {
        preBuildCleanup()
    }
  
    triggers {
      
        gitlabPush {
            buildOnMergeRequestEvents(true)
            buildOnPushEvents(true)
            enableCiSkip(true)
            setBuildDescription(true)
            addNoteOnMergeRequest(true)
            rebuildOpenMergeRequest('never')
            addVoteOnMergeRequest(false)
            useCiFeatures(false)
            acceptMergeRequestOnSuccess()
        }
    }
  
	steps {
		maven {
			mavenInstallation('ADOP Maven')
			goals('''
				clean
				package
				''')
            rootPOM('WebTest/pom.xml')
			}
			}
  
			}
