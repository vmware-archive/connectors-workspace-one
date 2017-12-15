/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const port = 8080;

const path = require('path');
const express = require('express');
const app = express();

app.use(express.static('public'));
app.use(express.json());
app.use(express.urlencoded({extended: true}));

app.get('/approvals', (req, res) => {
    console.log('GET /approvals: req.query=', req.query);
    // TODO - it actually allows you through with a bad auth code,
    // however, the later approval POST request fails due to the referrer I think:
    // https://us-east-2.certificates.amazon.com/approvals?code=test-auth-code&context=test-context
    if (req.query.code === 'test-auth-code' && req.query.context === 'test-context') {
        res.sendFile(path.join(__dirname, 'public/approval-page.html'));
    } else {
        res.status(404).send('Unexpected code: ' + req.query.code + ' and/or context: ' + req.query.context);
    }
});

app.post('/approvals', (req, res) => {
    console.log('POST /approvals: req.body=', req.body);
    if (req.body.utf8 === '\u2713' &&
        req.body.authenticity_token === 'test-csrf-token' &&
        req.body.validation_token === 'test-validation-token' &&
        req.body.context === 'test-context' &&
        req.body.commit === 'I Approve') {
        res.sendFile(path.join(__dirname, 'public/approval-confirmation-page.html'));
    } else {
        res.status(401).send('Unexpected params: ' + JSON.stringify(req.body));
    }
});

app.listen(port, () => console.log('Fake AWS Certificate backend listening on port ' + port));
