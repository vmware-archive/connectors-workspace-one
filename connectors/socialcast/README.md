# Socialcast Connector

## Building

```shell
# From the top level directory
./mvnw clean install -am -pl :socialcast-connector -Pmake-rpm
```

## Installing

### With yum

```shell
sudo yum install socialcast-connector-version.noarch.rpm
```

### With rpm

```shell
sudo rpm -i socialcast-connector-version.noarch.rpm
```

### Initial configuration

After the connector is installed for the first time, you need to configure the vIDM public key url that will be used for validating auth tokens.

```shell
# Create an application.properties file with the vIDM public key url configured
#   (alternatively, you can just copy /opt/vmware/connectors/socialcast/application.properties
#   to /etc/opt/vmware/connectors/socialcast/application.properties and modify it to have the
#   vIDM public key url configured for this step)
sudo echo "security.oauth2.resource.jwt.key-uri=https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem" > /etc/opt/vmware/connectors/socialcast/application.properties

# Make sure the config is readable by the roswell user and group
sudo chown roswell:roswell /etc/opt/vmware/connectors/socialcast/application.properties

# Now you can start the service
sudo systemctl start socialcast-connector

# Check the status after about 10-20 seconds to make sure the service is good
sudo systemctl status socialcast-connector

# You can also confirm there aren't any problems in the logs
less /var/log/vmware/connectors/socialcast/socialcast-connector.log
```

### Add User to Group For Convenience

It is probably convenient for you to add your user to the roswell group so it is easier to view/modify config files (without the need to sudo).

```shell
sudo usermod -aG roswell vagrant
```


## Updating

### With yum

```shell
sudo yum upgrade socialcast-connector-version.noarch.rpm
```

### With rpm

```shell
sudo rpm -U socialcast-connector-version.noarch.rpm
```

Updating will not touch your overriding application.properties in /etc.  It is probably a good idea to take a glance at /opt/vmware/connectors/socialcast/application.properties after updating to verify that there aren't any new things in the config that you should override.  If there are any new settings you wish to config, make sure you do so in the application.properties in /etc so that future updates/uninstalls don't throw away your config.


## Uninstalling

### With yum

```shell
sudo yum remove socialcast-connector
```

### With rpm

```shell
sudo rpm -e socialcast-connector
```
