# acceptance-demo — Context Prompt

Last updated: 2026-10-19

- Purpose: test app for the kvm-server platform's delivery loop (one namespace `apps`, every
  green master build a release, rollback with `lab.sh`; ADR-0028 in kvm-server). It is not a
  product; keep it minimal.
- Stack: Java 25, JDK `com.sun.net.httpserver` (no framework), JUnit 6, JaCoCo, Maven; image
  from `eclipse-temurin:25-jre` (pinned by digest), runs as uid 65532.
- Code: `src/main/java/demo/App.java` (routes `/`, `/actuator/health/{readiness,liveness}`),
  test `src/test/java/demo/AppTest.java` (starts the server on a free port).
- Delivery: `Jenkinsfile` is kvm-server's `docs/examples/app-repo/Jenkinsfile` (Version and
  Release stages on master; branches push nothing). `k8s/` is the template's Deployment
  (`app`) and Service (`api`), no Ingress; the release tag is set by Image Updater.
- `acceptance/namespace/`: a Namespace manifest the platform must refuse (used by kvm-server's
  `tests/acceptance/apps-namespace-releases.sh`).
- Rules: test first; never commit on master without the owner's OK; the owner merges.
