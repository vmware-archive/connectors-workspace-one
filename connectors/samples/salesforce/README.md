# Salesforce Connector

**Note:** Before running the Salesforce connector, make sure the discovery server and the OAuth server are running.

## Building

```shell
# From the top level directory
./mvnw clean install -am -pl :salesforce-connector -Pmake-rpm
```

## Installing

### With yum

```shell
sudo yum install salesforce-connector-version.noarch.rpm
```

### With rpm

```shell
sudo rpm -i salesforce-connector-version.noarch.rpm
```

### Initial configuration

After the connector is installed for the first time, you need to configure the vIDM public key url that will be used for validating auth tokens.

```shell
# Create an application.properties file with the vIDM public key url configured
#   (alternatively, you can just copy /opt/vmware/connectors/salesforce/application.properties
#   to /etc/opt/vmware/connectors/salesforce/application.properties and modify it to have the
#   vIDM public key url configured for this step)
sudo echo "security.oauth2.resource.jwt.key-uri=https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem" > /etc/opt/vmware/connectors/salesforce/application.properties

# Make sure the config is readable by the roswell user and group
sudo chown roswell:roswell /etc/opt/vmware/connectors/salesforce/application.properties

# Now you can start the service
sudo systemctl start salesforce-connector

# Check the status after about 10-20 seconds to make sure the service is good
sudo systemctl status salesforce-connector

# You can also confirm there aren't any problems in the logs
less /var/log/vmware/connectors/salesforce/salesforce-connector.log
```

### Add User to Group For Convenience

It is probably convenient for you to add your user to the roswell group so it is easier to view/modify config files (without the need to sudo).

```shell
sudo usermod -aG roswell vagrant
```


## Updating

### With yum

```shell
sudo yum upgrade salesforce-connector-version.noarch.rpm
```

### With rpm

```shell
sudo rpm -U salesforce-connector-version.noarch.rpm
```

Updating will not touch your overriding application.properties in /etc.  It is probably a good idea to take a glance at /opt/vmware/connectors/salesforce/application.properties after updating to verify that there aren't any new things in the config that you should override.  If there are any new settings you wish to config, make sure you do so in the application.properties in /etc so that future updates/uninstalls don't throw away your config.


## Uninstalling

### With yum

```shell
sudo yum remove salesforce-connector
```

### With rpm

```shell
sudo rpm -e salesforce-connector
```


## Prerequisite to using Salesforce Rest API

### Create a developer account with salesforce.com

Go to http://developer.salesforce.com and fill out the sign up form to get a salesforce account.

Once account is created, a client is created for the new account and salesforce.com provides the client id and client secret.

The client id and client secret are needed to create authorization tokens.

### Create Salesforce.com Security Token

**Note:** In addition to the password that you specified at the time of creating the developer's account, you also need to create a security token, that together with the password, will be needed for getting the authorization token.

1. Login to http://login.salesforce.com with your login credentials.
2. Once successfully logged in, check the salesforce host that you are redirected to (e.g. https://**na35.salesforce.com**/setup/forcecomHomepage.apexp?setupid=ForceCom).
3. Save the hostname, which is the salesforce instance your account is attached to. **Note:** From now on, this host string is referred as 'sf.sandbox.host' in this document.
4. From top right corner, choose option 'My Settings' when you click your name.
5. Click on the link 'Change My Password' under the 'Quick Links' section.
6. On the left hand menu, choose the 'Reset Security Token' menu item.
7. Once you submit the request, a security token is generated and emailed to the account attached to your developer account.
8. Store the security token.

### Create a Connected App

You must create a (fictional) Connected Application in your Salesforce account in order to access the Salesforce API.

1. Click "Setup" in the upper right-hand corner.
2. From the menu on the left, find "Build" and click the arrow next to "Create" to expand the menu options there.
3. Click on "Apps".
4. Find the "Connected Apps" section of the page. If you have just created your account, this section will be empty.
5. Click the "New" button to create a new Connected App.
6. Enter a Connected App Name for your app -- something as simple as "ApiTest" will do. The name should then automatically populate the API Name field. Supply a Contact Email.
7. Under "API (Enable OAuth API Settings"), check the Enable Oauth Settings box. For the Callback URL, enter `https://login.salesforce.com/services/oauth2/callback`.
8. For Selected OAuth Scopes, select at least "api", "full", and "refresh_token, open_access".
9. Leave "Use digital signatures" and "Include ID Token" unchecked.
10. Click the "Save" button. This will create the app.
11. Now you should be able to view the list of apps again (Build -> Create -> Apps) and see your app listed under Connected Apps.
12. Click on the app's name to see its details. You should see a "Consumer Key" value, which is a long alphanumeric string; this is your "Client ID" to use in generating access tokens.
13. Click on the "Click to reveal" link next to "Consumer Secret" and your "Client Secret" will be displayed. This currently seems to be a decimal number of about 20 digits.

