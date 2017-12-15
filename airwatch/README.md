# Airwatch Connector

## Building

```shell
# From the top level directory
./mvnw clean install -am -pl :airwatch-connector -Pmake-rpm
```

## Installing

### With yum

```shell
sudo yum install airwatch-connector-version.noarch.rpm
```

### With rpm

```shell
sudo rpm -i airwatch-connector-version.noarch.rpm
```

### Initial configuration

After the connector is installed for the first time, you need to configure two things. 
1. The vIDM public key url that will be used for validating auth tokens.
2. Details of managed apps that will be used for mapping app name to app id.


```shell
# 1.) Create an application.properties file with the vIDM public key url configured
#   (alternatively, you can just copy /opt/vmware/connectors/airwatch/application.properties
#   to /etc/opt/vmware/connectors/airwatch/application.properties and modify it to have the
#   vIDM public key url configured for this step)
sudo echo "security.oauth2.resource.jwt.key-uri=https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem" > /etc/opt/vmware/connectors/airwatch/application.properties

# 2.) Create a managed-apps.yml file at /opt/vmware/connectors/airwatch/.
#   (alternatively, you can just copy /opt/vmware/connectors/airwatch/managed-apps.yml
#   to /etc/opt/vmware/connectors/airwatch/managed-apps.yml and modify it to contain all
#   all the managed apps details.)
sudo cp /opt/vmware/connectors/airwatch/managed-apps.yml /etc/opt/vmware/connectors/airwatch/managed-apps.yml
sudo vim /etc/opt/vmware/connectors/airwatch/managed-apps.yml

# Make sure the configs are readable by the roswell user and group
sudo chown roswell:roswell /etc/opt/vmware/connectors/airwatch/application.properties
sudo chown roswell:roswell /etc/opt/vmware/connectors/airwatch/managed-apps.yml

# Now you can start the service
sudo systemctl start airwatch-connector

# Check the status after about 10-20 seconds to make sure the service is good
sudo systemctl status airwatch-connector

# You can also confirm there aren't any problems in the logs
less /var/log/vmware/connectors/airwatch/airwatch-connector.log
```

### If new managed apps are added in AirWatch

```shell
# Edit managed-apps.yml file to include all managed apps in AirWatch.
sudo vim /etc/opt/vmware/connectors/airwatch/managed-apps.yml

# Restart the connector service.
sudo systemctl restart airwatch-connector
```

### Add User to Group For Convenience

It is probably convenient for you to add your user to the roswell group so it is easier to view/modify config files (without the need to sudo).

```shell
sudo usermod -aG roswell vagrant
```


## Updating

### With yum

```shell
sudo yum upgrade airwatch-connector-version.noarch.rpm
```

### With rpm

```shell
sudo rpm -U airwatch-connector-version.noarch.rpm
```

Updating will not touch your overriding application.properties in /etc.  It is probably a good idea to take a glance at /opt/vmware/connectors/airwatch/application.properties after updating to verify that there aren't any new things in the config that you should override.  If there are any new settings you wish to config, make sure you do so in the application.properties in /etc so that future updates/uninstalls don't throw away your config.


## Uninstalling

### With yum

```shell
sudo yum remove airwatch-connector
```

### With rpm

```shell
sudo rpm -e airwatch-connector
```
