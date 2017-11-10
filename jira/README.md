# Jira Connector

**Note:** Before running the Jira connector, make sure the discovery server and the OAuth server are running.

## Building

```shell
# From the top level directory
./mvnw clean install -am -pl :jira-connector -Pmake-rpm
```

## Installing

### With yum

```shell
sudo yum install jira-connector-version.noarch.rpm
```

### With rpm

```shell
sudo rpm -i jira-connector-version.noarch.rpm
```

### Initial configuration

After the connector is installed for the first time, you need to configure the vIDM public key url that will be used for validating auth tokens.

```shell
# Create an application.properties file with the vIDM public key url configured
#   (alternatively, you can just copy /opt/vmware/connectors/jira/application.properties
#   to /etc/opt/vmware/connectors/jira/application.properties and modify it to have the
#   vIDM public key url configured for this step)
sudo echo "security.oauth2.resource.jwt.key-uri=https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem" > /etc/opt/vmware/connectors/jira/application.properties

# Make sure the config is readable by the roswell user and group
sudo chown roswell:roswell /etc/opt/vmware/connectors/jira/application.properties

# Now you can start the service
sudo systemctl start jira-connector

# Check the status after about 10-20 seconds to make sure the service is good
sudo systemctl status jira-connector

# You can also confirm there aren't any problems in the logs
less /var/log/vmware/connectors/jira/jira-connector.log
```

### Add User to Group For Convenience

It is probably convenient for you to add your user to the roswell group so it is easier to view/modify config files (without the need to sudo).

```shell
sudo usermod -aG roswell vagrant
```


## Updating

### With yum

```shell
sudo yum upgrade jira-connector-version.noarch.rpm
```

### With rpm

```shell
sudo rpm -U jira-connector-version.noarch.rpm
```

Updating will not touch your overriding application.properties in /etc.  It is probably a good idea to take a glance at /opt/vmware/connectors/jira/application.properties after updating to verify that there aren't any new things in the config that you should override.  If there are any new settings you wish to config, make sure you do so in the application.properties in /etc so that future updates/uninstalls don't throw away your config.


## Uninstalling

### With yum

```shell
sudo yum remove jira-connector
```

### With rpm

```shell
sudo rpm -e jira-connector
```


## Exercise the Jira API

### Get details of a specific JIRA issue

```shell
curl -i -X GET \
     -H 'X-Jira-Authorization:Bearer 00D41000000FLSG!AREAQH7ZNr_kQ7XY5UwQhUw11Ml7CxF0a726MsqfiKH4FZOqIpXC2o7YI3wt5_FT_n89nGLSKiFGruzbnmMqfYA61KhSY.Oc' \
     -H "Authorization:Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE0NzUyMTYyMzYsInVzZXJfbmFtZSI6Impkb2UiLCJqdGkiOiI3YmQ0MjUyNi1kNWQ5LTQ0N2EtOTZiMC0zNjBiZDJiZGUyNDMiLCJjbGllbnRfaWQiOiJyb3N3ZWxsIiwic2NvcGUiOlsicmVhZCIsIndyaXRlIl19.bR3u5CcJRi0JtuiNKfCOInzZGIM5mg-w1xMQFBQh3ajxVxbwBqhWXb5WzxX1YzbNGUoBIK5Xy0U5-2MjyB-yy1jRrhSFVX7xpPmWC0eqVGTagp_k7LLSS03Q03AvfvbqVn1tClvk-OH4NNkvcuMQWpUT85j2mnMvLPeUikqekDD6l0HfD9cfmaZ7sMYlXM3F7FQvPwQ1a53yUXEZJX6q3OS2N0m5rHnldtFjj7spbrAEGMy92VsWKIqK3s6KE-Obkuk_zw-08ABnOqkSjHQJXLVMopX2YU6H75jpLUzX_llUPYd8tXIZG7ttgbsehvLRu-x3aTbLI8uPSVI0gYF0SQ" \
     -H "Content-Type:application/json" \
     'http://localhost:8061/api/v1/issues/APF-5'
```

### Add Comment to JIRA issue

```shell
curl -i -X POST \
     -H 'X-Jira-Authorization:Bearer 00D41000000FLSG!AREAQH7ZNr_kQ7XY5UwQhUw11Ml7CxF0a726MsqfiKH4FZOqIpXC2o7YI3wt5_FT_n89nGLSKiFGruzbnmMqfYA61KhSY.Oc' \
     -H "Authorization:Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE0NzUyMTYyMzYsInVzZXJfbmFtZSI6Impkb2UiLCJqdGkiOiI3YmQ0MjUyNi1kNWQ5LTQ0N2EtOTZiMC0zNjBiZDJiZGUyNDMiLCJjbGllbnRfaWQiOiJyb3N3ZWxsIiwic2NvcGUiOlsicmVhZCIsIndyaXRlIl19.bR3u5CcJRi0JtuiNKfCOInzZGIM5mg-w1xMQFBQh3ajxVxbwBqhWXb5WzxX1YzbNGUoBIK5Xy0U5-2MjyB-yy1jRrhSFVX7xpPmWC0eqVGTagp_k7LLSS03Q03AvfvbqVn1tClvk-OH4NNkvcuMQWpUT85j2mnMvLPeUikqekDD6l0HfD9cfmaZ7sMYlXM3F7FQvPwQ1a53yUXEZJX6q3OS2N0m5rHnldtFjj7spbrAEGMy92VsWKIqK3s6KE-Obkuk_zw-08ABnOqkSjHQJXLVMopX2YU6H75jpLUzX_llUPYd8tXIZG7ttgbsehvLRu-x3aTbLI8uPSVI0gYF0SQ" \
     -H "Content-Type:application/json" \
     -d '{"body": "This is a new comment"}' \
     'http://localhost:8061/api/v1/issues/123df56/comment'
```
