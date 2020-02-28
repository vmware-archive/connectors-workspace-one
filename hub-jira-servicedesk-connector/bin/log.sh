#!/bin/bash

cd $(dirname $0) && cd ..
docker-compose logs -t --tail=100 -f
