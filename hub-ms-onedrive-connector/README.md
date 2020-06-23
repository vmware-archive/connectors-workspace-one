# Microsoft OneDrive Connector

The Microsoft File/Folder Access Request Approval connector notifies Workspace ONE users of access approval requests for One Drive and SharePoint files and folder inside Workspace ONE. Users see a notification card, showing details of the request such as Requested By, Requested For, and Requested On. In one click, users can approve read/edit access or decline the access request right within Workspace ONE.

When OneDrive and SharePoint users share files and folders with colleagues and if they have limited sharing privileges on those resources, a request for approval is sent to the owner of the files/folder as email. The connector processes such emails received by the resource owner and prepares a notification card showing required details and actions such as Approve Read, Approve Edit and Decline. The connector executes the selected action on the requested resource and sends email notifications about the action to the requestor (Requested By) and to the user with whom the resource needs to be shared (Requested For).

For generic details on how to build, install, and configure connectors, please see the [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md) at the root of this repository.
