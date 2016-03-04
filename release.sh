#!/usr/bin/env bash

mvn release:prepare -DautoVersionSubmodules="false" && \
mvn release:perform -Darguments="-Dmaven.deploy.skip=true" 


