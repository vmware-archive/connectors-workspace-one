# Airwatch Connector

The AirWatch connector presents cards inviting the user to install apps that are missing from the user's device. It does this based on application keywords, the device's udid, and the device's platform. All of these are passed as tokens in the card request.

Informal application names are resolved to bundle IDs using mappings specified in `/etc/opt/vmware/connectors/airwatch/managed-apps.yml`. For example:
```
android.boxer=com.android.boxer
android.concur=com.concur.breeze

ios.boxer=com.air-watch.boxer
```
In the above example, "boxer" maps to "com.android.boxer" for the Android platform, whereas "boxer" maps to "com.air-watch.boxer" for the iOS platform.

For generic details on how to build, install, and configure connectors, please see the [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md) at the root of this repository.


