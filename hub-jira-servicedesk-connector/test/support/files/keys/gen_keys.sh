#!/bin/bash

cd `dirname $0`

if [ -f "private.pem"]; then
  rm -f *pem
fi

openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -outform PEM -pubout -out public.pem