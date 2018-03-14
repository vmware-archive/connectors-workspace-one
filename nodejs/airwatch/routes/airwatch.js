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
 * This function will generate the card objects for the client by taking in
 * the platform, udid, and app_keywords parameters passed in by the client
 * (that was determined by running the regex and providing the env vars from
 * this project's metadata.json -- found by HAL discovery and applying it to
 * the Boxer email).  The app_keywords is an array of the keywords that will
 * match configuration for this instance (in managed-apps.yml) to determine
 * which bundle IDs to pass to AirWatch for presenting Install buttons to
 * the user.
 *
 * This connector will check with AirWatch whether the app bundle is already
 * installed or not and will present cards with Install buttons for any of
 * the apps that were determined to be missing on the device.

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

    if (!cardRequest.tokens) {
        return res.status(400).json({message: 'tokens is required'});
    }

    const udids = cardRequest.tokens.udid;
    if (!udids || !udids.length) {
        return res.status(400).json({message: 'udid is required'});
    }
    const udid = udids[0];

    const clientPlatforms = cardRequest.tokens.platform;
    if (!clientPlatforms || !clientPlatforms.length) {
        return res.status(400).json({message: 'platform is required'});
    }
    const clientPlatform = clientPlatforms[0].toLowerCase();

    const appKeywords = cardRequest.tokens.app_keywords;
    if (!appKeywords) {
        return res.status(400).json({message: 'app_keywords is required'});
    }
    if (!appKeywords.length) {
        return res.json({cards: []});
    }

    const promises = managedApps.airwatch.apps.filter(app => app[clientPlatform])
        .filter(app => _.intersection(app.keywords, appKeywords).length > 0)
        .map(app => fetchAppInstallStatus(baseUrl, auth, clientPlatform, udid, app));

    return Promise.all(promises).then(apps => {

        console.log('apps is: ', apps);

        const cards = apps.filter(app => app.isInstalled === false)
            .map(app => makeCard(routingPrefix, clientPlatform, udid, app));

        return res.json({cards: cards});

    }).catch(err => handleError(res, {message: 'Failed to generate cards', err: err}, err));
}

function fetchAppInstallStatus(baseUrl, auth, clientPlatform, udid, app) {
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
    return rp(options).then(result => ({
        appName: appName,
        appBundle: appBundle,
        isInstalled: result.IsApplicationInstalled !== false // undefined implies the app is installed
        // `User is not associated with the UDID : ${udid}`
        // `Unable to resolve the UDID : ${udid}`
    })); // TODO - catch here? (the error handling of Error:1001 is user forbidden, Error:1002 is udid not found)}
}

function makeCard(routingPrefix, clientPlatform, udid, app) {
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
}

/**
 * Install the app to the device.
 *
 * This will be called by the client when a user clicks the Install button in
 * their card.
 *
 * The app_name, udid, and platform were previously put into the request object
 * for the Install button by our requestCards call and the auth header and base
 * url header are provided by the client because we told it to in our
 * metadata.json that it found during HAL discovery (and when setting up a
 * tenant connector config).
 *
 * This action will look up enough information (tokens for cookies) to interact
 * with GreenBox in order to register that the provided app should be installed
 * on the provided device.
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

    fetchEucToken(authToken, udid, deviceType)
        .then(eucToken => fetchCsrfToken(eucToken))
        .then(tokens => fetchEntitlements(appName, tokens))
        .then(data => greenBoxInstall(data))
        .then(ignored => res.status(204).end())
        .catch(err => handleError(res, {message: 'Failed to install app'}, err));
}

function fetchEucToken(authToken, udid, deviceType) {
    const options = {
        json: true,
        method: 'POST',
        headers: {
            'Cookie': `HZN=${authToken}`
        },
        url: greenBoxUrl + '/catalog-portal/services/auth/eucTokens',
        qs: {
            deviceUdid: udid,
            deviceType: deviceType
        }
    };

    return rp(options).then(result => result.eucToken);
}

function fetchCsrfToken(eucToken) {
    const options = {
        resolveWithFullResponse: true,
        method: 'OPTIONS',
        headers: {
            'Cookie': `USER_CATALOG_CONTEXT=${eucToken}`
        },
        url: greenBoxUrl + '/catalog-portal/'
    };
    return rp(options).then(response => ({
        eucToken,
        csrfToken: response.headers['set-cookie'][0].replace('EUC_XSRF_TOKEN=', '')
    }));
}

function fetchEntitlements(appName, tokens) {
    const options = {
        json: true,
        headers: {
            'Cookie': `USER_CATALOG_CONTEXT=${tokens.eucToken}`
        },
        url: greenBoxUrl + '/catalog-portal/services/api/entitlements',
        qs: {
            q: appName
        }
    };
    return rp(options).then(results => {
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
}

function greenBoxInstall(data) {
    const options = {
        json: true,
        method: 'POST',
        headers: {
            'Cookie': `USER_CATALOG_CONTEXT=${data.tokens.eucToken};EUC_XSRF_TOKEN=${data.tokens.csrfToken}`,
            'X-XSRF-TOKEN': data.tokens.csrfToken
        },
        url: data.greenBoxInstallLink
    };
    return rp(options).then(results => {
        log('Install action status: %s for %s', results.status, data.greenBoxAppName);
        return results.status;
    });
}

exports.requestCards = requestCards;
exports.installApp = installApp;
