#!/bin/bash

set -eo pipefail

export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
if [ -z "$BRANCH_NAME" ]; then
    BRANCH_NAME="$(git rev-parse --abbrev-ref HEAD)"
fi
export VERSION_SUFFIX=$(echo $BRANCH_NAME | sed 's,/,-,g' | tr '[:upper:]' '[:lower:]')
export PROJECT_REVISION=$(mvn help:evaluate -Dexpression=revision -q -DforceStdout)

# require the revision to end in -SNAPSHOT
if [ "$PROJECT_REVISION" == "${PROJECT_REVISION/%-SNAPSHOT/}" ]; then
    echo The "revision" property in pom.xml must end with "-SNAPSHOT" for this script to work correctly.
    echo Actual value: revision = $PROJECT_REVISION
    exit 1
fi

# Insert branch name in project version
export PROJECT_VERSION=${PROJECT_REVISION/%-SNAPSHOT/-${VERSION_SUFFIX}-SNAPSHOT}

# When building verta main replace -SNAPSHOT with commit info
if [[ "$BRANCH_NAME" =~ ^(main|release/[^/]+)$ ]]; then
    COMMIT_INFO="$(TZ=UTC git show -s --format=%cd--%h --date='format-local:%Y-%m-%dT%H-%M-%S' --abbrev=7)"
    export PROJECT_VERSION=${PROJECT_VERSION/%-SNAPSHOT/-$COMMIT_INFO}
fi

LOCAL_MAVEN_SETTINGS_PARAM=""
if [ -f "$LOCAL_MAVEN_SETTINGS" ]; then
  LOCAL_MAVEN_SETTINGS_PARAM="-s $LOCAL_MAVEN_SETTINGS"
fi

export MAVEN_PARAMS="$LOCAL_MAVEN_SETTINGS_PARAM -DskipTests -Dmaven.compiler.showDeprecation=true -Djacoco.skip=true"
mvn -B versions:set -DnewVersion=$PROJECT_VERSION > /dev/null
mvn -B source:jar deploy $MAVEN_PARAMS || {
    if [ -z "$GITHUB_TOKEN" ]; then
        mvn -B versions:set -DnewVersion=$PROJECT_REVISION > /dev/null
    fi
    exit 1
}

# when running in CI leave revision for use by test step
if [ -z "$GITHUB_TOKEN" ]; then
    mvn -B versions:set -DnewVersion=$PROJECT_REVISION > /dev/null
fi