#!/bin/sh

set -x
commit=`git log --pretty=format:'%h' -n 1`

# Build Frontend
(
    cd frontend
    bash protogen.sh
    npm run build
)

# Build Backend
(
    sbt assembly
    docker build -t "snakes/snakesgg:${commit}" .
    docker push "snakes/snakesgg:${commit}"
)

echo "docker run -p 80:80 snakes/snakesgg:${commit}"
