# Bluesky Services Docker Setup

This directory contains Docker configurations for running Bluesky Queue Server and HTTP Server for the Phoebus queue-server application.

## Services

- **queue-server**: Bluesky Queue Server (RE Manager) - manages the execution queue
- **http-server**: Bluesky HTTP Server - provides REST API interface for the Java client  
- **redis**: Redis database for queue persistence

## Quick Start

### Option 1: Use Host Redis (if you have Redis running)
```bash
cd services/bluesky-services
docker-compose up -d queue-server http-server
```

### Option 2: Use Container Redis (recommended)
```bash
cd services/bluesky-services  
docker-compose --profile container-redis up -d
```

### Common Commands
1. **Check service status:**
   ```bash
   docker-compose ps
   ```

2. **View logs:**
   ```bash
   docker-compose logs -f
   # Or for specific service:
   docker-compose logs -f queue-server
   ```

3. **Stop services:**
   ```bash
   docker-compose down
   ```

4. **Rebuild after changes:**
   ```bash
   docker-compose build
   ```

## Accessing Services

- **HTTP Server API**: http://localhost:60610
- **Queue Server ZMQ**: tcp://localhost:60615  
- **Redis**: localhost:6380 (when using container Redis)

## API Authentication

The HTTP server uses API key `a` by default. Test the connection:

```bash
# Test status endpoint
curl "http://localhost:60610/api/status?api_key=a"

# Test plans endpoint  
curl "http://localhost:60610/api/plans/allowed?api_key=a"
```

## Configuration Files

- **ipython-profile/**: Contains the IPython profile with startup scripts
- **ipython-profile/profile_collection_sim/startup/**: Startup scripts loaded by the queue server
  - `00-ophyd.py`: Device definitions
  - `05-run-engine.py`: Run engine setup
  - `15-plans.py`: Plan definitions
  - `99-custom.py`: Custom configurations
  - `existing_plans_and_devices.yaml`: Plans and devices configuration
  - `user_group_permissions.yaml`: User permissions

## Using with Phoebus

1. Start the containers using the commands above
2. In Phoebus, set the queue server URL to: `http://localhost:60610`
3. Set the API key to: `a`
4. The Java client will connect to the HTTP server, which communicates with the queue server

## Troubleshooting

**Connection timeouts:**
- Ensure both containers are healthy: `docker-compose ps`
- Check queue server logs: `docker-compose logs queue-server`
- Check HTTP server logs: `docker-compose logs http-server`

**Port conflicts:**
- If port 6379 is in use, the container Redis uses port 6380 externally
- If ports 60610 or 60615 are in use, stop any existing bluesky processes

**Rebuild everything:**
```bash
docker-compose down
docker-compose build --no-cache
docker-compose --profile container-redis up -d
```