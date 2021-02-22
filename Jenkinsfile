#!groovy
def artifactId = "keyple-java-service"
def keypleVersion
pipeline {
    agent {
        kubernetes {
            label ${artifactId}
            yaml javaBuilder('1')
        }
    }
    environment {
        uploadParams = "-PdoSign=true --info"
        forceBuild = false
        PROJECT_NAME = ${artifactId}
        PROJECT_BOT_NAME = "Eclipse Keyple Bot"
    }
    stages {
        stage('Import keyring'){
            when {
                expression { env.GIT_URL == "https://github.com/eclipse/${artifactId}.git" && env.CHANGE_ID == null }
            }
            steps{
                container('java-builder') {
                    configFileProvider(
                        [configFile(fileId: 'gradle.properties',
                            targetLocation: '/home/jenkins/agent/gradle.properties')]) {
                        sh 'ln -s /home/jenkins/agent/gradle.properties /home/jenkins/.gradle/gradle.properties'
                        /* Read key Id in gradle.properties */
                        sh 'head -1 /home/jenkins/.gradle/gradle.properties'
                    }
                    withCredentials([
                        file(credentialsId: 'secret-subkeys.asc',
                            variable: 'KEYRING')]) {
                        /* Import GPG keyring with --batch and trust the keys non-interactively in a shell build step */
                        sh 'gpg1 --batch --import "${KEYRING}"'
                        sh 'gpg1 --list-secret-keys'
                        sh 'gpg1 --list-keys'
                        sh 'gpg1 --version'
                        sh 'for fpr in $(gpg1 --list-keys --with-colons  | awk -F: \'/fpr:/ {print $10}\' | sort -u); do echo -e "5\ny\n" |  gpg1 --batch --command-fd 0 --expert --edit-key ${fpr} trust; done'
                        sh 'ls -l  /home/jenkins/.gnupg/'
                    }
                }
            }
        }
        stage('Prepare settings') {
            steps{
                container('java-builder') {
                    script {
                        keypleVersion = sh(script: 'grep version gradle.properties | cut -d= -f2 | tr -d "[:space:]"', returnStdout: true).trim()
                        echo "Building version ${keypleVersion}"
                        deploySnapshot = env.GIT_URL == "https://github.com/eclipse/${artifactId}.git" && env.GIT_BRANCH == "develop" && env.CHANGE_ID == null && keypleVersion ==~ /.*-SNAPSHOT$/
                        deployRelease = env.GIT_URL == "https://github.com/eclipse/${artifactId}.git" && (env.GIT_BRANCH == "master" || env.GIT_BRANCH.startsWith('release-')) && env.CHANGE_ID == null && keypleVersion ==~ /\d+\.\d+.\d+$/
                    }
                }
            }
        }
        stage('Keyple Java: Build and Test') {
            steps{
                container('java-builder') {
                    sh './gradlew installAll --info'
                    catchError(buildResult: 'UNSTABLE', message: 'There were failing tests.', stageResult: 'UNSTABLE') {
                        sh './gradlew check --info'
                    }
                    junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'

                    script {
                        keypleVersion = sh(script: 'grep version gradle.properties | cut -d= -f2 | tr -d "[:space:]"', returnStdout: true).trim()
                        echo "Building version ${keypleVersion}"
                    }
                }
            }
        }
        stage('Keyple Java: Tag/Push') {
            when {
                expression { deployRelease }
            }
            steps{
                container('java-builder') {
                    withCredentials([usernamePassword(credentialsId: 'github-bot', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh """
                            git config --global user.email "${PROJECT_NAME}-bot@eclipse.org"
                            git config --global user.name "${PROJECT_BOT_NAME}"
                            git tag '${keypleVersion}'
                            git push "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/eclipse/${artifactId}.git" refs/tags/${keypleVersion}
                        """
                    }
                }
            }
        }
        stage('Keyple Java: Code Quality') {
            when {
                expression { deploySnapshot || deployRelease }
            }
            steps {
                catchError(buildResult: 'SUCCESS', message: 'Unable to log code quality to Sonar.', stageResult: 'FAILURE') {
                    container('java-builder') {
                        withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_LOGIN')]) {
                            sh './gradlew --stop'
                            sh './gradlew codeQuality --info'
                            sh './gradlew --stop'
                        }
                    }
                }
            }
        }
        stage('Keyple Java: Upload artifacts to sonatype') {
            when {
                expression { deploySnapshot || deployRelease }
            }
            steps{
                container('java-builder') {
                    configFileProvider(
                        [configFile(fileId: 'gradle.properties',
                            targetLocation: '/home/jenkins/agent/gradle.properties')]) {
                        sh './gradlew :uploadArchives ${uploadParams}'
                        sh './gradlew --stop'
                    }
                }
            }
        }
        stage('Keyple Java: Prepare packaging') {
            when {
                expression { deploySnapshot || deployRelease }
            }
            steps {
                container('java-builder') {
                    sh "mkdir ./repository"
                    sh "cp ./build/libs/${artifactId}*.jar ./repository"
                    sh "ls -R ./repository"
                }
            }
        }
        stage('Keyple Java: Deploy packaging to eclipse snapshots') {
            when {
                expression { deploySnapshot }
            }
            steps {
                container('java-builder') {
                    sshagent(['projects-storage.eclipse.org-bot-ssh']) {
                        sh "ssh genie.keyple@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/keyple/snapshots"
                        sh "ssh genie.keyple@projects-storage.eclipse.org mkdir -p /home/data/httpd/download.eclipse.org/keyple/snapshots"
                        sh "scp -r ./repository/* genie.keyple@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/keyple/snapshots"
                    }
                }
            }
        }
        stage('Keyple Java: Deploy packaging to eclipse releases') {
            when {
                expression { deployRelease }
            }
            steps {
                container('java-builder') {
                    sshagent(['projects-storage.eclipse.org-bot-ssh']) {
                        sh "ssh genie.keyple@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/keyple/releases"
                        sh "ssh genie.keyple@projects-storage.eclipse.org mkdir -p /home/data/httpd/download.eclipse.org/keyple/releases"
                        sh "scp -r ./repository/* genie.keyple@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/keyple/releases"
                    }
                }
            }
        }
    }
    post {
        always {
            container('java-builder') {
                archiveArtifacts artifacts: 'build/reports/tests/**', allowEmptyArchive: true
            }
        }
    }
}
