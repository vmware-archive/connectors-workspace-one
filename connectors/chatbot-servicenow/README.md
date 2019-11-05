# Chatbot ServiceNow Connector

The Chatbot ServiceNow connector has ticket related flows, to support workflows from a chatbot. 
1. File a general ticket.
2. View open tickets.

Admin can configure type of the general ServiceNow ticket. (Example - "incident", "problem", "ticket" etc)
Chatbot user is asked to provide a short description, needed to file the ticket.

The second flow is to support user queries like "What are my open tickets ?". The connector looks for most recent open tickets
that are created by the user. (User is identified by user's email id.) Connector generates objects for each ticket. 
Object contains details of the ticket like current status etc.



For generic details on how to build, install, and configure connectors, please see the [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md) at the root of this repository.
