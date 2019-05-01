# Coupa Connector for WS1 Hub

The Coupa connector presents cards inviting the user to approve or reject a report.

Approving and rejecting a report are the actions supported by this connector.

For generic details on how to build, install, and configure connectors, please see the [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md) at the root of this repository.

## Docker

Coupa connector requires an additional configuration of the coupa.api-key for the service account:

```
docker run --name hub-coupa-connector \
           -p 8080:8080 \
           ws1connectors/hub-coupa-connector \
           --server.port=8080 \
           --coupa.api-key="xyz" \
           --security.oauth2.resource.jwt.key-uri="https://prod.hero.vmwservices.com/security/public-key"
```
