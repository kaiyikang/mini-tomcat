#!/bin/bash

cd ..

# Install the Java files with Maven
mvn clean install;
if [ $? -ne 0 ]; then
    echo "Maven build failed. Exiting."
    exit 1
fi

# Execute the Java application
java -jar target/simple-http-server-1.0.0.jar;