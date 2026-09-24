// ============================================================
// E-Voting Platform — CI/CD Pipeline (Jenkinsfile)
// ============================================================
pipeline {
    agent any

    environment {
        MAVEN_HOME = tool 'Maven-3.9'
        JAVA_HOME  = tool 'JDK-21'
        PATH       = "${MAVEN_HOME}/bin:${JAVA_HOME}/bin:${env.PATH}"
        IMAGE_NAME = 'evoting-platform'
        // Secrets injected from Jenkins credential store
        SUPABASE_DB_URL      = credentials('supabase-db-url')
        SUPABASE_DB_USER     = credentials('supabase-db-user')
        SUPABASE_DB_PASSWORD = credentials('supabase-db-password')
        JWT_SECRET           = credentials('evoting-jwt-secret')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Building branch: ${env.BRANCH_NAME} — commit: ${env.GIT_COMMIT?.take(8)}"
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile -f evoting-platform/pom.xml -B -q'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test -f evoting-platform/pom.xml -B'
            }
            post {
                always {
                    junit 'evoting-platform/**/target/surefire-reports/*.xml'
                    jacoco(
                        execPattern: 'evoting-platform/**/target/jacoco.exec',
                        classPattern: 'evoting-platform/**/target/classes',
                        sourcePattern: 'evoting-platform/**/src/main/java'
                    )
                }
            }
        }

        stage('Static Analysis') {
            steps {
                sh 'mvn spotbugs:check -f evoting-platform/pom.xml -B -q || true'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -f evoting-platform/pom.xml -DskipTests -B -q'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'evoting-platform/**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                sh "docker build -t ${IMAGE_NAME}:${env.BUILD_NUMBER} -f evoting-platform/backend/evoting-web/Dockerfile ."
                sh "docker tag ${IMAGE_NAME}:${env.BUILD_NUMBER} ${IMAGE_NAME}:latest"
            }
        }
    }

    post {
        failure {
            echo 'Build failed! Notify team.'
        }
        success {
            echo 'Build successful!'
        }
        always {
            cleanWs()
        }
    }
}
