/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const port = process.env.PORT || 4000;
const express = require('express');
const app = express();
const commandLineArgs = require('command-line-args');
const discovery = require('./routes/discovery');
const servicenow = require('./routes/servicenow');
const vIdm = require('./vidm');

const optionDefinitions = [
    {
        name: 'vIdmPubKeyUrl',
        type: String
    }
];

const options = commandLineArgs(optionDefinitions);

app.use(express.json());
app.use(express.urlencoded({extended: true}));
app.use(express.static('public'))

app.set('trust proxy', true);

app.use(
    [
        '/cards/requests',
        '/api/v1/tickets/:requestSysId/approve',
        '/api/v1/tickets/:requestSysId/reject'
    ],
    (req, res, next) => {

        const authorization = req.header('authorization');

        if (authorization) {
            vIdm.verifyAuth(authorization, options).then(function (decoded) {
                // Everything was good, let them pass through
                res.locals.jwt = decoded;
                next()
            }).catch(function (err) {
                console.log('vIdm verification failed:', err);
                res.status(401).json({message: 'vIdm verification failed!'});
            });
        } else {
            res.status(401).send({message: 'Missing Authorization header'});
        }

    }
);

/*
 * Note that the "/connectors/{connectorId}/" prefix is required by Hero to
 * route to this microservice, but "/cards/requests", "/approve", and "/reject"
 * are what we define here.
 */

// For the client to be able to discover this endpoint:
app.get('/', discovery.root);

// For the client to request cards:
app.post('/cards/requests', servicenow.requestCards);

// For the client to later issue actions on those cards:
app.post('/api/v1/tickets/:requestSysId/approve', servicenow.approve);
app.post('/api/v1/tickets/:requestSysId/reject', servicenow.reject);


app.listen(
    port,
    () => {
        console.log('ServiceNow connector listening on port ', port);
    }
);
