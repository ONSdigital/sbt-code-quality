#!groovy

def artServer = Artifactory.server 'art-p-01'
def buildInfo = Artifactory.newBuildInfo()
def distDir = 'build/dist/'

pipeline {
    libraries {
        lib('jenkins-pipeline-shared@feature/sbt-config')
    }
    agent any
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '7'))
        timeout(time: 30, unit: 'MINUTES')
    }
    environment {
        MODULE_NAME = 'sbt-code-quality'
    }
    
    stages {
        stage('Checkout') {
            agent { label 'download.jenkins.slave' }
            steps {
                deleteDir()
                checkout scm        
                script {
                    buildInfo.name = "${MODULE_NAME}"
                    buildInfo.number = "${BUILD_NUMBER}"
                    buildInfo.env.collect()
                }
                colourText("info","BuildInfo: ${buildInfo.name}-${buildInfo.number}")
                stash name: 'Checkout'
            }
        }
        stage('Build'){
            agent { label 'build.sbt_0-13-13' }
            steps {
                unstash name: 'Checkout'
                sh "sbt compile"
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE_NAME} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE_NAME} failed!")
                }
            }
        }
        stage ('Publish') {
            agent { label 'build.sbt_0-13-13' }
            when { 
                branch "master" 
                // evaluate the when condition before entering this stage's agent, if any
                beforeAgent true 
            }
            steps {
                colourText("info", "Building ${env.BUILD_ID} on ${env.JENKINS_URL} from branch ${env.BRANCH_NAME}")
                unstash name: 'Checkout'
                sh 'sbt package'
                script {
                    def uploadSpec = """{
                        "files": [
                            {
                                "pattern": "target/scala-2.10/**/*.jar",
                                "target": "registers-sbt-snapshots/uk/gov/ons/${buildInfo.name}/${buildInfo.number}/"
                            }
                        ]
                    }"""
                    artServer.upload spec: uploadSpec, buildInfo: buildInfo
                }
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE_NAME} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE_NAME} failed!")
                }
            }
        }
    }
    post {
        success {
            colourText("success", "All stages complete. Build was successful.")
            slackSend(
                color: "good",
                message: "${env.JOB_NAME} success: ${env.RUN_DISPLAY_URL}"
            )
        }
        unstable {
            colourText("warn", "Something went wrong, build finished with result ${currentResult}. This may be caused by failed tests, code violation or in some cases unexpected interrupt.")
            slackSend(
                color: "danger",
                message: "${env.JOB_NAME} unstable: ${env.RUN_DISPLAY_URL}"
            )
        }
        failure {
            colourText("warn","Process failed at: ${env.NODE_STAGE}")
            slackSend(
                color: "danger",
                message: "${env.JOB_NAME} failed at ${env.STAGE_NAME}: ${env.RUN_DISPLAY_URL}"
            )
        }
    }
}