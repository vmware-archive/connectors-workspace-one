/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const port = process.env.PORT || 4001;
const express = require('express');
const app = express();
const discovery = require('./routes/discovery');
const airwatch = require('./routes/airwatch');
const vIdm = require('./vidm');
const options = require('./config/command-line-options');

app.use(express.json());
app.use(express.urlencoded({extended: true}));

app.set('trust proxy', true);

app.use(
    [
        '/cards/requests',
        '/mdm/app/install'
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
 * route to this microservice, but "/cards/requests" and "/installApp" are
 * what we define here.
 */

// For the client to be able to discover this endpoint:
app.get('/', discovery.root);
app.get('/discovery/metadata.json', discovery.metadata);

// For the client to request cards:
app.post('/cards/requests', airwatch.requestCards);

// For the client to later issue actions on those cards:
app.post('/mdm/app/install', airwatch.installApp);


app.listen(
    port,
    () => {
        console.log('AirWatch connector listening on port ', port);
    }
);
