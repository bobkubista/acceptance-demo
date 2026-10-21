#!/usr/bin/env bash
# Test: the Jenkinsfile publishes test results and coverage (kvm-server ADR-0028, D2 of the
# jenkins-junit-maven-mirror handoff). Static, no cluster. Run from anywhere: tests/jenkinsfile.sh [Jenkinsfile]
set -euo pipefail
f="${1:-$(dirname "$0")/../Jenkinsfile}"
FAILED=0
check() { if grep -Pzoq "$2" "$f"; then echo "PASS  $1"; else echo "FAIL  $1"; FAILED=1; fi; }
check "post { always { junit allowEmptyResults: true ... }" 'post\s*\{\s*always\s*\{\s*junit\s+allowEmptyResults:\s*true'
check "junit reads surefire and failsafe reports" "testResults: '\*\*/target/surefire-reports/\*\.xml, \*\*/target/failsafe-reports/\*\.xml'"
check "recordCoverage JACOCO after the Maven build (both stages)" "mvn -B -ntp verify'\s*recordCoverage\(tools: \[\[parser: 'JACOCO'\]\]\)(.|\n)*sonar:sonar'\s*\}\s*recordCoverage\(tools: \[\[parser: 'JACOCO'\]\]\)"
if grep -q 'repo.maven.apache.org\|settings.xml' "$f"; then echo "FAIL  mirror is set by the pod template, not here"; FAILED=1; else echo "PASS  Jenkinsfile does not set a Maven mirror"; fi
[[ $FAILED == 0 ]] && echo "TEST PASSED" || echo "TEST FAILED"
exit $FAILED
