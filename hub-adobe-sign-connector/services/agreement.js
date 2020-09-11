/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const fetch = require('node-fetch')
const utils = require('../utils/utils')
const mfCommons = require('@vmw/mobile-flows-connector-commons')

const retrieveAgreements = async (res) => {
    let username = 'email:'+ res.locals.mfJwt.email
    mfCommons.logReq(res, 'Retrieve agreement details for user %s',username)

    const options = {
        headers: {
            'Authorization':res.locals.backendAuthorization,
            'x-api-user': username
        }
    }
    return fetch(`${res.locals.backendBaseUrl}/api/rest/v6/agreements`, options)
        .then(utils.withExceptionForHttpError)
        .then(response => response.json())
}

const getAgreementDetail = async (res, agreementId) =>{
    const options = {
        headers: {
            Authorization:res.locals.backendAuthorization,
        }
    }
    return fetch(`${res.locals.backendBaseUrl}/api/rest/v6/agreements/${agreementId}`, options)
        .then(utils.withExceptionForHttpError)
        .then(response => response.json())
}

const getSigningUrl = async (res, agreementId) =>{
    const options = {
        headers: {
            Authorization:res.locals.backendAuthorization,
        }
    }
    return fetch(`${res.locals.backendBaseUrl}/api/rest/v6/agreements/${agreementId}/signingUrls`, options)
        .then(utils.withExceptionForHttpError)
        .then(response => response.json())
}

const getMember = async (res, agreementId) =>{
    const options = {
        headers: {
            Authorization:res.locals.backendAuthorization,
        }
    }
    return fetch(`${res.locals.backendBaseUrl}/api/rest/v6/agreements/${agreementId}/members`, options)
        .then(utils.withExceptionForHttpError)
        .then(response => response.json())
}

const retrieveDocument = async (res, agreementId) => {
    let documentUrl
    documentUrl = `${res.locals.backendBaseUrl}/api/rest/v6/agreements/${agreementId}/combinedDocument`
    const options = {
        headers: {
            Authorization:res.locals.backendAuthorization,
        }
    }
    mfCommons.logReq(res, 'Document Url: %s', documentUrl)
    return  fetch(documentUrl, options)
        .then(response => response.buffer())
}



module.exports = {
    retrieveAgreements,
    getAgreementDetail,
    getSigningUrl,
    retrieveDocument,
    getMember
}

