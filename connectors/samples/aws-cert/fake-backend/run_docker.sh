#!/bin/bash

port=9051
imageName=fake-aws-cert-backend

docker stop $imageName
docker rm $imageName
docker run --name $imageName -p $port:8080 -d $imageName
