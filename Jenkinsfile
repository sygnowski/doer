pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh "chmod u+x ./gradlew"
                sh "./gradlew --no-build-cache --no-daemon --console=plain --info -x shadowJar build"
            }
        }
    }
}
