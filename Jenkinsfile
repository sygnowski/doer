pipeline {
    agent any

    environment {
        GH_USERNAME     = credentials('gh-user')
        GH_TOKEN        = credentials('gh-token')
    }

    stages {
        stage('Build') {
            steps {
                sh "chmod u+x ./gradlew"
                sh "./gradlew --no-build-cache --no-daemon --console=plain --info -x shadowJar build"
            }
        }
    }
}
