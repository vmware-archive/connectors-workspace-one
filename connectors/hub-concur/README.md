# Concur Connector for WS1 Hub

The Concur connector presents cards inviting the user to approve or reject an expense report.

Approving an expense report and rejecting an expense report are the actions supported by this connector.

For generic details on how to build, install, and configure connectors, please see the [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md) at the root of this repository.

## Docker

Hub concur connector requires additional configuration parameter like OAuth instance url.

```
docker run --name hub-concur-connector \
           -p 8080:8080 \
           ws1connectors/hub-concur-connector \
           --server.port=8080 \
           --concur.oauth-instance-url="https://us-impl.api.concursolutions.com" \
           --security.oauth2.resource.jwt.key-uri="https://prod.hero.vmwservices.com/security/public-key"
```
