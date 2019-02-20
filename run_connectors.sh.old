#!/bin/bash

jvm_args=""
#jvm_args="$jvm_args --add-modules java.activation"
jvm_args="$jvm_args -Xss512k"
jvm_args="$jvm_args -server"
jvm_args="$jvm_args -Xmx1000m"
jvm_args="$jvm_args -XX:+HeapDumpOnOutOfMemoryError"
jvm_args="$jvm_args -XX:HeapDumpPath=$PWD/oome-$(date +%s).hprof"
jvm_args="$jvm_args -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5024"

java $jvm_args \
     -jar connectors/servicenow/target/servicenow*.jar \
     --server.port=9064 \
     --security.oauth2.resource.jwt.key-value="-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvgC4nghTATHFsjrGvC0W
RwIL3lW58goLBiT5I+54qveyneqWNpobUHDquR/EcMvNYzg7VPlJ2wu7aAtNQ4eD
cYohshTGEEKSf6HjuO8Cia6N6r4H4YTcSjY3dh1N7wYoqhOIokrVxHnNn5XA/M8b
tH4MRWGFOCLD76G/gVfnXt3DI2VlljG5ajc8p2nQbe+8khmTl+rY+cmbSCbDEo3b
KadpuFkq5kfS+ssLkIxSmzMOJcxmStYgFqcrb/o9/cDSkkFnDEWUJ8AyI4qzMmnm
E3PG4t8nC/ixTlKAm885MCrzsXKx/KBnQf8TbwHprTFMBZqUWBBWPraZ33Y6oRBc
TQIDAQAB
-----END PUBLIC KEY-----" \
     --management.endpoint.health.show-details=always \
     --logging.level.com.vmware.connectors.servicenow.ServiceNowController=TRACE \
     --logging.level.reactor.ipc.netty.channel.ChannelOperationsHandler=DEBUG \
     --logging.level.org.springframework.web.client.RestTemplate=TRACE \
     --logging.level.org.springframework.cache=TRACE \
     --logging.level.org.springframework.web.client.AsyncRestTemplate=TRACE
