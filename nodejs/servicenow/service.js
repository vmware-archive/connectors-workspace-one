/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const {log, logReq, urlPrefix} = require('./util');
const uuid = require('uuid/v4');
const rp = require('request-promise');
const normalizeUrl = require('normalize-url');
const sha1 = require('sha1');

function handleError(res, data, err) {
    logReq(res, 'Something went wrong:', err);
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
 * Called into by the Mobile Flows server on behalf of the client to display
 * cards for requests in the UI.  The auth header and base url header are
 * provided by the Mobile Flows server (details of them determined by the
 * settings an admin configured for the connector in UEM).
 *
 * This function will generate the card objects for the client by looking for
 * all tickets the user can approve/reject.
 *
 * With the user's sys_id, we look up their pending request for approvals (
 * state=requested and approver=our_user_sys_id) and look up those records'
 * request numbers.
 *
 * After we have all of that information, we generate cards in the structure
 * that the client expects.  These cards will include 2 actions -- approve
 * and reject -- that will have enough information encoded into their URLs
 * so the client can call back into our approve and reject REST calls
 * (potentially passing in extra information -- the reject reason for example).
 */
function requestCards(req, res) {
    const auth = req.headers['x-connector-authorization']
    const baseUrl = res.locals.xBaseUrl;
    const email = res.locals.email;
    const routingPrefix = req.headers['x-routing-prefix'];
    const imageUrl = normalizeUrl(`${urlPrefix(req)}/images/connector.png`)

    logReq(res, 'requestCards: routingPrefix=%s', routingPrefix);

    retrieveUserSysId(res, auth, baseUrl, email)
        .then(userSysId => retrieveApprovalRequests(res, auth, baseUrl, userSysId))
        .then(approvalRequests => retrieveRequestData(res, auth, baseUrl, approvalRequests))
        .then(requestData => retrieveRequestedItems(res, auth, baseUrl, requestData))
        .then(results => results.map(data => transformResultsIntoCards(baseUrl, imageUrl, routingPrefix, data)))
        .then(cards => res.json({cards: cards}))
        .catch(err => handleError(res, {message: 'Failed to generate cards', err: err}, err));
}

function retrieveUserSysId(res, auth, baseUrl, email) {
    logReq(res, 'retrieveUserSysId');

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

    return rp(options)
        .then(data => data.result[0].sys_id);
}

function retrieveApprovalRequests(res, auth, baseUrl, userSysId) {
    logReq(res, 'retrieveApprovalRequests: userSysId=%s', userSysId);

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

    return rp(options)
        .then(data => data.result);
}

function retrieveRequestData(res, auth, baseUrl, approvalRequests) {
    logReq(res, 'retrieveRequestData: approvalRequests.length=%s', approvalRequests.length);

    // Loop through and queue up multiple calls to look up the request numbers (ex. REQ0010001)
    let requestNumberPromises = approvalRequests.map(request => {
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

        logReq(res, 'calling out for sc_request: approvalSysId=%s', approvalSysId);

        return rp(options)
            .then(data => ({
                sys_id: request.sys_id,
                sys_approval_id: approvalSysId,
                comments: request.comments,
                due_date: request.due_date,
                sys_created_by: request.sys_created_by,
                price: data.result.price,
                number: data.result.number
            }));
    });

    return Promise.all(requestNumberPromises);
}

function retrieveRequestedItems(res, auth, baseUrl, requests) {
    logReq(res, 'retrieveRequestedItems: requests.length=%s', requests.length);

    const itemsPromises = requests.map(req => {
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

        return rp(options)
            .then(data => {
                req.items = data.result;
                return req;
            });
    });

    return Promise.all(itemsPromises);
}

function toCardHash(result) {
    const itemsHash = result.items.map(r => `id:${r.sys_id}:qty:${r.quantity}`).join(';');
    return sha1(`v1:id:${result.number}:items:${itemsHash}`);
}

function transformResultsIntoCards(baseUrl, imageUrl, routingPrefix, result) {
    const textItems = result.items.map(item => ({
        text: `${item.short_description} - ${item.quantity} @ $${item.price}`
    }));

    const ticketUrl = `${baseUrl}/sysapproval_approver.do?sys_id=${result.sys_approval_id}`;

    return {
        id: uuid(),
        creation_date: new Date().toISOString(),
        backend_id: result.number,
        hash: toCardHash(result),
        image: {
            href: imageUrl
        },
        name: 'ServiceNow',
        header: {
            title: '[Service Now] Approval Request',
            subtitle: [
                `${result.number}`
            ],
            links: {
                title: ticketUrl,
                subtitle: [
                    ticketUrl
                ]
            }
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
                remove_card_on_completion: true,
                action_key: 'DIRECT',
                url: {
                    href: routingPrefix + 'api/tickets/' + result.sys_id + '/approve'
                },
                type: 'POST',
                request: {},
                user_input: []
            },
            {
                id: uuid(),
                label: 'Reject',
                completed_label: 'Rejected',
                remove_card_on_completion: true,
                action_key: 'USER_INPUT',
                url: {
                    href: routingPrefix + 'api/tickets/' + result.sys_id + '/reject'
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
 * This will be called by the Mobile Flows server on behalf of the client when
 * a user clicks the Approve button in their card.
 *
 * The requestSysId was previously encoded into the url for the Approve button
 * by our requestCards call and the auth header and base url header are
 * provided by the Mobile Flows server (details of them determined by the
 * settings an admin configured for the connector in UEM).
 */
function approve(req, res) {
    const auth = req.headers['x-connector-authorization']
    const baseUrl = res.locals.xBaseUrl;
    const requestSysId = req.params.requestSysId;

    logReq(res, 'approve called: requestSysId=%s', requestSysId);

    updateTicketState(auth, baseUrl, requestSysId, 'approved')
        .then(result => res.json(result))
        .catch(err => handleError(res, {message: 'Failed to approve ticket ', requestSysId, err: err}, err));
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

    return rp(options).then(data => {
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
 * This will be called by the Mobile Flows server on behalf of the client when
 * a user clicks the Reject button in their card.
 *
 * The requestSysId was previously encoded into the url for the Reject button
 * by our requestCards call and the auth header and base url header are
 * provided by the Mobile Flows server (details of them determined by the
 * settings an admin configured for the connector in UEM).
 *
 * The reject reason will be provided by the client (which it prompted the user
 * for after they clicked the Reject button).  We instructed the client to do
 * this in our user_input section of the reject action we generated in the
 * requestCards call.
 */
function reject(req, res) {
    const auth = req.headers['x-connector-authorization']
    const baseUrl = res.locals.xBaseUrl;
    const requestSysId = req.params.requestSysId;
    const reason = req.body.reason;

    logReq(res, 'reject called: requestSysId=%s, reason=%s', requestSysId, reason);

    updateTicketState(auth, baseUrl, requestSysId, 'rejected', reason)
        .then(result => res.json(result))
        .catch(err => handleError(res, {message: 'Failed to reject ticket ', requestSysId, err: err}, err));
}

exports.requestCards = requestCards;
exports.approve = approve;
exports.reject = reject;