### Edit Access Permissions

Before you can authenticate and retrieve an OAuth token, you need to modify your Connected Application to allow your user to gain access through it.

1. Click on "Setup" if necessary. From the menu on the left, under "Administer", expand "Manage Apps" and click on "Connected Apps" (NOT on "Connected Apps OAuth Usage").
2. You should see your Connected App listed. Click "Edit" to the left of the app's name.
3. Under "OAuth Policies" on the next screen, you should see a drop-down field labeled "Permitted Users"; its current value will probably be "Admin approved users are pre-authorized".
4. Change its value to "All users may self-authorize".
5. Optionally, for "IP Relaxation", choose "Relax IP restrictions". This should allow you to omit your security token from the "Password" field when you submit a token request (below).
6. Click "Save" to save your changes. You should now be able to request a bearer token.

### Generate a Session Id-based Bearer Token to access salesforce.com rest api(s)

Curl the salesforce oauth2/token url and retrieve the access token from the response:

```shell
curl https://login.salesforce.com/services/oauth2/token \
     --data-urlencode "grant_type=password" \
     --data-urlencode "client_id=<client id for your Connected App>" \
     --data-urlencode "client_secret=<client secret for your Connected App>" \
     --data-urlencode "username=<force.com user name>" \
     --data-urlencode "password=<force.com password><security token created in Step 2>"
```

**Note:** Execute the command whenever a token expires and a new token needs to be generated.


## Exercise the Salesforce API

**Note:**
* The value of the header 'X-Salesforce-Authorization' is the authorization token for accessing salesforce rest API. Please refer to the section above on how to generate the auth token.
* The value of the header 'Authorization' is the oauth token for accessing Roswell Api Gateway, please refer to the README.md file under the auth-server module on how to generate the oauth token.

### Create an Account

```shell
curl https://sf.sandbox.host/services/data/v20.0/sobjects/Account/ \
     -H "Authorization: Bearer <token generated from Step 3>" \
     -H "Content-Type: application/json" \
     -d '{"Name":"<your account name>"}'
```

### Search for matching Accounts

```shell
curl -i -X GET \
     -H "X-Salesforce-Authorization:Bearer 00D41000000FLSG!AREAQLQPHCrGz_y_hUbyYYHsnQSFvR1QfxqElFmO7qWZVrGTIEHcJODeWethQlmJKZAr3rVo3IybvwYro3R8yftm7Rc68Iq1" \
     -H "Content-Type:application/json" \
     -H "Authorization:Bearer eyJraWQiOiJ3b3U3dWhkYjZmYyIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJqZG9lIiwiZXhwIjoxNDc2MDI3MjI1fQ.R7j-oEUWp4qDI4c1ZNulvAKQqyEkADdi2cqiG67HZboYPU1vnBTLUEMKuLoSge5eQhIMTC4xvGlqdEdImhN2AgHSmfeZzsaoC7ZUhF0usBEWGHnEYnk9kIoipd338yMiiopg1COqGNh1xfodCsNxMwnZOc8NbM5xiJB0oTxe2bAMWd22nVQN_YM2bUZimwJCYRzzvL7UWFphKN1awmj0bMiLEUFtwbo7DPOvFkkB7Y5kmg9OkG4OfHpn-ePs6OPDcRdEujBM96shS_H39UK9dM0st7VIcmzrgpvdbvTTdMrszNlpTnObjqOKu_jr-yxaV96Z9S2VJtmhMhC-hod1nw" \
     'http://localhost:8061/api/v1/accounts?accountSearchTxt=ACME'
```

