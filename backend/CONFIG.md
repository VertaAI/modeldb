# mdb-v2-backend config

Configuration Properties description for ModelDB version 2

## gRPC Server ***(Mandatory)***

```yaml
grpcServer:
  port: 8085
```

1. modeldb-backend gRPC server port

## Spring Server ***(Mandatory)***

```yaml
springServer:
  port: 8086
  shutdownTimeout: 30
```

1. spring server used for the Artifact server and metrics purpose
2. `port:` Artifact server port
3. `shutdownTimeout:` Artifact server gracefully shutdown time in second

## AuthService Properties ***(Optional)***

- AuthService is the authentication service that provides access control on top of the modeldb-backend.
- you can use your AuthService by implementing the protos in the [UAC package](../protos/protos/public/uac/UACService.proto).

```yaml
authService:
  host: uacservice
  port: 50051
```

1. `host` is the location of your authentication server.
2. `port` authentication service port

### Artifact Store Config ***(Mandatory)***

```yaml
artifactStoreConfig:
  artifactStoreType: NFS #S3, GCP, NFS
  S3:
    cloudAccessKey: #Set S3 accessKey, like access
    cloudSecretKey: #Set S3 secretKey, like secret
    cloudBucketName: modeldb_artifacts # Note: bucket needs to exist already
  NFS:
    nfsServerHost: localhost
    nfsUrlProtocol: https
    nfsRootPath: /Users/mvartak/Projects/ArtifactStore/test_dir/
    artifactEndpoint:
      getArtifact: "/v1/artifact1/getArtifact"
      storeArtifact: "/v1/artifact/storeArtifact"
```

1. `artifactStoreType` define your prefer artifact store type like NFS, S3.
1. If select `S3` then set appropriate `cloudAccessKey`, `cloudSecretKey` provide by amazon setup and `cloudBucketName`(**Note:** bucket needs to exist already) of amazon S3
1. If select `NFS` then set appropriate properties
    - `nfsServerHost` NFS server host where you have to connect default value is `localhost`
    - `nfsUrlProtocol` NFS server URL protocol if it secure then its value is `https` otherwise `http`
    - `nfsRootPath` is the root path of NFS server where you want to store all artifacts
    - `artifactEndpoint` define the artifact endpoints URLs which you will use for store & get artifacts

### Database Config ***(Mandatory)***

```yaml
database:
  DBType: relational
  timeout: 4
  liquibaseLockThreshold: 60 #time in second
  RdbConfiguration:
    RdbDatabaseName: modeldb
    RdbDriver: "org.postgresql.Driver"
    RdbDialect: "org.hibernate.dialect.PostgreSQLDialect"
    RdbUrl: "jdbc:postgresql://localhost:5432"
    RdbUsername: postgres
    RdbPassword: root
```

1. `DBType` provide the provision to configure different database like Relation DB, noSql DB. based on this type modeldb-backend initialize the database. (***Note:*** Currently modeldb-backend support only relational DB but you can extend it by writing your own database code)
1. `timeout`(**Optional**) the time in seconds to wait for the database operation used to validate the DB connection to complete. Default value is 4.
1. `liquibaseLockThreshold`(**Optional**) the time in second to check liquibase db lock threshold, if system found lock time difference greater then this threshold at system startup it will release the liquibase lock. Default value is 60
1. `RdbConfiguration` define the database relevant configuration above sample defined for the PostgreSQL DB.

    ***Note:***
    - Ensure the user mentioned in `RdbUsername` & `RdbPassword` has create privileges on the database mentioned in `RdbDatabaseName`.

### Test Database Config ***(Mandatory)***

```yaml
test:
  test-database:
    DBType: relational
    timeout: 4
    liquibaseLockThreshold: 60 #time in second
    RdbConfiguration:
      RdbDatabaseName: modeldb_test
      RdbDriver: "org.postgresql.Driver"
      RdbDialect: "org.hibernate.dialect.PostgreSQLDialect"
      RdbUrl: "jdbc:postgresql://localhost:5432"
      RdbUsername: postgres
      RdbPassword: root
  testUsers:
    primaryUser:
      email:
      devKey:
    secondaryUser:
      email:
      devKey:
```

1. `test-database` go with the same steps define on above section
1. `testUsers` if you are use the authService then `testUsers` properties are compulsory and provide to register test users details `email` and `devKey` which provide by UAC service.
