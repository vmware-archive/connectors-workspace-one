/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const {
    httpMethods,
    cardFieldTypes,
    cardActionKeys,
    CardBuilder,
    CardBodyBuilder,
    CardFieldBuilder,
    CardActionBuilder,
    CardActionInputFieldBuilder
} = require('../cards-builders');

const uuid = require('uuid/v4');
const rp = require('request-promise');

function log(format, ...args) {
    console.log('%s - ' + format, new Date().toISOString(), ...args);
}

function handleError(res, data, err) {
    let errCode = 500;
    if (err.statusCode) {
        res.header('X-Backend-Status', err.statusCode);

        if (err.statusCode === 401) {
            errCode = 400;
        }
    }
    res.status(errCode).json(data);
}

/**
 * Called into by the Boxer client to display cards for requests in the UI.
 *
 * This function will generate the card objects for the client by taking in
 * the ticket_id parameter passed in by the client (that was determined by
 * running the regex from this project's metadata.json -- found by HAL
 * discovery and applying it to the Boxer email).  The ticket_id is an array
 * of the request numbers (ex. REQ0010001).
 *
 * With the user's sys_id, we look up their pending request for approvals (
 * state=requested and approver=our_user_sys_id) and look up those records'
 * request numbers to later filter only the results that match the ticket_id
 * passed in by the client.
 *
 * After we have all of that information, we generate cards in the structure
 * that the client expects.  These cards will include 2 actions -- approve
 * and reject -- that will have enough information encoded into their URLs
 * so the client can call back into our /approve and /reject REST calls (
 * potentially passing in extra information -- the reject reason for example).
 */
function requestCards(req, res) {
    const auth = req.headers['x-servicenow-authorization']
    const baseUrl = req.headers['x-servicenow-base-url'];
    const routingPrefix = req.headers['x-routing-prefix'];
    const cardRequest = req.body;
    const principal = res.locals.jwt.prn;

    log('requestCards: baseUrl=%s, routingPrefix=%s, user=%s, cardRequest=', baseUrl, routingPrefix, principal, cardRequest);

    if (!req.body.tokens) {
        res.status(400).json({message: 'tokens is required'});
        return;
    }

    const requestNumbers = req.body.tokens.ticket_id;

    if (!requestNumbers || !requestNumbers.length) {
        res.json({cards: []});
        return;
    }

    const emails = req.body.tokens.email;

    if (!emails || !emails.length) {
        res.json({cards: []});
        return;
    }

    retrieveUserSysId(auth, baseUrl, emails[0]).then(function (userSysId) {
        return retrieveApprovalRequests(auth, baseUrl, userSysId);
    }).then(function (approvalRequests) {
        return retrieveRequestData(auth, baseUrl, requestNumbers, approvalRequests);
    }).then(function (requestData) {
        log('all calls to sc_request after filtering away unnecessary: requestData.length=%s', requestData.length);

        return retrieveRequestedItems(auth, baseUrl, requestData);
    }).then(function (results) {
        return results.map(function (data) {
            // return transformResultsIntoCardsUsingBuilder(routingPrefix, data);
            return transformResultsIntoCardsUsingSimpleJson(routingPrefix, data);
        });
    }).then(function (cards) {
        res.json({cards: cards});
    }).catch(function (err) {
        handleError(res, {message: 'Failed to generate cards', err: err}, err);
    });
}

function retrieveUserSysId(auth, baseUrl, email) {
    log('retrieveUserSysId: baseUrl=%s, email=%s', baseUrl, email);

    const options = {
        json: true,
        headers: {
            'Authorization': auth
        },
        url: baseUrl + '/api/now/table/sys_user',
        qs: {
            sysparm_fields: 'sys_id',
            sysparm_limit: 1,
            email: email // TODO - this is potentially flawed, ServiceNow doesn't enforce unique emails
        }
    };

    return rp(options).then(function (data) {
        return data.result[0].sys_id;
    });
}

function retrieveApprovalRequests(auth, baseUrl, userSysId) {
    log('retrieveApprovalRequests: baseUrl=%s, userSysId=%s', baseUrl, userSysId);

    const options = {
        json: true,
        headers: {
            'Authorization': auth
        },
        url: baseUrl + '/api/now/table/sysapproval_approver',
        qs: {
            sysparm_fields: 'sys_id,sysapproval,comments,due_date,sys_created_by',
            sysparm_limit: 10000,
            source_table: 'sc_request',
            state: 'requested',
            approver: userSysId
        }
    };

    return rp(options).then(function (data) {
        return data.result; // pull out the multiple approval requests from the {result: [...]} response
    });
}

