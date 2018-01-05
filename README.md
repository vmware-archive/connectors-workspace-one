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

Please see README files within the [individual connectors](https://github.com/vmware/connectors-workspace-one/tree/master/connectors) for details on how to install the RPMs.


## Contributing

The connectors-workspace-one project team welcomes contributions from the community. Before you start working with 
connectors-workspace-one, please read our [Developer Certificate of Origin](https://cla.vmware.com/dco). All 
contributions to this repository must be signed as described on that page. Your signature certifies that you wrote 
the patch or have the right to pass it on as an open-source patch. For more detailed information, refer 
to [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Workspace One Connectors are available under the [BSD 2 license](https://github.com/vmware/connectors-workspace-one/blob/master/LICENSE.txt)