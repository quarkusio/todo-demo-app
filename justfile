# Handy sets of commands to run side operations usually described in the doc otherwise
# expects podman and quarkus CLI

postgres := "postgres:15-bullseye"

# Start the database using podman
start-infra:
    podman run --ulimit memlock=-1:-1 -it --rm=true \
        --name postgres-quarkus-rest-http-crud \
        -e POSTGRES_USER=restcrud \
        -e POSTGRES_PASSWORD=restcrud \
        -e POSTGRES_DB=rest-crud \
        -p 5432:5432 {{postgres}}

# Stop the database using podman
stop-infra:
    podman stop $(podman ps -q --filter ancestor={{postgres}})

#Using quarkus CLI, build in native
native:
    quarkus build --no-tests --native

