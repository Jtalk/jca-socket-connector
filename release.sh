#!/usr/bin/env bash

mvn release:prepare -DautoVersionSubmodules="true" && \
mvn release:perform -Darguments="-Dmaven.deploy.skip=true" 


