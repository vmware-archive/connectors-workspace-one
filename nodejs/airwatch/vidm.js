/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const jwt = require('jsonwebtoken');
const rp = require('request-promise');

let pubKeyCache;

function verifyAuth(authorization, options) {

    return getPublicKey(options).then(function (pubKeyContents) {

        return new Promise(function (resolve, reject) {

            // https://github.com/auth0/node-jsonwebtoken
            const jwtOptions = {
                /*
                 * Don't allow 'none' OR HMAC.
                 *
                 * We have decided that a RSA/ECDSA public key will be used and
                 * which key has been set out-of-band.  We cannot allow the
                 * caller to specify non-RSA/ECDSA algorithms or else they can
                 * either specify none or specify HMAC that uses a shared
                 * secret.
                 * Since jwt.verify has the same parameter for shared secret or
                 * public key, this would allow an attacker to specify HMAC alg
                 * signed with a shared secret of the public key contents --
                 * which are not meant to be hidden -- and appear to be valid.
                 *
                 * (It doesn't look like node-jsonwebtoken fixed this security
                 * issue from 2015 yet, but they probably did and the fix just
                 * isn't as obvious as using a different function name or
                 * having a forced algorithm passed in, so I'm going to assume
                 * that the burden is on us to make sure we don't mix pub key
                 * and shared secret algorithms in the algorithms option.)
                 */
                algorithms: [
                    'RS256',
                    'RS384',
                    'RS512',
                    'ES256',
                    'ES384',
                    'ES512'
                ],
                // audience: 'TODO',
                // issuer: 'TODO',
                // subject: 'TODO',
                clockTolerance: 60,
                clockTimestamp: Date.now() / 1000
            };

            const auth = authorization.replace('Bearer ', '').trim();

            jwt.verify(auth, pubKeyContents, jwtOptions, function(err, decoded) {
                if (err) {
                    reject('Failed JWT validation! ' + err);
                } else {
                    resolve(decoded);
                }
            });

        });

    });

}

function getPublicKey(options) {

    if (pubKeyCache && pubKeyCache.expiresAtTime > Date.now()) {
        return Promise.resolve(pubKeyCache.contents);
    }

    return rp(options.vIdmPubKeyUrl).then(function (data) {

        const expiresAtTime = Date.now() + 3600000;

        console.log(
            'Updating pub key cache for url: %s, set to expire around: %s',
            options.vIdmPubKeyUrl,
            new Date(expiresAtTime)
        );

        pubKeyCache = {
            expiresAtTime: expiresAtTime,
            contents: data
        };

        return data;

    }).catch(function (response) {
        return Promise.reject('Failed to retrieve public key! ' + response.statusCode);
    });
}

exports.verifyAuth = verifyAuth;
