if [ -d "target" ]; then
    java -jar target/graph-genome.jar build-and-align "$@"
else
    echo "No target files detected. Build the project with >mvn clean install"
fi
