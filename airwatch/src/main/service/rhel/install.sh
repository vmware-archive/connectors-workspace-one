#!/usr/bin/env bash

if [ "$#" -ne 2 ]; then
    echo "Usage: install [TENANT_PUBLIC_KEY_URL] [MANAGED_APPS_FILE_PATH]"
    exit 1
fi

cd $(dirname $0)

TENANT_PUBLIC_KEY_URL=$(echo "$1" | sed 's/&/\\&/')
MANAGED_APPS_FILE_PATH=$2

yum update -y
yum install java-1.8.0-openjdk -y
useradd -r -s /bin/false roswell
mkdir -p /opt/vmware/connectors
mkdir -p /var/log/vmware/connectors
chown roswell /var/log/vmware/connectors/
mkdir -p /etc/opt/vmware/connectors

cp files/airwatch-connector.service /etc/systemd/system/
cp files/logback.xml /etc/opt/vmware/connectors/
cp $MANAGED_APPS_FILE_PATH /etc/opt/vmware/connectors/managed-apps.yml
cp files/airwatch-connector.jar /opt/vmware/connectors/


sed s~{TENANT_PUBLIC_KEY_URL}~$TENANT_PUBLIC_KEY_URL~ files/airwatch-connector.service > /etc/systemd/system/airwatch-connector.service

systemctl daemon-reload
systemctl start airwatch-connector
systemctl enable airwatch-connector