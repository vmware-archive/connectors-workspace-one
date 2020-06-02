# Chatbot ServiceNow Connector

The Chatbot ServiceNow Connector allows user to directly connect to ServiceNow via Hub Assistant. It helps to have a conversational dialog for the following workflows 
1. File a general ticket.
2. View open tickets.
3. Order a device

Admin can configure type of the general ServiceNow ticket. (Example - "incident", "problem", "ticket" etc)
Chatbot user is asked to provide a short description, needed to file the ticket.

The second flow is to support user queries like "What are my open tickets ?". The connector looks for most recent open tickets
that are created by the user. (User is identified by user's email id.) Connector generates objects for each ticket. 
Object contains details of the ticket like current status etc.

The third flow is to support ordering a device. It enables users order a device by just talking to a chatbot.

For generic details on how to build, install, and configure connectors, please check [README.md](https://github.com/vmware/connectors-workspace-one/blob/master/README.md). 
