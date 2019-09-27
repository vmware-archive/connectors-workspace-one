/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

function urlPrefix(req) {
    const proto = req.header('x-forwarded-proto') || 'http';
    const host = req.header('x-forwarded-host');
    const port = req.header('x-forwarded-port');
    const path = req.header('x-forwarded-prefix') || '';

    if (host && port) {
        return `${proto}://${host}:${port}/${path}`
    }

    if (host && !port) {
        return `${proto}://${host}/${path}`
    }

    return `${proto}://${req.headers.host}/${path}`
}

function log(fmt, ...args) {
    console.log(fmt, ...args);
}

function logReq(res, fmt, ...args) {
    const tenant = res.locals.tenant;
    const tenantFmt = tenant ? `[t: ${tenant}] ` : '';
    const username = res.locals.username;
    const usernameFmt = username ? `[u: ${username}] ` : '';
    const domain = res.locals.domain;
    const domainFmt = domain ? `[d: ${domain}] ` : '';
    const email = res.locals.email;
    const emailFmt = email ? `[e: ${email}] ` : '';
    const xReqId = res.locals.xReqId;
    const xReqIdFmt = xReqId ? `[req: ${xReqId}] ` : '';
    const xBaseUrl = res.locals.baseUrl;
    const xBaseUrlFmt = xBaseUrl ? `[base: ${xBaseUrl}] ` : '';
    log(tenantFmt + usernameFmt + domainFmt + emailFmt + xReqIdFmt + xBaseUrlFmt + fmt, ...args);
}

module.exports = {
    log,
    logReq,
    urlPrefix
};
