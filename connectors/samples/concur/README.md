# Concur Connector

**Note:** Before running the Concur connector, make sure the discovery server and the OAuth server are running.

## Building

```shell
# From the top level directory
./mvnw clean install -am -pl :concur-connector -Pmake-rpm
```

## Installing

### With yum

```shell
sudo yum install concur-connector-version.noarch.rpm
```

### With rpm

```shell
sudo rpm -i concur-connector-version.noarch.rpm
```

### Initial configuration

After the connector is installed for the first time, you need to configure the vIDM public key url that will be used for validating auth tokens.

```shell
# Create an application.properties file with the vIDM public key url configured
#   (alternatively, you can just copy /opt/vmware/connectors/concur/application.properties
#   to /etc/opt/vmware/connectors/concur/application.properties and modify it to have the
#   vIDM public key url configured for this step)
sudo echo "security.oauth2.resource.jwt.key-uri=https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem" > /etc/opt/vmware/connectors/concur/application.properties

# Make sure the config is readable by the roswell user and group
sudo chown roswell:roswell /etc/opt/vmware/connectors/concur/application.properties

# Now you can start the service
sudo systemctl start concur-connector

# Check the status after about 10-20 seconds to make sure the service is good
sudo systemctl status concur-connector

# You can also confirm there aren't any problems in the logs
less /var/log/vmware/connectors/concur/concur-connector.log
```

### Add User to Group For Convenience

It is probably convenient for you to add your user to the roswell group so it is easier to view/modify config files (without the need to sudo).

```shell
sudo usermod -aG roswell vagrant
```


## Updating

### With yum

```shell
sudo yum upgrade concur-connector-version.noarch.rpm
```

### With rpm

```shell
sudo rpm -U concur-connector-version.noarch.rpm
```

Updating will not touch your overriding application.properties in /etc.  It is probably a good idea to take a glance at /opt/vmware/connectors/concur/application.properties after updating to verify that there aren't any new things in the config that you should override.  If there are any new settings you wish to config, make sure you do so in the application.properties in /etc so that future updates/uninstalls don't throw away your config.


## Uninstalling

### With yum

```shell
sudo yum remove concur-connector
```

### With rpm

```shell
sudo rpm -e concur-connector
```

Updating will not touch your overriding application.properties in /etc.  It is probably a good idea to take a glance at /opt/vmware/connectors/concur/application.properties after updating to verify that there aren't any new things in the config that you should override.  If there are any new settings you wish to config, make sure you do so in the application.properties in /etc so that future updates/uninstalls don't throw away your config.

## Exercise the Concur API

### Approve the concur expense request

```shell
curl -X POST \
  'http://localhost:8100/connectors/46cf06a0-d5cf-485a-be5c-8470952ac4ac/api/expense/approve/79D89435DAE94F53BF60?reason=Approve' \
  -H 'content-type: application/x-www-form-urlencoded' \
  -H 'x-concur-authorization: asdfasdf' \
  -H 'x-concur-base-url: http://172.17.0.1:8850/concur'
```

### Reject the concur expense request

```shell
curl -X POST \
  'http://localhost:8100/connectors/46cf06a0-d5cf-485a-be5c-8470952ac4ac/api/expense/approve/79D89435DAE94F53BF60?reason=Reject' \
  -H 'content-type: application/x-www-form-urlencoded' \
  -H 'x-concur-authorization: asdfasdf' \
  -H 'x-concur-base-url: http://172.17.0.1:8850/concur'
```

