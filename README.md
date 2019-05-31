# TODO Application with Quarkus


Branches:

* `master` - use H2 (in memory), no native support
* `postgresql` - use posgresql, support native mode 

## Compilation

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

## Postgres - Run database

Run:

```bash
docker run --ulimit memlock=-1:-1 -it --rm=true --memory-swappiness=0 \
    --name postgres-quarkus-rest-http-crud -e POSTGRES_USER=restcrud \
    -e POSTGRES_PASSWORD=restcrud -e POSTGRES_DB=rest-crud \
    -p 5432:5432 postgres:10.5
```