function retrieveRequestData(auth, baseUrl, requestNumbers, approvalRequests) {
    log('retrieveRequestData: baseUrl=%s, requestNumbers=%s, approvalRequests.length=%s', baseUrl, requestNumbers, approvalRequests.length);

    // Loop through and queue up multiple calls to look up the request numbers (ex. REQ0010001)
    let requestNumberPromises = approvalRequests.map(function (request) {
        const approvalSysId = request.sysapproval.value;
        const options = {
            json: true,
            headers: {
                'Authorization': auth
            },
            url: baseUrl + '/api/now/table/sc_request/' + approvalSysId,
            qs: {
                 sysparm_fields: 'sys_id,price,number'
            }
        };

        log('calling out for sc_request: baseUrl=%s, approvalSysId=%s', baseUrl, approvalSysId);

        return rp(options).then(function (data) {
            return {
                sys_id: request.sys_id,
                sys_approval_id: approvalSysId,
                comments: request.comments,
                due_date: request.due_date,
                sys_created_by: request.sys_created_by,
                price: data.result.price,
                number: data.result.number
            };
        });
    });

    // After all of the request number lookups, filter out the results the client isn't interested in
    return Promise.all(requestNumberPromises).then(function (results) {
        log('all calls to sc_request returned: results.length=%s', results.length);

        return results.filter(function (result) {
            return requestNumbers.includes(result.number);
        });
    });
}

function retrieveRequestedItems(auth, baseUrl, requests) {
    log('retrieveRequestedItems: baseUrl=%s, requests.length=%s', baseUrl, requests.length);

    const itemsPromises = requests.map(function (req) {
        const options = {
            json: true,
            headers: {
                'Authorization': auth
            },
            url: baseUrl + '/api/now/table/sc_req_item',
            qs: {
                sysparm_fields: 'sys_id,price,request,short_description,quantity',
                sysparm_limit: 10000,
                request: req.sys_approval_id
            }
        };

        return rp(options).then(function (data) {
            req.items = data.result;
            return req;
        });
    });

    return Promise.all(itemsPromises);
}

function transformResultsIntoCardsUsingBuilder(routingPrefix, result) {
    const lineItems = new CardFieldBuilder()
        .setType(cardFieldTypes.GENERAL)
        .setTitle('Items');

    result.items.forEach(function (item) {
        lineItems.addTextContent(`${item.short_description} - ${item.quantity} @ $${item.price}`);
    });

    return new CardBuilder()
        .setHeader(`Approval Request - ${result.number}`)
        .setBody(
            new CardBodyBuilder()
                .addField(
                    new CardFieldBuilder()
                        .setType(cardFieldTypes.GENERAL)
                        .setTitle('Total Price')
                        .setDescription(`$${result.price}`)
                        .build()
                )
                .addField(
                    new CardFieldBuilder()
                        .setType(cardFieldTypes.GENERAL)
                        .setTitle('Requester')
                        .setDescription(result.sys_created_by)
                        .build()
                )
                .addField(
                    new CardFieldBuilder()
                        .setType(cardFieldTypes.GENERAL)
                        .setTitle('Due By')
                        .setDescription(result.due_date)
                        .build()
                )
                .addField(lineItems.build())
                .build()
        )
        .addAction(
            new CardActionBuilder()
                .setLabel('Approve')
                .setCompletedLabel('Approved')
                .setActionKey(cardActionKeys.DIRECT)
                .setUrl(`${routingPrefix}api/v1/tickets/${result.sys_id}/approve`)
                .setType(httpMethods.POST)
                .build()
        )
        .addAction(
            new CardActionBuilder()
                .setLabel('Reject')
                .setCompletedLabel('Rejected')
                .setActionKey(cardActionKeys.USER_INPUT)
                .setUrl(`${routingPrefix}api/v1/tickets/${result.sys_id}/reject`)
                .setType(httpMethods.POST)
                .addUserInputField(
                    new CardActionInputFieldBuilder()
                        .setId('reason')
                        .setLabel('Reason for rejection')
                        .setMinLength(1)
                        .build()
                )
                .build()
        )
        .build();
}

