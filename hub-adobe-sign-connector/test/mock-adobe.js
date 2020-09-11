'use strict'

const mfCommons = require('@vmw/mobile-flows-connector-commons')

let fakeBackend

const ADOBE_BAD_TOKEN = 'bad-token'
const ADOBE_GOOD_TOKEN = 'good-token'
const startFakeBackend = () => {
    const express = require('express')
    const app = express()
    app.use(express.json())
    fakeBackend = app.listen(4000, () => console.log('Fake backend listening on 4000'))


    app.use('/api/rest/v6/*', (req, res, next) => {
        const adobeAuthToken = req.header('authorization')
        if (adobeAuthToken === ADOBE_BAD_TOKEN) {
            mfCommons.log('Invalid or expired adobe auth token.')
            return res.status(401).json({
                errorCode: 'INVALID_ACCESS_TOKEN',
                errorMessage: 'Access token provided is invalid or has expired'
            })
        }
        else if (adobeAuthToken === ADOBE_GOOD_TOKEN){
            next()
        }
        else{
            res.status(400).json({errorMessage: 'unit test failure: unexpected token'})
        }
    })

    app.get('/api/rest/v6/agreements', (req, res, next) => {
        let responseJson
        let userEmail
        let responseStatus
        const adobeAuthToken = req.header('authorization')
        userEmail = req.header('x-api-user')
        if (adobeAuthToken === ADOBE_GOOD_TOKEN) {
            switch (userEmail) {
                case 'email:bgurrala@vmware.com':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/user-all-agreement')
                    break
                case 'email:karnsa@vmware.com':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/user-with-no-active-agreements')
                    break
                case 'email:bvandana@vmware.com':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/user-with-two-or-more-active-agreement')
                    break
                default:
                    throw new Error('Unexpected user email.')
            }
            return res.status(responseStatus).json(responseJson)
        }
        next()
    })

    app.get('/api/rest/v6/agreements/:agreementId', (req, res, next) => {
        let responseJson
        let responseStatus
        let agreementId
        agreementId = req.params.agreementId
        const adobeAuthToken = req.header('authorization')
        if (adobeAuthToken === ADOBE_GOOD_TOKEN) {
            switch (agreementId) {
                case 'CBJCHBCAABAAtikxNLycMSwFYZUQbINIRqfMvOR_IMCV':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/user-agreement-1')
                    break
                case 'CBJCHBCAABAAzKSgJ3YR5h1LNM_nhHV69aJLcZ089lYU':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/user-agreement-2')
                    break
                case 'CBJCHBCAABAAR3IGlblssyTZtKqMS1o3cCnCzFJQY6XD':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/user-agreement-3')
                    break
                default:
                    throw new Error('Unexpected agreement id.')
            }
            return res.status(responseStatus).json(responseJson)
        }
        next()
    })

    app.get('/api/rest/v6/agreements/:agreementId/combinedDocument/url', (req, res, next) => {
        let responseJson
        let responseStatus
        let agreementId
        agreementId = req.params.agreementId
        const adobeAuthToken = req.header('authorization')
        if (adobeAuthToken === ADOBE_GOOD_TOKEN) {
            switch (agreementId) {
                case 'CBJCHBCAABAAtikxNLycMSwFYZUQbINIRqfMvOR_IMCV':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/user-document-url-1')
                    break
                case 'CBJCHBCAABAAzKSgJ3YR5h1LNM_nhHV69aJLcZ089lYU':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/user-document-url-2')
                    break
                case 'CBJCHBCAABAAR3IGlblssyTZtKqMS1o3cCnCzFJQY6XD':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/user-document-url-3')
                    break
                default:
                    throw new Error('Unexpected agreement id.')
            }
            return res.status(responseStatus).json(responseJson)
        }
        next()
    })

    app.get('/api/rest/v6/agreements/:agreementId/signingUrls', (req, res, next) => {
        let resposeBody
        let responseJson
        let responseStatus
        let agreementId
        agreementId = req.params.agreementId
        const adobeAuthToken = req.header('authorization')
        if (adobeAuthToken === ADOBE_GOOD_TOKEN) {
            switch (agreementId) {
                case 'CBJCHBCAABAAtikxNLycMSwFYZUQbINIRqfMvOR_IMCV':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/document-sign-url-1')
                    break
                case 'CBJCHBCAABAAzKSgJ3YR5h1LNM_nhHV69aJLcZ089lYU':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/document-sign-url-2')
                    break
                case 'CBJCHBCAABAAR3IGlblssyTZtKqMS1o3cCnCzFJQY6XD':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/document-sign-url-3')
                    break
                default:
                    throw new Error('Unexpected agreement id.')
            }
            return res.status(responseStatus).json(responseJson)
        }
        next()
    })
    app.get('/api/rest/v6/agreements/:agreementId/members', (req, res, next) => {
        let resposeBody
        let responseJson
        let responseStatus
        let agreementId
        agreementId = req.params.agreementId
        const adobeAuthToken = req.header('X-Connector-Authorization')
        if (adobeAuthToken !== ADOBE_GOOD_TOKEN) {
            switch (agreementId) {
                case 'CBJCHBCAABAAtikxNLycMSwFYZUQbINIRqfMvOR_IMCV':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/members-in-agreement1')
                    break
                case 'CBJCHBCAABAAzKSgJ3YR5h1LNM_nhHV69aJLcZ089lYU':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/members-in-agreement2')
                    break
                case 'CBJCHBCAABAAR3IGlblssyTZtKqMS1o3cCnCzFJQY6XD':
                    responseStatus = 200
                    responseJson = require('./adobeApi/response/members-in-agreement3')
                    break
                default:
                    throw new Error('Unexpected agreement id.')
            }
            return res.status(responseStatus).json(responseJson)
        }
        next()
    })

    app.get('/api/rest/v6/agreements/:agreementId/combinedDocument', (req, res, next) => {
        const adobeAuthToken = req.header('authorization')
        if (adobeAuthToken === ADOBE_GOOD_TOKEN) {
            return res.send(Buffer.from('attachment', 'utf-8'))
        }
        next()
    })

}

const start = () => {
    startFakeBackend()
}

const stop = () => {
    fakeBackend.close()
}

module.exports = {
    start,
    stop
}
