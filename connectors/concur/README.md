# Concur Connector

The Concur connector presents cards inviting the user to approve an expense report, reject an expense report and open an expense report in a browser. It does this based on Report ID passed as tokens in the card request.

Approving an expense report, rejecting an expense report, open the expense report in browser are the actions supported by this connector.

For generic details on how to build, install, and configure connectors, please see the [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md) at the root of this repository.


## Docker

Concur connector requires additional configuration like concur OAuth instance url, client id, and client secret.

```
docker run --name concur-connector \
           -p 8080:8080 \
           ws1connectors/concur-connector \
           --server.port=8080 \
           --concur.oauth-instance-url="https://us-impl.api.concursolutions.com" \
           --concur.client-id="2803b8f8-a42b-43c2-a739-b8768e4759b8" \
           --concur.client-secret="e013335d-b4ce-4c43-a7e4-b67abc1adcb0" \
           --security.oauth2.resource.jwt.key-uri="https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem"
```
