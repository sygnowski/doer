pipeline {
    agent any

    environment {
        GH_USERNAME     = credentials('gh-user')
        GH_TOKEN        = credentials('gh-token')
        DOCKER_PASSWD   = credentials('lab-docker-passwd')
    }

    parameters {
        string(
            name: 'EXTRA_OPTS',
            defaultValue: '--no-build-cache --no-daemon --console=plain --info',
            description: 'Gradle Extra Options'
        )
        choice(
            description: 'Build Docker Image',
            choices: ['NO', 'YES'],
            name: 'OPT_BUILD_DOCKER'
        )
        choice(
            description: 'Publish Docker Image',
            choices: ['NO', 'YES'],
            name: 'OPT_PUBLISH_DOCKER'
        )
    }

    stages {
        stage('Build') {
            steps {
                sh "chmod -v u+x ./gradlew"
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
                //IMAGE_BUILD_TAG = "ci-${BRANCH_NAME}-${BUILD_NUMBER}"
                IMAGE_BUILD_TAG = "${BRANCH_NAME}"
                DOCKER_PUBLISH_IMAGE = "${params.OPT_PUBLISH_DOCKER}"
            }
            agent { label 'docker' }
            steps {
                println "Env IMAGE_BUILD_TAG: $env.IMAGE_BUILD_TAG"
                sh "./build-docker.sh"
            }
        }
    }
}
