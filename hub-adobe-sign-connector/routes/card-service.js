/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const agreement = require('../services/agreement')
const cardObject = require('../utils/card-objects')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
exports.getCardObject = async (req, res) => {
    let getAgreementsForUser
    let filteredAgreements
    let card = []
    let cardObjects
    if (!res.locals.backendBaseUrl) {
        return res.status(400).json({ message: 'Backend API base URL is required' })
    }
    try {
        getAgreementsForUser =  await agreement.retrieveAgreements(res)
        filteredAgreements = getAgreementsForUser.userAgreementList.filter(function (agreement) {
        return agreement.status === "WAITING_FOR_MY_SIGNATURE"
        })
        card = await cardObject.agreementToCards(res, filteredAgreements, res.locals.mfRoutingPrefix);
    }
    catch (e) {
        return await handleAdobeError(res, e, 'Failed to retrive agreement details')
    }
    cardObjects = {
        objects: card
    }
    return res.json(cardObjects)
}


const handleAdobeError = async (res, error, message = 'Adobe API has failed.') => {
    mfCommons.logReq(res, message)
    let responseBody
    if (error.status) {
        responseBody = await error.response.text()
        mfCommons.logReq(res, error.message + ', ' + responseBody)
    }
    if(error.status === 401){
        //Access token provided is invalid or has expired
        return res
            .status(400)
            .header('X-Backend-Status', 401)
            .json({ error: error.message })
    }else{
        return res
            .status(500)
            .header('X-Backend-Status', error.status)
            .json({ error: error.message })
    }
}


