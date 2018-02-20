#!/bin/bash


docker stop servicenow-connector
docker rm servicenow-connector
docker run --name servicenow-connector -p 4000:4000 -d servicenow-connector
