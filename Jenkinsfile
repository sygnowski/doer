pipeline {
    agent any

    environment {
        GH_USERNAME     = credentials('gh-user')
        GH_TOKEN        = credentials('gh-token')
    }

    parameters {
        string(name: 'EXTRA_OPTS', defaultValue: '--no-build-cache --no-daemon --console=plain --info -x shadowJar', description: 'Gradle Extra Options')
    }

    stages {
        stage('Build') {
            steps {
                sh "chmod u+x ./gradlew"
                sh "./gradlew ${params.EXTRA_OPTS} build"
            }
        }
    }
}
