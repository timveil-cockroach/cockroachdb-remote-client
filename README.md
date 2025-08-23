# CockroachDB Remote Client

[![CI Build and Test](https://github.com/timveil-cockroach/cockroachdb-remote-client/actions/workflows/ci.yml/badge.svg)](https://github.com/timveil-cockroach/cockroachdb-remote-client/actions/workflows/ci.yml)
[![Release Docker Image](https://github.com/timveil-cockroach/cockroachdb-remote-client/actions/workflows/release.yml/badge.svg)](https://github.com/timveil-cockroach/cockroachdb-remote-client/actions/workflows/release.yml)
[![Docker Pulls](https://img.shields.io/docker/pulls/timveil/cockroachdb-remote-client)](https://hub.docker.com/repository/docker/timveil/cockroachdb-remote-client)

A lightweight Docker container for performing one-time administrative tasks against CockroachDB clusters. This Spring Boot application connects to a CockroachDB cluster, executes initialization tasks, and exits - perfect for use in Kubernetes init containers, Docker Compose services, or CI/CD pipelines.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Quick Start](#quick-start)
- [Environment Variables](#environment-variables)
- [Usage Examples](#usage-examples)
- [Docker Operations](#docker-operations)
- [Common Use Cases](#common-use-cases)
- [Troubleshooting](#troubleshooting)
- [Development](#development)

## Overview

The CockroachDB Remote Client is designed to solve the common problem of initializing CockroachDB clusters in containerized environments. Instead of running initialization scripts directly on database nodes, this container can:

- Initialize a new CockroachDB cluster
- Create databases and users
- Set enterprise licenses and organization settings
- Configure cluster-wide settings
- Run as a one-time task and exit cleanly

## Key Features

- **Zero Configuration**: Works with environment variables only
- **Multi-Platform**: Supports both AMD64 and ARM64 architectures  
- **Security First**: Supports both secure (TLS) and insecure connections
- **Enterprise Ready**: Built-in support for CockroachDB Enterprise features
- **Kubernetes Native**: Perfect for init containers and Jobs
- **Lightweight**: Based on Spring Boot with minimal dependencies

## Quick Start

The simplest way to use this container is with Docker:

```bash
docker run \
    --env COCKROACH_HOST=localhost:26257 \
    --env COCKROACH_INSECURE=true \
    --env DATABASE_NAME=myapp \
    --env COCKROACH_INIT=true \
    timveil/cockroachdb-remote-client:latest
```

This will:
1. Initialize the CockroachDB cluster
2. Create a database named `myapp`
3. Exit successfully

## Environment Variables

### Connection Settings

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `COCKROACH_HOST` | **Yes** | - | CockroachDB host and port (`<host>:<port>`) |
| `COCKROACH_USER` | No | `root` | Username for the session (not used in commands) |
| `COCKROACH_PORT` | No | - | Port if not specified in `COCKROACH_HOST` |
| `COCKROACH_INSECURE` | No | `false` | Use insecure connection (`true`/`false`) |
| `COCKROACH_CERTS_DIR` | No | - | Path to certificate directory for TLS |

### Database Operations

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_NAME` | No | - | Database to create |
| `DATABASE_USER` | No | - | Database user to create (granted admin privileges) |
| `DATABASE_PASSWORD` | No | - | Password for `DATABASE_USER` (NULL if not provided) |

### Enterprise Features

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `COCKROACH_ORG` | No | - | Sets `cluster.organization` setting |
| `COCKROACH_LICENSE_KEY` | No | - | Sets `enterprise.license` setting |

### Cluster Management

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `COCKROACH_INIT` | No | `false` | Initialize cluster with `cockroach init` |

## Usage Examples

### Docker Compose with CockroachDB Cluster

```yaml
services:
  crdb-0:
    image: cockroachdb/cockroach:latest
    command: start --insecure --join=crdb-0,crdb-1,crdb-2
    ports:
      - "26257:26257"
      - "8080:8080"

  crdb-1:
    image: cockroachdb/cockroach:latest
    command: start --insecure --join=crdb-0,crdb-1,crdb-2

  crdb-2:
    image: cockroachdb/cockroach:latest
    command: start --insecure --join=crdb-0,crdb-1,crdb-2

  crdb-init:
    image: timveil/cockroachdb-remote-client:latest
    environment:
      - COCKROACH_HOST=crdb-0:26257
      - COCKROACH_INSECURE=true
      - COCKROACH_INIT=true
      - DATABASE_NAME=myapp
      - DATABASE_USER=appuser
      - DATABASE_PASSWORD=secretpassword
    depends_on:
      - crdb-0
      - crdb-1  
      - crdb-2
```

### Kubernetes Init Container

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  template:
    spec:
      initContainers:
      - name: db-init
        image: timveil/cockroachdb-remote-client:latest
        env:
        - name: COCKROACH_HOST
          value: "cockroachdb:26257"
        - name: COCKROACH_INSECURE
          value: "false"
        - name: COCKROACH_CERTS_DIR
          value: "/certs"
        - name: DATABASE_NAME
          value: "myapp"
        - name: DATABASE_USER
          value: "appuser"
        volumeMounts:
        - name: certs
          mountPath: /certs
      containers:
      - name: myapp
        image: myapp:latest
```

### Enterprise License Setup

```bash
docker run \
    --env COCKROACH_HOST=prod-cluster:26257 \
    --env COCKROACH_CERTS_DIR=/certs \
    --env COCKROACH_ORG="My Company" \
    --env COCKROACH_LICENSE_KEY="your-enterprise-license-key" \
    --volume /path/to/certs:/certs:ro \
    timveil/cockroachdb-remote-client:latest
```

### Secure Connection with TLS

```bash
docker run \
    --env COCKROACH_HOST=secure-cluster:26257 \
    --env COCKROACH_CERTS_DIR=/certs \
    --env DATABASE_NAME=production \
    --volume /path/to/cockroach-certs:/certs:ro \
    timveil/cockroachdb-remote-client:latest
```

## Docker Operations

### Pull the Image

```bash
docker pull timveil/cockroachdb-remote-client:latest
```

### Build Locally

```bash
# Single platform
docker build -t cockroachdb-remote-client:local .

# Multi-platform build
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag timveil/cockroachdb-remote-client:latest \
  .
```

### Run with Custom Configuration

```bash
docker run \
    --env COCKROACH_HOST=my-cluster:26257 \
    --env COCKROACH_INSECURE=true \
    --env DATABASE_NAME=testdb \
    --rm \
    timveil/cockroachdb-remote-client:latest
```

## Common Use Cases

### 1. CI/CD Pipeline Database Setup
Use in your CI/CD pipeline to set up test databases:

```yaml
# GitHub Actions example
- name: Initialize Test Database
  run: |
    docker run --network host \
      --env COCKROACH_HOST=localhost:26257 \
      --env COCKROACH_INSECURE=true \
      --env DATABASE_NAME=test_${{ github.run_id }} \
      timveil/cockroachdb-remote-client:latest
```

### 2. Kubernetes Job for Database Migration
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: db-setup
spec:
  template:
    spec:
      containers:
      - name: db-init
        image: timveil/cockroachdb-remote-client:latest
        env:
        - name: COCKROACH_HOST
          valueFrom:
            secretKeyRef:
              name: db-config
              key: host
        - name: DATABASE_NAME
          value: "production"
      restartPolicy: Never
```

### 3. Local Development Environment
```bash
# Start CockroachDB
docker run -d --name cockroach -p 26257:26257 -p 8080:8080 \
  cockroachdb/cockroach:latest start-single-node --insecure

# Initialize with development data
docker run --network container:cockroach \
  --env COCKROACH_HOST=localhost:26257 \
  --env COCKROACH_INSECURE=true \
  --env COCKROACH_INIT=true \
  --env DATABASE_NAME=development \
  --env DATABASE_USER=devuser \
  --env DATABASE_PASSWORD=devpass \
  timveil/cockroachdb-remote-client:latest
```

## Troubleshooting

### Connection Issues

**Problem**: `connection refused` errors
```
Solution: Ensure CockroachDB is running and accessible at the specified host:port
Check: docker ps, network connectivity, firewall rules
```

**Problem**: TLS/certificate errors  
```
Solution: Verify COCKROACH_CERTS_DIR contains valid certificates
Required files: ca.crt, client.root.crt, client.root.key
```

### Authentication Issues

**Problem**: `authentication failed` errors
```
Solution: Check username and certificate validity
For insecure: ensure COCKROACH_INSECURE=true
For secure: verify client certificates match the user
```

### Environment Variable Issues

**Problem**: Variables not being recognized
```
Solution: Ensure exact spelling and case sensitivity
Example: COCKROACH_HOST (not cockroach_host)
Check: Use 'true'/'false' strings, not booleans
```

### Debugging

Enable debug output by checking container logs:
```bash
docker run --name debug-run \
  --env COCKROACH_HOST=localhost:26257 \
  --env COCKROACH_INSECURE=true \
  timveil/cockroachdb-remote-client:latest

# Check logs
docker logs debug-run
```

## Development

### Prerequisites
- Java 21+
- Maven 3.6+
- Docker

### Local Build and Test
```bash
# Compile and package
./mvnw clean package

# Build Docker image
docker build -t cockroachdb-remote-client:dev .

# Test locally
docker run --rm \
  --env COCKROACH_HOST=localhost:26257 \
  --env COCKROACH_INSECURE=true \
  cockroachdb-remote-client:dev
```

### Architecture
- **Language**: Java 21
- **Framework**: Spring Boot 3.5.4
- **Build Tool**: Maven
- **Base Image**: cockroachdb/cockroach (for CockroachDB binary)
- **Runtime**: ApplicationRunner pattern for one-time execution

---

For more information about CockroachDB client connections, see the [official documentation](https://www.cockroachlabs.com/docs/stable/use-the-built-in-sql-client.html#client-connection).