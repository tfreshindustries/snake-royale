#!/bin/sh

set -x

# Build Frontend
(
    cd frontend
    npm install
    sh protogen.sh
    npm run build
)

# Build Backend
sbt assembly
mv /usr/src/snakes/server/target/scala-2.12/server.jar server.jar

