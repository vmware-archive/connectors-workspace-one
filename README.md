# Workspace One Connectors

## Overview

These are connectors that have been developed for Workspace One Mobile Flows. Although all of them can be used as-is, 
they can also be used as base from which VMware customers can develop their own connectors.

The connectors are written in Java and use the [Spring Framework](https://spring.io/). More specifically, they use 
[Spring Boot](https://projects.spring.io/spring-boot/), embedding [Tomcat](http://tomcat.apache.org/) 8.5.

For a detailed, language-neutral, specification for how to develop connectors, please see the 
[Card Connectors Guide](https://github.com/vmwaresamples/card-connectors-guide).

This repository also includes common libraries. Please see their [README](https://github.com/vmware/connectors-workspace-one/blob/master/common/README.md) for more details.

### Prerequisites

* Java 8
* Vagrant (use [Vagrantfile](https://github.com/vmware/connectors-workspace-one/blob/master/Vagrantfile))

Vagrant isn't necessary if you're using an RPM-based OS, such as CentOS or RHEL. You will need the following packages though:

```
yum install -y git rpm-build vim nmap-ncat java-1.8.0-openjdk-devel
```
* Clone this repository or download as zip
* Unzip the repo if needed 

### Build

Use the below command from within the repository directory to build the RPMs :
    
    ./mvnw clean install -Pmake-rpm

### Installing

Each connector has its own RPM. For example, `jira-connector-1.0.0.noarch.rpm`. This can be found at  ```Repo DI/connectors/jira/target/rpm/jira-connector/RPMS/noarch/ ```

The first step is to use the RPM to install the connector as a service. For example:

```
yum install jira-connector-1.0.0.noarch.rpm 
```
Before the service can be run, some configuration is required. The connectors authenticate requests expecting an access token from VMware IDM. These tokens are JWTs whose signatures must be verified using a public key. This public key is acquired from a URL that is supplied to the connector via a new configuration file. 

For example, for the Jira connector:

```
echo "security.oauth2.resource.jwt.key-uri=https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem" \
> /etc/opt/vmware/connectors/jira/application.properties
```

The hostname of the URL will vary depending on your IDM tenant.

The configuration file created above must be part of the `roswell` user and group. Again, using Jira as an example:

```
chown roswell:roswell /etc/opt/vmware/connectors/jira/application.properties
```

The connector being a Spring Boot application, many other configuration options are available&mdash;for example, `server.port`. Please see the Spring Boot documentation for more details.

There might also be connector-specific configuration required. Please see the README files within the [individual connectors](https://github.com/vmware/connectors-workspace-one/tree/master/connectors) for further details.

Once the connector is configured, it can be started. For example:

```
systemctl start jira-connector
```

Check the status after about 10-20 seconds to make sure the service is good:

```
systemctl status jira-connector
```

Also check the logs if there are problems:

```
less /var/log/vmware/connectors/jira/jira-connector.log
```

### Updating

The connector can be updated using yum. For example, to update the Jira connector from 1.0.0 to 1.0.1:
```
yum upgrade jira-connector-1.0.1.noarch.rpm
```
Updating will not touch your overriding `application.properties` in `/etc`. It is probably a good idea to take a glance at `/opt/vmware/connectors/jira/application.properties` after updating to verify that there aren't any new things in the config that you should override. If there are any new settings you wish to configure, make sure you do so in the `application.properties` in `/etc` so that future updates/uninstalls don't throw away your config.

## Uninstalling

```
yum remove jira-connector
```

## Contributing

The connectors-workspace-one project team welcomes contributions from the community. Before you start working with 
connectors-workspace-one, please read our [Developer Certificate of Origin](https://cla.vmware.com/dco). All 
contributions to this repository must be signed as described on that page. Your signature certifies that you wrote 
the patch or have the right to pass it on as an open-source patch. For more detailed information, refer 
to [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Workspace One Connectors are available under the [BSD 2 license](https://github.com/vmware/connectors-workspace-one/blob/master/LICENSE.txt)
