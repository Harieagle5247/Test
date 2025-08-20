def call(){
    pipeline {
        agent any
        tools{
            maven "maven1"
        }
        parameters{
            string(name: 'BRANCH_NAME',defaultValue: 'main',description: 'Git Branch to Build (String)')
            //string(name: 'GitURL',description: 'Git Repo URL')
            choice(name: 'DEPLOY_ENV',choices: ['Dev','Qa','Prod'],description: 'Select Deployment Environment')
        }
        environment{
            GitURL = "https://github.com/Harieagle5247/simple-java-maven-app.git"
            ARTIFACT_REPO = "/var/artifacts"
            BUILD_INFO    = "${env.JOB_NAME}-${env.BUILD_NUMBER}"
            GIT_CREDENTIALS_ID = 'github'
        }
        stages{
            stage('Checkout'){
                steps{
                    checkoutCode(params.BRANCH_NAME,env.GitURL)
                }
            }
            stage('Build'){
                steps{
                    buildCode()
                }
            }
            stage('Parallel'){
                parallel{
                    stage('sample Test'){
                        steps{
                            sampleTest()
                        }
                    }
                    stage('simple Test'){
                        steps{
                            simpleTest()
                        }
                    }
                    stage('junit Test'){
                        steps{
                            junitTest()
                        }
                    }
                }
            }
            stage('Deploy'){
                steps{
                    deployApp()
                }
            }
            
        }
    post{
        always{
            echo "Pipeline finished for job: ${env.JOB_NAME}, build: ${env.BUILD_NUMBER}"
        }
        success{
            echo "✅ Build & Deploy succeeded! in ${params.DEPLOY_ENV} environment."
        }
        failure{
            echo "❌ Build failed. Check logs."
        }
    }
    }
}

def checkoutCode(branch,url){
    echo "Checking out Branch : ${params.BRANCH_NAME}"
    git branch: branch,url: url
}

def buildCode(){
    //sh 'mvn -B -DskipTests clean package'
    runCommand("mvn -B -DskipTests clean package")
}

def sampleTest(){
    sh 'mvn test'
    println "sample Testing"
}

def simpleTest(){
    println "simple Testing"
}

def junitTest(){  
    println "junit Testing"
}

def runCommand(cmd) {
    echo "Running command: ${cmd}"
    sh "${cmd}"
}

def deployApp(){
    def artifactPath = "target/*.jar"
    def destPath = "${ARTIFACT_REPO}/${params.DEPLOY_ENV}/${BUILD_INFO}"

    archiveArtifacts artifacts: artifactPath, fingerprint: true

    deployArtifact(artifactPath, destPath,"${params.DEPLOY_ENV}")
}

def deployArtifact(src,dest,deploy_env){
    echo "Preparing Deployment for environment : ${deploy_env}"
    if(deploy_env == 'Dev'){
        deployDev(src,dest)
    }
    else if(deploy_env == 'Qa'){
        deployQa(src,dest)
    }
    else if(deploy_env == 'Prod'){
        approveProdDeploy()
        deployProd(src,dest)
    }
    else{
        error "❌ Unknown environment: ${deploy_env}"
    }
}

def deployDev(src,dest){
    echo "➡️ DEV environment: Only copying locally"
}

def deployQa(src,dest){
    echo "➡️ QA environment: Deploying to QA artifacts directory"
}

def deployProd(src,dest){
    echo "➡️ PROD environment: Deploying to remote production server"
    echo "Production deployment complete 🚀"
}

def approveProdDeploy(){
    def userChoice = input(
        message: "⚠️ Production Deployment Approval Required",
        parameters: [
            [$class: 'ChoiceParameterDefinition', 
             choices: "Approve\nDeny", 
             description: 'Select your action', 
             name: 'ACTION']
        ]
    )
    echo "✅ Selected action: ${userChoice}"

    if (userChoice == 'Approve') {
        echo "Approved....Starting Deploying to Prodcution"
    } else {
        currentBuild.result = 'ABORTED'
        error("User denied deployment. Pipeline stopped.")
    }
}
