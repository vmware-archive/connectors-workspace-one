To install the connector:

./install.sh [TENANT_PUBLIC_KEY_URL]

TENANT_PUBLIC_KEY_URL - The URL of the vIDM public key used for validating auth tokens.

Example usage:

./install.sh "https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem"