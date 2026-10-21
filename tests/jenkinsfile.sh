#!/usr/bin/env bash
# Test: the Jenkinsfile publishes test results and coverage (kvm-server ADR-0028, D2 of the
# jenkins-junit-maven-mirror handoff). Rollout: merge only AFTER kvm-server's Jenkins (junit and
# coverage plugins, ADR-0032) is deployed and its acceptance run is green. Static, no cluster. Run from anywhere: tests/jenkinsfile.sh [Jenkinsfile]
set -euo pipefail
f="${1:-$(dirname "$0")/../Jenkinsfile}"
FAILED=0
check() { if grep -Pzoq "$2" "$f"; then echo "PASS  $1"; else echo "FAIL  $1"; FAILED=1; fi; }
check "post { always { junit allowEmptyResults: true ... }" 'post\s*\{\s*always\s*\{\s*junit\s+allowEmptyResults:\s*true'
check "junit reads surefire and failsafe reports" "testResults: '\*\*/target/surefire-reports/\*\.xml, \*\*/target/failsafe-reports/\*\.xml'"
check "recordCoverage JACOCO in post always, sourceCodeRetention NEVER" "post\\s*\\{\\s*always\\s*\\{(.|\\n)*recordCoverage\\(tools: \\[\\[parser: 'JACOCO'\\]\\], sourceCodeRetention: 'NEVER'\\)"
if [[ $(grep -c 'recordCoverage(' "$f") == 1 ]]; then echo "PASS  recordCoverage once"; else echo "FAIL  recordCoverage once"; FAILED=1; fi
if grep -q 'repo.maven.apache.org\|settings.xml' "$f"; then echo "FAIL  mirror is set by the pod template, not here"; FAILED=1; else echo "PASS  Jenkinsfile does not set a Maven mirror"; fi
[[ $FAILED == 0 ]] && echo "TEST PASSED" || echo "TEST FAILED"
exit $FAILED
