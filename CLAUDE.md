# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application that creates a Docker container for performing remote CockroachDB administrative tasks. The application is designed to run as a one-time task container that connects to a CockroachDB cluster, performs initialization tasks, and then exits.

## Key Architecture

- **Single Application Class**: `RemoteClientApplication.java` - Spring Boot ApplicationRunner that executes CockroachDB commands via ProcessBuilder
- **Environment-Driven Configuration**: All functionality controlled through environment variables
- **CockroachDB Binary Integration**: Docker image includes the cockroach binary for executing SQL commands
- **Multi-stage Docker Build**: Uses Maven for compilation and cockroachdb/cockroach image for binary

## Common Commands

### Build and Package
```bash
./mvnw clean package
```

### Docker Operations
```bash
# Build image
docker build --no-cache --platform linux/arm64 -t timveil/cockroachdb-remote-client:latest .

# Build for multiple platforms
docker buildx build --builder cloud-timveil-cloudy \
  --platform linux/amd64,linux/arm64 \
  --tag timveil/cockroachdb-remote-client:latest \
  --push .

# Run with environment variables
docker run \
    --env COCKROACH_HOST=localhost:26257 \
    --env COCKROACH_INSECURE=true \
    --env DATABASE_NAME=test \
    --env COCKROACH_INIT=true \
    -it timveil/cockroachdb-remote-client:latest
```

## Environment Variables

The application behavior is entirely controlled through environment variables:

### Required Variables
- `COCKROACH_HOST` - CockroachDB host and port (`<host>:<port>`)
- `COCKROACH_USER` - CockroachDB user for the session

### Connection Variables
- `COCKROACH_PORT` - Port if not specified in COCKROACH_HOST
- `COCKROACH_INSECURE` - Use insecure connection (true/false)
- `COCKROACH_CERTS_DIR` - Path to certificate directory

### Database Setup Variables
- `DATABASE_NAME` - Database to create
- `DATABASE_USER` - Database user to create (will be granted admin privileges)
- `DATABASE_PASSWORD` - Password for DATABASE_USER

### Enterprise Variables
- `COCKROACH_ORG` - Sets cluster.organization setting
- `COCKROACH_LICENSE_KEY` - Sets enterprise.license setting

### Initialization Variables
- `COCKROACH_INIT` - Initialize cluster with `cockroach init` command

## Application Flow

1. **Cluster Initialization** - If `COCKROACH_INIT=true`, runs `cockroach init`
2. **Remote Debugging** - Always enables `server.remote_debugging.mode = 'any'`
3. **Enterprise License** - Sets organization and license if provided
4. **Database Creation** - Creates database if `DATABASE_NAME` specified
5. **User Management** - Creates user and grants privileges if `DATABASE_USER` specified

## Development Notes

- No test files exist in this project
- Uses Spring Boot 3.5.4 with Java 21
- Minimal dependencies (only spring-boot-starter)
- Application runs once and exits (not a long-running service)
- All CockroachDB operations executed via ProcessBuilder calling `/cockroach` binary
- Error handling throws RuntimeException on non-zero exit codes