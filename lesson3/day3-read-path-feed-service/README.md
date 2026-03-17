# Day 3: Read Path – Feed Service

Timeline reconstruction and feed logic with virtual threads (Project Loom).

## Quick start

From the **lesson3** directory:

1. **Setup and build**
   ```bash
   ./setup.sh
   ```
   (Or from here: `./build.sh` after setup has been run once.)

2. **Start the server**
   ```bash
   ./start-server.sh
   ```
   Listens on port 8081.

3. **In another terminal: run tests and demo**
   ```bash
   ./run-tests.sh
   ./run-demo.sh
   ./view-dashboard.sh
   ```
   Open `dashboard_out.html` for metrics.

## Cleanup

Stop the server (Ctrl+C) or run from this directory:

```bash
./stop.sh
```

For full cleanup (containers, Docker prune), run the repo root `cleanup.sh` or this directory’s `cleanup.sh`.

## Requirements

See [requirements.txt](requirements.txt).
