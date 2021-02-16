# TODO Application with Quarkus

This is an example application based on a Todo list where the different tasks are created, read, updated, or deleted from the database. Default this application for convenience purposes uses an in-memory database called H2 that allows you to run this application without depending on an external database being available. However, the H2 database is not supported to run in native mode, so before you do the native compilation, you will have to switch to the `postgresql`  branch. 

## Branches:
* `master` - use H2 (in memory), no native support
* `postgresql` - use posgresql, support native mode 

## Compile and run on a JVM

```bash
mvn package
java -jar target/todo-backend-1.0-SNAPSHOT-runner.jar
```

Then, open: http://localhost:8080/

## Development mode

```bash
mvn compile quarkus:dev
```
Then, open: http://localhost:8080/

## Compile to Native and run with PostgresSQL ( in a container )

Compile:
```bash
git checkout --track origin/postgresql
mvn clean package -Pnative
```
Run:
```bash
docker run --ulimit memlock=-1:-1 -it --rm=true --memory-swappiness=0 \
    --name postgres-quarkus-rest-http-crud \
    -e POSTGRES_USER=restcrud \
    -e POSTGRES_PASSWORD=restcrud \
    -e POSTGRES_DB=rest-crud \
    -p 5432:5432 postgres:10.5
target/todo-backend-*-runner
```
## Other links

- http://localhost:8080/health (Show the build in Health check for the datasource)
- http://localhost:8080/metrics (Show the default Metrics)
- http://localhost:8080/openapi (The OpenAPI Schema document in yaml format)
- http://localhost:8080/swagger-ui (The Swagger UI to test out the REST Endpoints)
- http://localhost:8080/graphql/schema.graphql (The GraphQL Schema document)
- http://localhost:8080/graphql-ui/ (The GraphiQL UI to test out the GraphQL Endpoint)
- http://localhost:8080/q/dev/ (Show dev ui)