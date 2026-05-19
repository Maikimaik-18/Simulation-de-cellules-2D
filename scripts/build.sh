#!/usr/bin/env bash
# Build and run the Cell Simulation application from CLI.
# Usage: ./scripts/build.sh
set -e

cd "$(dirname "$0")/.."

echo "==> Compiling..."
mvn clean compile

echo "==> Launching JavaFX application..."
mvn javafx:run
