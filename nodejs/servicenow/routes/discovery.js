/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const hal = require('halberd');

/**
 * Called into by the Hero Card server to discover the endpoint for the card
 * request and to tell the Boxer client what headers to specify, how to
 * tokenize information, and any other information to send in REST calls.
 */
function root(req, res) {
    const resource = new hal.Resource();
    const base = `${protocol(req)}://${host(req)}`;
    resource.link('metadata', `${base}/discovery/metadata.json`);
    resource.link('cards', `${base}/cards/requests`);
    resource.link('image', `${base}/images/connector.png`);
    res.setHeader('Content-Type', 'application/hal+json');
    res.send(resource.toJSON());
}

function protocol(req) {
    // Express looks for X-Forwarded-Proto
    return req.protocol;
}

function host(req) {
    // request.hostname is broken in Express 4 so we have to deal with the X-Forwarded- headers ourselves
    const forwardedHost = req.headers['x-forwarded-host'];
    const forwardedPort = req.headers['x-forwarded-port'];
    if (forwardedHost && forwardedPort) {
        return forwardedHost + ':' + forwardedPort;
    } else {
        return req.headers.host;
    }
}


exports.root = root;
