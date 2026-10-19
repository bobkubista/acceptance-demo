# acceptance-demo

The smallest app that exercises the kvm-server delivery loop (ADR-0028 in
[kvm-server](https://github.com/bobkubista/kvm-server)): a Java 25 HTTP server that answers
`acceptance-demo <release>` on `/` and `UP` on `/actuator/health/readiness` and `/liveness`.

## How it is delivered

1. A push reaches Jenkins (GitHub webhook). Every branch builds and tests (`mvn verify`).
2. On `master` a green build (tests, SonarQube quality gate) is a release
   `<major>.<minor>.<build>`: major.minor from the pom (`1.0.0-SNAPSHOT` gives `1.0.<build>`).
   The image `registry.internal.bobkubista.com/acceptance-demo:<release>` is pushed first,
   then the Git tag `v<release>`.
3. Argo CD runs `k8s/` in namespace `apps` as `acceptance-demo-api` / `acceptance-demo-app`;
   Image Updater moves it to the newest release within a few minutes.
4. Roll back: `./lab.sh rollback acceptance-demo <release>` in kvm-server.

`acceptance/namespace/` is a deliberately forbidden manifest for kvm-server's acceptance test
(an app may not create a Namespace). Nothing deploys it on purpose.

## Build locally

```bash
mvn verify                                   # tests + JaCoCo coverage
docker build -t acceptance-demo --build-arg APP_VERSION=dev . && docker run -p 8080:8080 acceptance-demo
curl localhost:8080/
```
