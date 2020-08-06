# Hub Saba Connector for VMware Workspace ONE Mobile Flows Overview

The Hub Saba connector is developed for Workspace One Mobile Flows. Although it can be used as-is, it can also be used as a base from which VMware customers can develop their own connector.

The connector is written using Express framework for NodeJs. 

It has Saba learning related flows, to support Workspace ONE Hub notifications. 
1. Produce notifications for pending Curriculum that aren't yet acquired by the user.
2. Produce notifications for pending Certifications(Accreditation) that aren't yet acquired by the user.

Generated cards contain current status of the learning and also a direct link to launch it in the Saba web. 

## Connector configuration
- MF_JWT_PUB_KEY_URI

It is the MobileFlows server public key URL. If the connector to be associated with prod, please use:
'https://prod.hero.vmwservices.com/security/public-key'


For a detailed, language-neutral, specification for how to develop connectors, please see the
[Card Connectors Guide](https://github.com/vmware-samples/card-connectors-guide).

## License

Workspace One Hub ServiceNow Connector is available under the [BSD 2 license](https://github.com/vmware/connectors-workspace-one/blob/master/LICENSE.txt)
