# Workspace One Connectors

## Overview

These are connectors that have been developed for Workspace One Mobile Flows. Although all of them can be used as-is, 
they can also be used as base from which VMware customers can develop their own connectors.

The connectors are written in Java and use the [Spring Framework](https://spring.io/). More specifically, they use 
[Spring Boot](https://projects.spring.io/spring-boot/), embedding [Tomcat](http://tomcat.apache.org/) 8.5.

For a detailed, language-neutral, specification for how to develop connectors, please see the 
[Card Connectors Guide](https://github.com/vmwaresamples/card-connectors-guide).


### Prerequisites

* Java 8

### Build

There are two ways of building. The first is simply to create the fat jars:

    ./mvnw clean install
    
Additionally, it is possible to build RPMs, which install the connectors as services on RPM-based systems:
    
    ./mvnw clean install -Pmake-rpm

Building the RPMs is possible only within an RPM-based system. A [Vagrantfile](https://github.com/vmware/connectors-workspace-one/blob/master/Vagrantfile)   
is provided for this purpose.   

### Installation

Each connector has its own RPM. For example, `jira-connector-1.0.noarch.rpm`.

The first step is to use the RPM to install the connector as a service. For example:
```
yum install jira-connector-1.0.noarch.rpm 
```
Before the service can be run, some configuration is required. The connectors authenticate requests expecting an access token from VMware IDM. These tokens are JWTs whose signatures must be verified using a public key. This public key is acquired from a URL that is supplied to the connector via a new configuration file. 

For example, for the Jira connector:
```
echo "security.oauth2.resource.jwt.key-uri=https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem" > /etc/opt/vmware/connectors/jira/application.properties
```
The hostname of the URL will vary depending on your IDM tenant.

The configuration file created above must be part of the `roswell` user and group. Again, using Jira as an example:
```
chown roswell:roswell /etc/opt/vmware/connectors/jira/application.properties
```
The connector being a Spring Boot application, many other configuration options are available&mdash;for example, `server.port`. Please see the Spring Boot documentation for more details.

There might also be connector-specifc configuration required. Please see the README files within the [individual connectors](https://github.com/vmware/connectors-workspace-one/tree/master/connectors) for further details.

Once the connector is configured, it can be started. For example:
```
systemctl start jira-connector
```
Check the status after about 10-20 seconds to make sure the service is good:
```
sudo systemctl status jira-connector
```
Also check the logs if there are problems:
```
less /var/log/vmware/connectors/jira/jira-connector.log
```
## Contributing

The connectors-workspace-one project team welcomes contributions from the community. Before you start working with 
connectors-workspace-one, please read our [Developer Certificate of Origin](https://cla.vmware.com/dco). All 
contributions to this repository must be signed as described on that page. Your signature certifies that you wrote 
the patch or have the right to pass it on as an open-source patch. For more detailed information, refer 
to [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Workspace One Connectors are available under the [BSD 2 license](https://github.com/vmware/connectors-workspace-one/blob/master/LICENSE.txt)
