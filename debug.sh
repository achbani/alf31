#!/bin/bash
if [[ -z ${MAVEN_OPTS} ]]; then
    echo "The environment variable 'MAVEN_OPTS' is not set, setting it for you";
    MAVEN_OPTS="-Xms512m -Xmx6G"
fi
echo "MAVEN_OPTS is set to '$MAVEN_OPTS'";
mvnDebug clean install alfresco:run