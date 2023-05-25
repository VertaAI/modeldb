export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
export LIQUIBASE_MIGRATION=true
export RUN_LIQUIBASE_SEPARATE=false
export LOG4J_FORMAT_MSG_NO_LOOKUPS=true
export VERTA_MODELDB_TEST_CONFIG=itconfig/config-test-h2.yaml
#mvn -B verify -Dtest=PullRequestSuite -Dmaven.compiler.useIncrementalCompilation=false -Dsurefire.rerunFailingTestsCount=2
mvn -B verify -Dtest=PullRequestSuite -Dsurefire.rerunFailingTestsCount=2 jacoco:report
