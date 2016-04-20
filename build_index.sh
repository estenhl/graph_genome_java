#!/bin/bash

if [ -d "target" ]; then
    java -Xmx4096m -Xms4096m -jar target/graph-genome.jar index "$@"
else
    echo "No target files detected. Build the project with >mvn clean install"
fi
