# Workspace One Connectors

## Overview

These are connectors that have been developed for Workspace One Mobile Flows. Although all of them can be used as-is,
they can also be used as base from which VMware customers can develop their own connectors.

The connectors are written in Java and use the [Spring Framework](https://spring.io/). More specifically, they use
[Spring Boot](https://projects.spring.io/spring-boot/), embedding [Tomcat](http://tomcat.apache.org/) 8.5.

For a detailed, language-neutral, specification for how to develop connectors, please see the
[Card Connectors Guide](https://github.com/vmware-samples/card-connectors-guide).

This repository also includes common libraries. Please see their [README](https://github.com/vmware/connectors-workspace-one/blob/master/connectors-common/README.md) for more details.

## Docker

The connectors can be run as [Docker](https://www.docker.com/) containers.

### Prerequisites

* Docker

### Running

The latest published versions of the connectors are available in the [Docker Hub registry](https://hub.docker.com/u/ws1connectors/).

For example, you can run the latest jira-connector with:

```
docker run --name jira-connector \
           -p 8080:8080 \
           -d \
           ws1connectors/jira-connector \
           --server.port=8080 \
           --security.oauth2.resource.jwt.key-uri="https://prod.hero.vmwservices.com/security/public-key"
```

## Fat Jars

The connectors can also be built and run as [executable "fat" jars](https://docs.spring.io/spring-boot/docs/current/reference/html/howto-build.html#howto-create-an-executable-jar-with-maven).

### Prerequisites

* [Java 11](https://www.java.com/en/download/help/index_installing.xml)

### Building

Clone this repository or download as zip.
Unzip the repo if you downloaded a zip.
Use the below command from within the repository directory to build the fat JARs:

```
./mvnw clean install
```

### Running

After building the JAR(s), you can run them with `java -jar`.  For example, here is a command to run the jira-connector:

```
java -server \
     -jar \
     connectors/jira/target/jira-connector-2.1-SNAPSHOT.jar \
     --server.port=8080 \
     --security.oauth2.resource.jwt.key-uri="https://prod.hero.vmwservices.com/security/public-key"
```

## NodeJS Based Connectors
The nodejs based connectors can be build and deployed using Docker.

### Prerequisites

* Docker

### Building

Clone this repository or download as zip.
Unzip the repo if you downloaded a zip.
Use the below command from within the repository directory to create the docker image:

```
docker build -t jira-service-desk .
```

### Running

You can run your local container built from the docker build command above with:

```
docker run --name jira-service-desk \
           -p 8080:8080 \
           -e PORT=8080 \
           -e token_public_key_url="https://prod.hero.vmwservices.com/security/public-key" \
           -e MF_JWT_PUB_KEY_URI="https://prod.hero.vmwservices.com/security/public-key" \
           -d \
           jira-service-desk
```

The latest published versions of the connectors are available in the [Docker Hub registry](https://hub.docker.com/u/ws1connectors/).

For example, you can run the latest jira-service-desk connector with:

```
docker run --name jira-service-desk \
           -p 8080:8080 \
           -e PORT=8080 \
           -e token_public_key_url="https://prod.hero.vmwservices.com/security/public-key" \
           -e MF_JWT_PUB_KEY_URI="https://prod.hero.vmwservices.com/security/public-key" \
           -d \
           ws1connectors/jira-service-desk
```

## Troubleshooting

For more information on common mistakes, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## Contributing

The connectors-workspace-one project team welcomes contributions from the community. Before you start working with
connectors-workspace-one, please read our [Developer Certificate of Origin](https://cla.vmware.com/dco). All
contributions to this repository must be signed as described on that page. Your signature certifies that you wrote
the patch or have the right to pass it on as an open-source patch. For more detailed information, refer
to [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Workspace One Connectors are available under the [BSD 2 license](https://github.com/vmware/connectors-workspace-one/blob/master/LICENSE.txt)
