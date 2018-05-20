# Airwatch Connector

The AirWatch connector presents cards inviting the user to install apps that are missing from the user's device. It does this based on application keywords, the device's udid, and the device's platform. All of these are passed as tokens in the card request.

Informal application keywords are resolved to bundle IDs using mappings specified in `/etc/opt/vmware/connectors/airwatch/managed-apps.yml`. For example:
```
airwatch:
  apps:
    - app: Coupa
      android:
        name: Coupa Mobile
        id: com.coupa.android.coupamobile
      ios:
        name: Coupa - Expenses & Approvals
        id: com.coupa.push
      keywords:
        - coupa
        - expense
        - travel
        - reimburse
```
In the above example, "expense" maps to "com.coupa.android.coupamobile" for the Android platform, whereas "expense" maps to "com.coupa.push" for the iOS platform.

Platform specific application names helps connector to uniquely identify an app to trigger MDM install.

For generic details on how to build, install, and configure connectors, please see the [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md) at the root of this repository.

## Docker

The AirWatch App Discovery Connector requires additional configurations to the docker command mentioned in [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md#docker).
They are details of manged apps and Greenbox url.
Assuming you have a `managed-apps.yml` in `/my/config/dir`:

```
docker run --name airwatch-connector \
           -p 8080:8080 \
           -d \
           --mount type=bind,source=/my/config/dir,target=/mnt \
           ws1connectors/airwatch-connector \
           --spring.config.additional-location=file:/mnt/managed-apps.yml \
           --server.port=8080 \
           --greenbox.url="https://acme.vmwareidentity.com" \
           --security.oauth2.resource.jwt.key-uri="https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem"
```

## RPM

In general connectors are built, installed and run as RPMs as explained in [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md#rpm).
Same guide should be followed but make sure to supply the additional configurations required for the AirWatch App Discovery Connector, before starting the service.
Additional configurations include details of manged apps and Greenbox url.

Assuming you have a `managed-apps.yml` in `/my/config/dir`:

Provide a copy of the file for the service to read.
```
cp /my/config/dir/managed-apps.yml /etc/opt/vmware/connectors/airwatch/managed-apps.yml

```
Let connector know the base url of Greenbox. This is needed to trigger the native install action for managed apps.
```
echo "greenbox.url=https://acme.vmwareidentity.com" >> /etc/opt/vmware/connectors/airwatch/application.properties
```

All good to start the app discovery connector service. 
If you would like to cross check the application properties file, it looks like below.

```
cat /etc/opt/vmware/connectors/airwatch/application.properties
security.oauth2.resource.jwt.key-uri=https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem
greenbox.url=https://acme.vmwareidentity.com
```