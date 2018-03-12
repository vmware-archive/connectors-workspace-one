#!/bin/bash


docker stop airwatch-connector
docker rm airwatch-connector
docker run --name airwatch-connector -p 4001:4001 -d airwatch-connector
