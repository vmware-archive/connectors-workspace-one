/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const managedApps = require('../config/managed-apps');
const _ = require('lodash');
const uuid = require('uuid/v4');
const rp = require('request-promise');
const options = require('../config/command-line-options');
const greenBoxUrl = options.greenBoxUrl;

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
 * TODO - write up a description
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
    const auth = req.headers['authorization']
    const baseUrl = req.headers['x-airwatch-base-url'];
    const routingPrefix = req.headers['x-routing-prefix'];
    const cardRequest = req.body;
    const principal = res.locals.jwt.prn;

    log('requestCards: baseUrl=%s, routingPrefix=%s, user=%s, cardRequest=', baseUrl, routingPrefix, principal, cardRequest);

    if (!req.body.tokens) {
        res.status(400).json({message: 'tokens is required'});
        return;
    }

    const udids = req.body.tokens.udid;
    if (!udids || !udids.length) {
        res.status(400).json({message: 'udid is required'});
        return;
    }
    const udid = udids[0];

    const clientPlatforms = req.body.tokens.platform;
    if (!clientPlatforms || !clientPlatforms.length) {
        res.status(400).json({message: 'platform is required'});
        return;
    }
    const clientPlatform = clientPlatforms[0].toLowerCase();

    const appKeywords = req.body.tokens.app_keywords;
    if (!appKeywords) {
        res.status(400).json({message: 'app_keywords is required'});
        return;
    }
    if (!appKeywords.length) {
        res.json({cards: []});
        return;
    }

    // loop through the config to find the "managed apps" based on platform and app keywords

    // log('Getting app installation status for bundleId: %s with air-watch base url: %s", app.id, baseUrl);
    // GET baseUrl + '/deviceservices/AppInstallationStatus?Udid=${udid}&BundleId=${app.id}'
    // Authorization header is the same as the vIdm auth header
    // maybe do the weird error handling that Shree did (string.contains): I need to test against a real instance to see if we can clean that up
    // if (result.IsApplicationInstalled === false) { make a non-empty card } // (undefined/null implies true)
    // TODO - build the card

    const promises = managedApps.airwatch.apps.filter(app => {
        return app[clientPlatform] && _.intersection(app.keywords, appKeywords).length;
    }).map(app => {
        const appBundle = app[clientPlatform].id;
        const appName = app[clientPlatform].name;
        log('Getting app installation status for bundleId: %s with air-watch base url: %s', appBundle, baseUrl);
        const options = {
            json: true,
            headers: {
                'Authorization': auth
            },
            url: baseUrl + '/deviceservices/AppInstallationStatus',
            qs: {
                Udid: udid,
                BundleId: appBundle
            }
        };
        return rp(options).then(result => {
            return {
                appName: appName,
                appBundle: appBundle,
                isInstalled: result.IsApplicationInstalled !== false
            };
            // `User is not associated with the UDID : ${udid}`
            // `Unable to resolve the UDID : ${udid}`
        }); // TODO - catch here? (the error handling of Error:1001 is user forbidden, Error:1002 is udid not found)
    });

    return Promise.all(promises).then(apps => {
        const cards = apps.map(app => {
            if (app.IsApplicationInstalled) {
                return;
            }
            return {
                id: uuid(),
                creation_date: new Date().toISOString(),
                header: {
                    title: `[AirWatch] ${app.appName}`
                },
                body: {
                    description: 'Please install this application for better interaction with the resource.'
                },
                actions: [
                    {
                        id: uuid(),
                        label: 'Install',
                        completed_label: 'Installed',
                        action_key: 'DIRECT',
                        url: {
                            href: routingPrefix + 'mdm/app/install'
                        },
                        type: 'POST',
                        request: {
                            app_name: app.appName,
                            udid: udid,
                            platform: clientPlatform
                        },
                        user_input: []
                    }
                ]
            };
        }).filter(card => {
            return !!card; // filter away undefined (already installed)
        });

        return res.json({cards: cards});
    });
}


/**
 * TODO - write up something for this
 *
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
function installApp(req, res) {
    const auth = req.headers['authorization']
    const baseUrl = req.headers['x-airwatch-base-url'];
    const appName = req.body.app_name;
    const udid = req.body.udid;
    const platform = req.body.platform.toLowerCase();

    log('installApp called: baseUrl=%s, appName=%s, udid=%s, platform=%s', baseUrl, appName, udid, platform);

    const app = managedApps.airwatch.apps.find(app => app[platform].name === appName);

    if (!app) {
        return res.statusCode(400).json({message: `Can't install ${appName}. It is not a managed app.`});
    }

    const authToken = auth.replace('Bearer ', '');

    const deviceType = platform === 'ios' ? 'Apple' : platform;

    const eucTokenOptions = {
        json: true,
        method: 'POST',
        headers: {
            'Cookie': `HZN=${authToken}`
        },
        url: baseUrl + '/catalog-portal/services/auth/eucTokens',
        qs: {
            deviceUdid: udid,
            deviceType: deviceType
        }
    };

    return rp(eucTokenOptions).then(result => {
        return result.eucToken;
    }).then(eucToken => {
        const csrfTokenOptions = {
            resolveWithFullResponse: true,
            method: 'OPTIONS',
            headers: {
                'Cookie': `USER_CATALOG_CONTEXT=${eucToken}`
            },
            url: baseUrl + '/catalog-portal/'
        };
        return rp(csrfTokenOptions).then(response => {
            return {
                eucToken,
                csrfToken: response.headers['set-cookie'][0].replace('EUC_XSRF_TOKEN=', '')
            };
        });
    }).then(tokens => {
        const greenBoxAppOptions = {
            json: true,
            headers: {
                'Cookie': `USER_CATALOG_CONTEXT=${tokens.eucToken}`
            },
            url: greenBoxUrl + '/catalog-portal/services/api/entitlements',
            qs: {
                q: appName
            }
        };
        return rp(greenBoxAppOptions).then(results => {
            const entitlements = results._embedded.entitlements;
            if (entitlements.length !== 1) {
                return Promise.reject({message: `Unable to map ${appName} to a single GreenBox app`});
            }
            return {
                tokens,
                greenBoxAppName: entitlements[0].name,
                greenBoxInstallLink: entitlements[0]._links.install.href
            };
        });
    }).then(data => {
        const greenBoxInstallOptions = {
            json: true,
            method: 'POST',
            headers: {
                'Cookie': `USER_CATALOG_CONTEXT=${data.tokens.eucToken};EUC_XSRF_TOKEN=${data.tokens.csrfToken}`,
                'X-XSRF-TOKEN': data.tokens.csrfToken
            },
            url: data.greenBoxInstallLink
        };
        return rp(greenBoxInstallOptions).then(results => {
            log('Install action status: %s for %s', results.status, data.greenBoxAppName);
            return results.status;
        });
    }).then(ignored => res.status(204).end());

}


exports.requestCards = requestCards;
exports.installApp = installApp;
