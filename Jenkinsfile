pipeline {
    agent any

    environment {
        GH_USERNAME     = credentials('gh-user')
        GH_TOKEN        = credentials('gh-token')
    }

    parameters {
        string(name: 'EXTRA_OPTS', defaultValue: '--no-build-cache --no-daemon --console=plain --info', description: 'Gradle Extra Options')
        choice(
            choices: ['NO', 'YES'],
            name: 'OPT_BUILD_DOCKER',
        )
    }

    stages {
        stage('Build') {
            steps {
                sh "chmod u+x ./gradlew"
                sh "./gradlew ${params.EXTRA_OPTS} build distTar -x distZip -x shadowJar"
            }
        }
        stage('Docker Build Image') {
            when {
                expression {
                    return params.OPT_BUILD_DOCKER == "YES"
                }
            }
            environment {
                IMAGE_BUILD_TAG = "ci-${BRANCH_NAME}-${BUILD_NUMBER}"
            }
            agent { label 'docker' }
            steps {
                sh "./build-docker.sh"
            }

        }
    }
}
