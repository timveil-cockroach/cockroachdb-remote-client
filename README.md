# CockroachDB Remote Client

[![CI Build and Test](https://github.com/timveil-cockroach/cockroachdb-remote-client/actions/workflows/ci.yml/badge.svg)](https://github.com/timveil-cockroach/cockroachdb-remote-client/actions/workflows/ci.yml)
[![Release Docker Image](https://github.com/timveil-cockroach/cockroachdb-remote-client/actions/workflows/release.yml/badge.svg)](https://github.com/timveil-cockroach/cockroachdb-remote-client/actions/workflows/release.yml)
[![Docker Pulls](https://img.shields.io/docker/pulls/timveil/cockroachdb-remote-client)](https://hub.docker.com/repository/docker/timveil/cockroachdb-remote-client)

Docker image used to perform simple tasks against a CockroachDB cluster then disappear.  For example, this can be useful for creating a database or setting configuration parameters, especially when used in Kubernetes or Docker Compose.  The following `docker-compose.yml` snippet highlights how it may be used, specifically the `crdb-init` service. 

```yaml
services:

  crdb-0:
    ...

  crdb-1:
    ...

  crdb-2:
    ...

  lb:
    container_name: lb
    hostname: lb
    image: timveil/dynamic-haproxy:latest
    ports:
      - "26257:26257"
      - "8080:8080"
      - "8081:8081"
    environment:
      - NODES=crdb-0 crdb-1 crdb-2
    depends_on:
      - crdb-0
      - crdb-1
      - crdb-2

  crdb-init:
    container_name: crdb-init
    hostname: crdb-init
    image: timveil/cockroachdb-remote-client:latest
    environment:
      - COCKROACH_HOST=crdb-0:26257
      - COCKROACH_INSECURE=true
      - DATABASE_NAME=test
    depends_on:
      - lb
```

The following `environment` variables are supported.  See https://www.cockroachlabs.com/docs/stable/use-the-built-in-sql-client.html#client-connection for more details.
* `COCKROACH_HOST` - __Required__. CockroachDB host and port number to connect to `<host>:<port>`.  If port not included you must specify `COCKROACH_PORT`.
* `COCKROACH_USER` - CockroachDB user that will own the remote client session.
* `COCKROACH_PORT` - CockroachDB port if not specified by `COCKROACH_HOST`.
* `COCKROACH_INSECURE` - Use an insecure connection.  Value must be `true` or `false`.
* `COCKROACH_CERTS_DIR` - The path to the certificate directory containing the CA and client certificates and client key.
* `DATABASE_NAME` - Name of database to create.
* `DATABASE_USER` - Name of new database user to create.  __Important__, this user will be created as a CockroachDB `admin`.
* `DATABASE_PASSWORD` - Password for `DATABASE_USER`.  If not provided the password will be set to `NULL` preventing the user from using [password based authentication](https://www.cockroachlabs.com/docs/v20.2/create-user.html#prevent-a-user-from-using-password-authentication).
* `COCKROACH_ORG` - The value of the `cluster.organization` setting.
* `COCKROACH_LICENSE_KEY` - The value of the `enterprise.license` setting.
* `COCKROACH_INIT` - Initializes the CockroachDB cluster with the `cockroach init` command.

## Building the Image
```bash
docker build --no-cache --platform linux/arm64 -t timveil/cockroachdb-remote-client:latest .
```

## Publishing the Image
```bash
docker push timveil/cockroachdb-remote-client:latest
```

## Building and Publishing the Image with Docker Build Cloud
```bash
docker buildx build --builder cloud-timveil-cloudy \
  --platform linux/amd64,linux/arm64 \
  --tag timveil/cockroachdb-remote-client:latest \
  --push .
```

## Running the Image
```bash
docker run -it timveil/cockroachdb-remote-client:latest
```

running the image with environment variables
```bash
docker run \
    --env COCKROACH_HOST=localhost:26257 \
    --env COCKROACH_INSECURE=true \
    --env DATABASE_NAME=test \
    --env COCKROACH_INIT=true \
    -it timveil/cockroachdb-remote-client:latest
```