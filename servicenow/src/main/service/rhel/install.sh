#!/usr/bin/env bash

if [ "$#" -ne 1 ]; then
    echo "Usage: install [TENANT_PUBLIC_KEY_URL]"
    exit 1
fi

cd $(dirname $0)

TENANT_PUBLIC_KEY_URL=$(echo "$1" | sed 's/&/\\&/')

yum update -y
yum install java-1.8.0-openjdk -y
useradd -r -s /bin/false roswell
mkdir -p /opt/vmware/connectors
mkdir -p /var/log/vmware/connectors
chown roswell /var/log/vmware/connectors/
mkdir -p /etc/opt/vmware/connectors

cp files/servicenow-connector.service /etc/systemd/system/
cp files/logback.xml /etc/opt/vmware/connectors/
cp files/servicenow-connector.jar /opt/vmware/connectors/


sed s~{TENANT_PUBLIC_KEY_URL}~$TENANT_PUBLIC_KEY_URL~ files/servicenow-connector.service > /etc/systemd/system/servicenow-connector.service

systemctl daemon-reload
systemctl start servicenow-connector
systemctl enable servicenow-connector
