# acceptance-demo

The smallest app that exercises the kvm-server delivery loop (ADR-0028 in
[kvm-server](https://github.com/bobkubista/kvm-server)): a Java 25 HTTP server with a Hello world
page on `/` (public on https://bobkubista.com, ADR-0030 in kvm-server), `acceptance-demo <release>`
on `/version`, and `UP` on `/actuator/health/readiness` and `/liveness` (not public).

## How it is delivered

1. A push reaches Jenkins (GitHub webhook). Every branch builds and tests (`mvn verify`); Jenkins shows the JUnit
   results and the JaCoCo coverage of every build, and Maven dependencies come from the Nexus mirror.
2. On `master` a green build (tests, SonarQube quality gate) is a release
   `<major>.<minor>.<build>`: major.minor from the pom (`1.0.0-SNAPSHOT` gives `1.0.<build>`).
   The image `registry.internal.bobkubista.com/acceptance-demo:<release>` is pushed first,
   then the Git tag `v<release>`.
3. Argo CD runs `k8s/` in namespace `apps` as `acceptance-demo-api` / `acceptance-demo-app`;
   Image Updater moves it to the newest release within a few minutes.
4. Roll back: `./lab.sh rollback acceptance-demo <release>` in kvm-server.

`acceptance/namespace/` and `acceptance/foreign-namespace/` are deliberately forbidden manifests for kvm-server's acceptance test
(an app may not create a Namespace, nor objects in another namespace). Nothing deploys them on purpose.

## Build locally

```bash
mvn verify                                   # tests + JaCoCo coverage (fails below 80% lines/branches)
docker build -t acceptance-demo --build-arg APP_VERSION=dev . && docker run -p 8080:8080 acceptance-demo
curl localhost:8080/                           # Hello world page
curl localhost:8080/version                    # acceptance-demo dev
```
