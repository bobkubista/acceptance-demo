// Example Jenkinsfile for an app repo built by the kvm-server platform (ADR-0015/0016/0028).
// A push reaches Jenkins as a GitHub webhook (ADR-0025); the daily scan is a safety net.
//
// Conventions the platform relies on:
//   - ONE agent pod per pipeline (Jenkins runs one agent pod at a time).
//   - Image name: registry.internal.bobkubista.com/<repo name, lower-case, [a-z0-9-]>
//     (or the names listed in k8s/images for multi-image repos).
//   - Every green build of the default branch (BRANCH_IS_PRIMARY, set by Jenkins' GitHub
//     branch source) is a release (ADR-0028): version <major>.<minor>.<build number>,
//     major.minor from the pom (raise them by hand). The image <version> is pushed first, then
//     the Git tag v<version>; Argo CD Image Updater deploys the newest release.
//   - Branches build and test, but push nothing and deploy nothing.
//   - Test results (junit) and JaCoCo coverage show on every build. Maven resolves through Nexus
//     maven-public: the pod template 'buildkit' mounts the settings and sets MAVEN_ARGS (k8s/jenkins.yml),
//     so do not repeat -s or a mirror here.
pipeline {
  agent {
    kubernetes {
      inheritFrom 'buildkit'          // shared pod template: BuildKit rootless + registry credentials
      // merge, not the default override: otherwise the containers below replace the template's
      // and container('buildkit') is missing ("container buildkit not found in pod")
      yamlMergeStrategy merge()
      defaultContainer 'maven'
      yaml '''
        spec:
          containers:
            - name: maven
              image: maven:3.9.16-eclipse-temurin-25
              command: ["cat"]
              tty: true
              resources:
                requests: { memory: 768Mi, cpu: 250m }
                limits: { memory: 1Gi }
      '''
    }
  }
  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timeout(time: 45, unit: 'MINUTES')
  }
  environment {
    REPO  = "${env.JOB_NAME.split('/')[1].toLowerCase().replaceAll('[^a-z0-9-]+', '-')}"
    IMAGE = "registry.internal.bobkubista.com/${REPO}"
  }
  stages {
    stage('Build and test') {
      when { not { expression { env.BRANCH_IS_PRIMARY == 'true' } } }
      steps {
        sh 'mvn -B -ntp verify'
      }
    }
    // SonarQube Community has no branch analysis: only master is analysed
    stage('Build, test, analyse') {
      when { expression { env.BRANCH_IS_PRIMARY == 'true' } }
      steps {
        withSonarQubeEnv('SonarQube') {
          // the checkout belongs to the jnlp user; let git (Sonar blame) read it as root
          sh 'git config --global --add safe.directory "$WORKSPACE"'
          sh 'mvn -B -ntp verify sonar:sonar'
        }
      }
    }
    stage('Quality gate') {
      when { expression { env.BRANCH_IS_PRIMARY == 'true' } }
      steps {
        timeout(time: 10, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true   // SonarQube calls back via its webhook
        }
      }
    }
    stage('Version') {
      when { expression { env.BRANCH_IS_PRIMARY == 'true' } }
      steps {
        // 1.4.0-SNAPSHOT in the pom + build 57 -> 1.4.57
        sh '''
          base=$(mvn -B -ntp -q help:evaluate -Dexpression=project.version -DforceStdout)
          echo "$base" | grep -oE '^[0-9]+\\.[0-9]+' | sed "s/$/.${BUILD_NUMBER}/" > version.txt
          grep -qE '^[0-9]+\\.[0-9]+\\.[0-9]+$' version.txt || { echo "pom version '$base' has no major.minor"; exit 1; }
        '''
        script { env.VERSION = readFile('version.txt').trim() }
        echo "Release ${env.VERSION}"
      }
    }
    stage('Image') {
      steps {
        container('buildkit') {
          // master: push <version>; branches: build only (proves the Dockerfile), push nothing
          sh '''
            if [ "$BRANCH_IS_PRIMARY" = true ]; then out="name=${IMAGE}:${VERSION},push=true"
            else out="name=${IMAGE}:branch,push=false"; fi
            # the Dockerfile may use ARG APP_VERSION to tell the app which release it is
            buildctl-daemonless.sh build \
              --frontend dockerfile.v0 \
              --local context=. \
              --local dockerfile=. \
              --opt build-arg:APP_VERSION="${VERSION:-branch}" \
              --output type=image,$out
          '''
        }
      }
    }
    // The tag comes after the image: a tag always points at an image that exists. A failing
    // tag makes the build red. A tag creates no commit, so it triggers no new build.
    stage('Release') {
      when { expression { env.BRANCH_IS_PRIMARY == 'true' } }
      steps {
        withCredentials([string(credentialsId: 'github-release-token', variable: 'GH_TOKEN')]) {
          sh '''
            set +x   # Jenkins runs sh -x: never echo the commands that handle the token
            umask 077
            h=$(mktemp); trap 'rm -f "$h" tag.json' EXIT
            printf 'Authorization: Bearer %s\\n' "$GH_TOKEN" > "$h"
            slug=$(git config --get remote.origin.url | sed -E 's#^(https://github.com/|git@github.com:)##; s#\\.git$##')
            echo "$slug" | grep -qE '^[A-Za-z0-9._-]+/[A-Za-z0-9._-]+$' || { echo "unexpected repo '$slug'"; exit 1; }
            body=$(printf '{"ref": "refs/tags/v%s", "sha": "%s"}' "$VERSION" "$(git rev-parse HEAD)")
            code=$(curl -s -o tag.json -w '%{http_code}' -X POST -H @"$h" \
              -H 'Accept: application/vnd.github+json' -d "$body" "https://api.github.com/repos/$slug/git/refs")
            if [ "$code" != 201 ]; then echo "tag v$VERSION failed ($code):"; cat tag.json; exit 1; fi
            echo "tagged $slug v$VERSION"
          '''
        }
      }
    }
  }
  post {
    always {
      junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml'
      // no source copy on the controller (disk); the coverage numbers and file list are kept
      recordCoverage(tools: [[parser: 'JACOCO']], sourceCodeRetention: 'NEVER')
    }
  }
}
