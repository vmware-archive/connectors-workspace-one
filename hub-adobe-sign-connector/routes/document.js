/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const agreement = require('../services/agreement')
const mfCommons = require('@vmw/mobile-flows-connector-commons')

exports.getDocument = async (req, res) => {
    let documentBuffer
    let filename = `${req.query.filename}.pdf`
    mfCommons.logReq(res, 'Document name: %s', filename)
    if (!res.locals.backendBaseUrl) {
        return res.status(400).json({ message: 'Backend API base URL is required' })
    }
    documentBuffer =  await agreement.retrieveDocument(res, req.params.agreementId)
    res.setHeader('Content-Disposition',`attachment; filename="${filename}"`)
    return res.send(documentBuffer)
}

