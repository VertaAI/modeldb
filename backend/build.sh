set -e
mvn clean
mvn package -Dmaven.test.skip=true
docker build --no-cache -t vertaaiofficial/modeldb-backend:latest -f dockerfile --rm .
