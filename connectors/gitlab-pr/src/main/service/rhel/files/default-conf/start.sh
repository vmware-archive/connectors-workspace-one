#!/bin/bash

connector_name='gitlab-pr'
install_path="/opt/vmware/connectors/$connector_name"
etc_path="/etc/opt/vmware/connectors/$connector_name"

source $install_path/jvm.conf

/usr/bin/java $JVM_OPTS \
    -jar $install_path/${connector_name}-connector.jar \
    --spring.config.additional-location=file:$install_path/application.properties,file:$etc_path/application.properties
