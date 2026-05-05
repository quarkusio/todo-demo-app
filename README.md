# TODO Application with Quarkus

This is an example application based on a Todo list where the different tasks are created, read, updated, or deleted from the database. This application uses `postgresql` as a database and that is provided with Quarkus Dev Services. When running in a 
non-dev mode you will have to provide the database yourself. 

## Development mode

```bash
mvn compile quarkus:dev
```
Then, open: http://localhost:8080/

## Compile and run on a JVM with PostgresSQL ( in a container )

```bash
mvn package
```
Run:
```bash
docker run --ulimit memlock=-1:-1 -it -d --rm=true \
    --name postgres-quarkus-rest-http-crud \
    -e POSTGRES_USER=restcrud \
    -e POSTGRES_PASSWORD=restcrud \
    -e POSTGRES_DB=rest-crud \
    -p 5432:5432 postgres:18
java -jar target/quarkus-app/quarkus-run.jar
```

Then, open: http://localhost:8080/

## Compile to Native and run with PostgresSQL ( in a container )

Compile:
```bash
mvn clean package -Pnative
```
Run:
```bash
docker run --ulimit memlock=-1:-1 -it -d --rm=true \
    --name postgres-quarkus-rest-http-crud \
    -e POSTGRES_USER=restcrud \
    -e POSTGRES_PASSWORD=restcrud \
    -e POSTGRES_DB=rest-crud \
    -p 5432:5432 postgres:18
./target/todo-backend-1.0-SNAPSHOT-runner
```
## Using Podman

If you use [Podman](https://podman.io/) instead of Docker, Dev Services requires the Podman socket to be active and the `DOCKER_HOST` environment variable to be set. A shell alias (`alias docker=podman`) is not sufficient because Quarkus Dev Services connects through the container socket API, not the CLI.

Start the Podman socket and set the required environment variables before running the application:

```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
```

Then start dev mode as usual:

```bash
mvn compile quarkus:dev
```

## Other links

- http://localhost:8080/q/health (Show the build in Health check for the datasource)
- http://localhost:8080/q/openapi (The OpenAPI Schema document in yaml format)
- http://localhost:8080/q/swagger-ui (The Swagger UI to test out the REST Endpoints)
- http://localhost:8080/graphql/schema.graphql (The GraphQL Schema document)
- http://localhost:8080/q/graphql-ui/ (The GraphiQL UI to test out the GraphQL Endpoint)
- http://localhost:8080/q/dev-ui/ (Show dev ui)
