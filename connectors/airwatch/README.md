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


