#!/usr/bin/env bash

if [ $# == 1 ]; then
	echo "Socket connector API version explicitly set to $1"
	SOCKET_CONNECTOR_API_VERSION_VALUE=-Dsocket.connector.api.version=$1
fi

mvn release:prepare -DautoVersionSubmodules="false" $SOCKET_CONNECTOR_API_VERSION_VALUE && \
mvn release:perform -Darguments="-Dmaven.deploy.skip=true" $SOCKET_CONNECTOR_VERSION_VALUE


