# Hub Jira Service Desk Connector

Hub Jira Service Desk connector allows a user to view, approve, or decline Approvals in their Jira Service Desk approval queue.

## Discovery
The discovery URL is of the form:

    https://base_url/

## Card Response

The sample card looks something like this:

        {
            "objects": [
                {
                    "image": {
                        "href": "https://s3.amazonaws.com/vmw-mf-assets/connector-images/jira-service-desk.png"
                    },
                    "body": {
                        "fields": [
                            {
                                "type": "COMMENT",
                                "title": "Description",
                                "description": "I need an iPhone XS Max 256GB for testing the 2020 iOS14 alpha."
                            },
                            {
                                "type": "GENERAL",
                                "title": "Reporter",
                                "description": "David Customer"
                            },
                            {
                                "type": "GENERAL",
                                "title": "Request Type",
                                "description": "New mobile device"
                            },
                            {
                                "type": "GENERAL",
                                "title": "Status",
                                "description": "Waiting for approval"
                            },
                            {
                                "type": "GENERAL",
                                "title": "Date Created",
                                "description": "Today 3:32 PM"
                            }
                        ],
                        "description": "https://{jsd_instance}.atlassian.net/servicedesk/customer/portal/1/FSDP-41"
                    },
                    "actions": [
                        {
                            "action_key": "DIRECT",
                            "id": "41d5db66-eb92-42be-930e-ca1bc7859f1e",
                            "user_input": [],
                            "request": {
                                "decision": "approve",
                                "issueKey": "FSDP-41"
                            },
                            "repeatable": false,
                            "primary": true,
                            "label": "Approve",
                            "completed_label": "Approved",
                            "type": "POST",
                            "url": {
                                "href": "https://prod.hero.vmwservices.com/hero/connectors/{connectorid}/card/jiraservicedesk/actions"
                            }
                        },
                        {
                            "action_key": "DIRECT",
                            "id": "4b9e26e7-88a8-4205-a692-1724c25799b9",
                            "user_input": [],
                            "request": {
                                "decision": "decline",
                                "issueKey": "FSDP-41"
                            },
                            "repeatable": false,
                            "primary": false,
                            "label": "Decline",
                            "completed_label": "Declined",
                            "type": "POST",
                            "url": {
                                "href": "https://prod.hero.vmwservices.com/hero/connectors/{connectorid}/card/jiraservicedesk/actions"
                            }
                        }
                    ],
                    "id": "{connectorid}",
                    "backend_id": "FSDP-41",
                    "hash": "sDsVlXMKsWovzN2JdniE+uQO75tXx0W4gMGBpQRrzGk=",
                    "header": {
                        "title": "iPhone XS Max ",
                        "subtitle": [
                            "FSDP-41"
                        ]
                    }
                }
            ]
        }

Each card has two actions -- `Approve`, or `Decline`

For generic details on how to build, install, and configure connectors, please see the [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md) at the root of this repository.
