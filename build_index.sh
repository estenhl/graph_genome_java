#!/bin/bash

if [ -d "target" ]; then
    java -Xms4096m -Xmx4096m -jar target/graph-genome.jar index "$@"
else
    echo "No target files detected. Build the project with >mvn clean install"
fi
