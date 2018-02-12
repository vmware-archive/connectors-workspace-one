# Salesforce Connector

The Salesforce connector searches Salesforce for Contacts based on the sender's email address and the email address of the Salesforce user.  If the sender is a Contact of the Salesforce user, then an informational card is returned that displays the Contact's name, Account, Phone Number, and if available, information about the associated Opportunity.

If the sender is not a Contact of the Salesforce user, but other Contacts from the sender's email domain are, then a card is returned offering the create a new Contact in Salesforce, and if desired, associate it with an opportunity that already has Contacts with that email domain.

For generic details on how to build, install, and configure connectors, please see the [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md) at the root of this repository.

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
* The value of the header 'Authorization' is the vidm oauth token for accessing Mobile Flows Api Gateway.

### Create an Account

```shell
curl https://sf.sandbox.host/services/data/v20.0/sobjects/Account/ \
     -H "Authorization: Bearer <token generated from Step 3>" \
     -H "Content-Type: application/json" \
     -d '{"Name":"<your account name>"}'
```
### References

* [Force.com REST API Developer Guide Quick Start](https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/quickstart.htm)
