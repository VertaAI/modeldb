#!/bin/bash

set -eo pipefail

mvn -Dtest=PullRequestSuite -Dsurefire.rerunFailingTestsCount=2 -B verify jacoco:report
