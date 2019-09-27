/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const port = process.env.PORT || 3801;
const jwt = require('./jwt-validation');
const {log, logReq, urlPrefix} = require('./util');
const {requestCards, approve, reject} = require('./service');
const normalizeUrl = require('normalize-url');
const express = require('express');
const app = express();

function handleAuthorization(req, res, next) {
    const authorization = req.header('authorization');

    if (authorization) {
        jwt.verifyAuth(authorization)
            .then(decoded => {
                // Everything was good, let them pass through
                const index = decoded.prn.lastIndexOf('@');
                res.locals.jwt = decoded;
                res.locals.tenant = decoded.prn.substring(index + 1);
                res.locals.username = decoded.prn.substring(0, index);
                res.locals.domain = decoded.domain;
                res.locals.email = decoded.eml;
                res.locals.xReqId = req.headers['x-request-id']
                res.locals.xBaseUrl = req.headers['x-connector-base-url'];
                next()
            })
            .catch(err => {
                logReq(res, 'Mobile Flows JWT verification failed:', err);
                res.status(401).json({message: 'Mobile Flows JWT verification failed!'});
            });
    } else {
        res.status(401).json({message: 'Missing Authorization header'});
    }
}

function handleDiscovery(req, res) {
    const prefix = urlPrefix(req);
    logReq(res, 'discovery called: prefix=%s', prefix);
    let discoveryResponse = {
        image: {
            href: normalizeUrl(`${prefix}/images/connector.png`)
        },
        actions: {},
        object_types: {
            card: {
                pollable: true,
                endpoint: {
                    href: normalizeUrl(`${prefix}/api/cards/requests`)
                }
            }
        }
    };
    res.json(discoveryResponse);
}

function handleCards(req, res) {
    requestCards(req, res);
}

function handleApprove(req, res) {
    approve(req, res);
}

function handleReject(req, res) {
    reject(req, res);
}

app.use(express.json());
app.use(express.urlencoded({extended: true}));
app.use(express.static('public'))

app.use(['/api/*'], handleAuthorization);

// For the Mobile Flows server to be able to discover this endpoint:
app.get('/', handleDiscovery);

// For the Mobile Flows server to request cards (through polling):
app.post('/api/cards/requests', handleCards);

// For the Mobile Flows server to later issue actions on those cards on behalf of the client:
app.post('/api/tickets/:requestSysId/approve', handleApprove);
app.post('/api/tickets/:requestSysId/reject', handleReject);

app.listen(port, () => log('ServiceNow connector listening on port ', port));
