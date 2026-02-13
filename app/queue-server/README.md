# Phoebus Queue Server Application

A JavaFX-based client for the [Bluesky QueueServer](https://blueskyproject.io/bluesky-queueserver/) that provides a graphical interface for managing experiment queues, monitoring status, and editing plans at beamlines and scientific facilities.

## Features

### Queue Management
- **View & Edit Queue**: See all queued plans with parameters, reorder items via drag-and-drop
- **Execute Plans**: Start, stop, pause, resume, and abort plan execution
- **Real-time Status**: Live updates via WebSocket connections showing queue state, running plans, and RE Manager status
- **History View**: Browse completed plans with execution results and metadata

### Plan Editing
- **Interactive Plan Editor**: Create and modify plans with type-safe parameter editing
- **Python Type Support**: Full support for Python types (strings, lists, dicts, booleans, numbers)
- **Schema Validation**: Parameters validated against plan schemas from the Queue Server
- **Live Preview**: See plan parameters as they will be sent to the server

### Plan Viewer
- **Plan Details**: View plan parameters, metadata, and execution results
- **Parameter Display**: All parameters shown with correct Python syntax and types
- **Copy to Queue**: Duplicate plans with one click

### Console Monitor
- **Live Console Output**: Real-time streaming of Queue Server console output
- **WebSocket Support**: Efficient streaming via WebSocket connections (fallback to HTTP polling)
- **Autoscroll**: Automatically scroll to latest output with toggle control

## Quick Start

### Prerequisites

- **Java 17** or later
- **Maven** (for building from source)
- **Bluesky Queue Server** running and accessible

### Starting a Local Queue Server

Use the provided Docker setup for local development:

```bash
cd services/bluesky-services
docker-compose --profile container-redis up -d
```

This starts:
- Bluesky Queue Server (RE Manager) on ports 60615/60625
- HTTP Server REST API on port 60610
- Redis database on port 6380

For details, see [services/bluesky-services/README.md](../../services/bluesky-services/README.md)

### Configuration

Set environment variables to connect to your Queue Server:

```bash
# Queue Server HTTP address (default: http://localhost:60610)
export QSERVER_HTTP_SERVER_URI=http://localhost:60610

# API Key authentication
export QSERVER_HTTP_SERVER_API_KEY=a

# Or use a file containing the API key
export QSERVER_HTTP_SERVER_API_KEYFILE=~/.phoebus/qserver_api_key.txt
```

### Building & Running

#### As Part of Phoebus

```bash
# From phoebus root directory
mvn clean install -DskipTests
cd phoebus-product/target
./phoebus
```

Then open **Applications → Queue Server** from the menu.

#### Standalone Build

```bash
# From queue-server directory
cd app/queue-server
mvn clean install
```

## Configuration Options

Configuration via **Edit → Preferences → Queue Server** or environment variables:

| Preference        | Environment Variable              | Default                  | Description                           |
|-------------------|-----------------------------------|--------------------------|---------------------------------------|
| `queue_server_url`| `QSERVER_HTTP_SERVER_URI`         | `http://localhost:60610` | Queue Server HTTP address             |
| `api_key`         | `QSERVER_HTTP_SERVER_API_KEY`     | *(none)*                 | API key for authentication            |
| `api_key_file`    | `QSERVER_HTTP_SERVER_API_KEYFILE` | *(none)*                 | Path to file containing API key       |
| `use_websockets`  | *(none)*                          | `true`                   | Use WebSockets for streaming data     |
| `connectTimeout`  | *(none)*                          | `5000`                   | HTTP connection timeout (ms)          |
| `debug`           | *(none)*                          | `false`                  | Enable HTTP request/response logging  |

## Contributing

When making changes:

1. Ensure proper Python type handling in parameter editor
2. Test with both WebSocket and HTTP fallback modes
3. Verify API key authentication works
4. Update documentation for new features
5. Follow existing code style and patterns