function transformResultsIntoCardsUsingSimpleJson(routingPrefix, result) {
    const textItems = result.items.map(function (item) {
        return {
            text: `${item.short_description} - ${item.quantity} @ $${item.price}`
        };
    });

    return {
        id: uuid(),
        creation_date: new Date().toISOString(),
        name: 'ServiceNow',
        header: {
            title: `Approval Request - ${result.number}`
        },
        body: {
            fields: [
                {type: 'GENERAL', title: 'Total Price', description: `$${result.price}`},
                {type: 'GENERAL', title: 'Requester', description: result.sys_created_by},
                {type: 'GENERAL', title: 'Due By', description: result.due_date},
                {type: 'COMMENT', title: 'Items', content: textItems}
            ]
        },
        actions: [
            {
                id: uuid(),
                primary: true,
                label: 'Approve',
                completed_label: 'Approved',
                action_key: 'DIRECT',
                url: {
                    href: routingPrefix + 'api/v1/tickets/' + result.sys_id + '/approve'
                },
                type: 'POST',
                request: {},
                user_input: []
            },
            {
                id: uuid(),
                label: 'Reject',
                completed_label: 'Rejected',
                action_key: 'USER_INPUT',
                url: {
                    href: routingPrefix + 'api/v1/tickets/' + result.sys_id + '/reject'
                },
                type: 'POST',
                request: {},
                user_input: [
                    {id: 'reason', label: 'Reason for rejection', min_length: 1}
                ]
            }
        ]
    };
}

/**
 * Approve the ServiceNow request.
 *
 * This will be called by the client when a user clicks the Approve button in
 * their card.
 *
 * The requestSysId was previously encoded into the url for the Approve button
 * by our requestCards call and the auth header and base url header are
 * provided by the client because we told it to in our metadata.json that it
 * found during HAL discovery (and when setting up a tenant connector config).
 */
function approve(req, res) {
    const auth = req.headers['x-servicenow-authorization']
    const baseUrl = req.headers['x-servicenow-base-url'];
    const requestSysId = req.params.requestSysId;

    log('approve called: baseUrl=%s, requestSysId=%s', baseUrl, requestSysId);

    updateTicketState(auth, baseUrl, requestSysId, 'approved').then(function (result) {
        res.json(result);
    }).catch(function (err) {
        handleError(res, {message: 'Failed to approve ticket ', requestSysId, err: err}, err);
    });
}

function updateTicketState(auth, baseUrl, requestSysId, state, comments) {

    const body = {
        state: state
    };

    if (comments) {
        body.comments = comments;
    }

    const options = {
        method: 'PATCH',
        body: body,
        json: true,
        headers: {
            'Authorization': auth
        },
        url: baseUrl + '/api/now/table/sysapproval_approver/' + requestSysId,
        qs: {
            sysparm_fields: 'sys_id,state,comments'
        }
    };

    return rp(options).then(function (data) {
        /*
         * I'm just transforming the data here so it's easier to distinguish
         * what is coming from where.  So if I test this in curl, it's obvious
         * that I made it through to my transformation instead of being unsure
         * if I've only sent back something from ServiceNow, or accidentally
         * short-circuited something.  In this case, the results of the
         * approve/reject REST calls aren't used by the client, so it is only
         * useful for debugging purposes.
         */
        return {
            approval_sys_id: data.result.sys_id,
            approval_state: data.result.state,
            approval_comments: data.result.comments
        };
    });
}

/**
 * Reject the ServiceNow request.
 *
 * This will be called by the client when a user clicks the Reject button in
 * their card.
 *
 * The requestSysId was previously encoded into the url for the Reject button
 * by our requestCards call and the auth header and base url header are
 * provided by the client because we told it to in our metadata.json that it
 * found during HAL discovery (and when setting up a tenant connector config).
 *
 * The reject reason will be provided by the client (which it prompted the user
 * for after they clicked the Reject button).  We instructed the client to do
 * this in our user_input section of the reject action we generated in the
 * requestCards call.
 */
function reject(req, res) {
    const auth = req.headers['x-servicenow-authorization']
    const baseUrl = req.headers['x-servicenow-base-url'];
    const requestSysId = req.params.requestSysId;
    const reason = req.body.reason;

    log('reject called: baseUrl=%s, requestSysId=%s, reason=%s', baseUrl, requestSysId, reason);

    updateTicketState(auth, baseUrl, requestSysId, 'rejected', reason).then(function (result) {
        res.json(result);
    }).catch(function (err) {
        handleError(res, {message: 'Failed to reject ticket ', requestSysId, err: err}, err);
    });
}


exports.requestCards = requestCards;
exports.approve = approve;
exports.reject = reject;
