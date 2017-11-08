To install the connector:

./install.sh [TENANT_PUBLIC_KEY_URL] [MANAGED_APPS_FILE_PATH]

TENANT_PUBLIC_KEY_URL - The URL of the vIDM public key used for validating auth tokens.
MANAGED_APPS_FILE_PATH - File path to a yml file containing details of managed apps. It is used for bundleID mapping and
also for generating connector regex.

Content of a managed-apps file looks like below. (sample-managed-apps.yml):
android:
    boxer: com.boxer.email
    concur: com.concur.breeze
    suite:com.hijack.android
ios:
    vmware boxer: com.air-watch.boxer
    concur: com.concur.concurmobile


Example usage:

./install.sh "https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem" "/home/airwatch/Desktop/airwatch/sample-managed-apps.yml"