#### Sample Response

```json
[
  {
    "name": "ACME LLC",
    "addContactLink": "/api/v1/contacts?accountId=00141000004KMUhAAO"
  },
  {
    "name": "ACME INC",
    "addContactLink": "/api/v1/contacts?accountId=99252000005JLVxBBO"
  }
]
```

### Add Contact to selected Account

```shell
curl -i -X POST \
     -H "X-Salesforce-Authorization:Bearer 00D41000000FLSG!AREAQLQPHCrGz_y_hUbyYYHsnQSFvR1QfxqElFmO7qWZVrGTIEHcJODeWethQlmJKZAr3rVo3IybvwYro3R8yftm7Rc68Iq1" \
     -H "Content-Type:application/json" \
     -H "Authorization:Bearer eyJraWQiOiJ3b3U3dWhkYjZmYyIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJqZG9lIiwiZXhwIjoxNDc2MDI3MjI1fQ.R7j-oEUWp4qDI4c1ZNulvAKQqyEkADdi2cqiG67HZboYPU1vnBTLUEMKuLoSge5eQhIMTC4xvGlqdEdImhN2AgHSmfeZzsaoC7ZUhF0usBEWGHnEYnk9kIoipd338yMiiopg1COqGNh1xfodCsNxMwnZOc8NbM5xiJB0oTxe2bAMWd22nVQN_YM2bUZimwJCYRzzvL7UWFphKN1awmj0bMiLEUFtwbo7DPOvFkkB7Y5kmg9OkG4OfHpn-ePs6OPDcRdEujBM96shS_H39UK9dM0st7VIcmzrgpvdbvTTdMrszNlpTnObjqOKu_jr-yxaV96Z9S2VJtmhMhC-hod1nw" \
     -d '{"FirstName": "David", "LastName": "Shaw"}' \
     'http://localhost:8061/api/v1/contacts?accountId=00141000004KMUhAAO'
```

### Get metadata for salesforce entities (e.g. account, contact)

```shell
curl -i -X GET \
     -H 'X-Salesforce-Authorization:Bearer 00D41000000FLSG!AREAQH7ZNr_kQ7XY5UwQhUw11Ml7CxF0a726MsqfiKH4FZOqIpXC2o7YI3wt5_FT_n89nGLSKiFGruzbnmMqfYA61KhSY.Oc' \
     -H "Authorization:Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE0NzUyMTYyMzYsInVzZXJfbmFtZSI6Impkb2UiLCJqdGkiOiI3YmQ0MjUyNi1kNWQ5LTQ0N2EtOTZiMC0zNjBiZDJiZGUyNDMiLCJjbGllbnRfaWQiOiJyb3N3ZWxsIiwic2NvcGUiOlsicmVhZCIsIndyaXRlIl19.bR3u5CcJRi0JtuiNKfCOInzZGIM5mg-w1xMQFBQh3ajxVxbwBqhWXb5WzxX1YzbNGUoBIK5Xy0U5-2MjyB-yy1jRrhSFVX7xpPmWC0eqVGTagp_k7LLSS03Q03AvfvbqVn1tClvk-OH4NNkvcuMQWpUT85j2mnMvLPeUikqekDD6l0HfD9cfmaZ7sMYlXM3F7FQvPwQ1a53yUXEZJX6q3OS2N0m5rHnldtFjj7spbrAEGMy92VsWKIqK3s6KE-Obkuk_zw-08ABnOqkSjHQJXLVMopX2YU6H75jpLUzX_llUPYd8tXIZG7ttgbsehvLRu-x3aTbLI8uPSVI0gYF0SQ" \
     -H "Content-Type:application/json" \
     'http://localhost:8061/api/v1/metadata/entity/account'
```

### References

* [Force.com REST API Developer Guide Quick Start](https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/quickstart.htm)
