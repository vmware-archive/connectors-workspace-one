/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const fetch = require('node-fetch')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const { expect } = require('chai')
const testUtils = require('./test-utils')
const mockAdobe = require('./mock-adobe')

const MF_SERVER_PORT = 5000
const CONNECTOR_PORT = 3000
const X_CONNECTOR_BASE_URL = 'http://localhost:4000'


describe('Adobe sign connector tests', () => {
    let server

    before(() => {
        if (!server) {
            mfCommons.mockMfServer.start(MF_SERVER_PORT)
            mockAdobe.start()
            process.env.MF_JWT_PUB_KEY_URI = `http://localhost:${MF_SERVER_PORT}/security/public-key`

            try {
                delete require.cache[require.resolve('../index')]
                server = require('../index')
            } catch (e) {
                console.log('Something went wrong!', e)
                expect.fail('Something went wrong!')
            }
        }
    })

    after(() => {
        if (server) {
            server.close()
            mfCommons.mockMfServer.stop()
            mockAdobe.stop()
        }
    })

    describe('Health', () => {
        it('should return status of up', async () => {
            const response = await fetch(`http://localhost:${CONNECTOR_PORT}/health`)
            const actualJson = await response.json()

            expect(response.status).to.eql(200)
            expect(actualJson.status).to.eql('UP')
        })
    })

    describe('Discovery', () => {
        it('should return correct metadata', async () => {
            const response = await callDiscovery()
            const actualJson = await response.json()
            expect(mfCommons.validateDiscovery(actualJson).valid).to.eql(true)
            const expDiscovery = require('./connector/response/discovery')
            expect(response.status).to.eql(200)
            expect(actualJson).to.eql(expDiscovery)

        })
    })

    describe('Adobe Auth token error', () => {
        it('when Auth is expired or invalid', async () => {
            const mfToken = mfCommons.mockMfServer.getMfToken({
                username: 'bgurrala',
                audience: 'https://my-host:3030/abc/api/cards/requests'
            })
            const authorization = 'bad-token'
            const response = await callCardApi(mfToken, authorization, X_CONNECTOR_BASE_URL)
            const actualJson = await response.json()
            expect(actualJson).to.eql({
                "error": "Unauthorized"
            })
        })
    })

    describe('X-Connector headers', () => {
        it('should reject if backend base URL is not present', async () => {
            const mfToken = mfCommons.mockMfServer.getMfToken({
                username: 'ansoni',
                audience: 'https://my-host:3030/abc/api/cards/requests'
            })
            const response = await callCardApi(mfToken, 'good-token', '')
            const actualJson = await response.json()
            expect(response.status).to.eql(400)
            expect(actualJson.message).to.eql('Backend API base URL is required')
        })
    })

    describe('card-with-multiple-objects', () => {
        it('when user has two or more active agreements', async () => {
            const mfToken = mfCommons.mockMfServer.getMfToken({
                username: 'bvandana',
                audience: 'https://my-host:3030/abc/api/cards/requests'
            })
            const authorization = 'good-token'
            const response = await callCardApi(mfToken, authorization, X_CONNECTOR_BASE_URL)
            const actualJson = await response.json()
            testUtils.replaceCardsUuid(actualJson)
            expect(mfCommons.validateCard(actualJson).valid).to.eql(true)
            const expCardsResponse = require('./connector/response/success-with-two-or-more-card-objects')
            expect(actualJson).to.eql(expCardsResponse)
        })
    })

    describe('card-with-no-object', () => {
        it('when user has no active agreement', async () => {
            const mfToken = mfCommons.mockMfServer.getMfToken({
                username: 'karnsa',
                audience: 'https://my-host:3030/abc/api/cards/requests'
            })
            const authorization = 'good-token'
            const response = await callCardApi(mfToken, authorization, X_CONNECTOR_BASE_URL)
            const actualJson = await response.json()
            testUtils.replaceCardsUuid(actualJson)
            expect(mfCommons.validateCard(actualJson).valid).to.eql(true)
            const expCardsResponse = require('./connector/response/success-with-empty-card-object')
            expect(actualJson).to.eql(expCardsResponse)
        })
    })

    describe('card-with-one-object', () => {
        it('when user has only one active agreement', async () => {
            const mfToken = mfCommons.mockMfServer.getMfToken({
                username: 'bgurrala',
                audience: 'https://my-host:3030/abc/api/cards/requests'
            })
            const authorization = 'good-token'
            const response = await callCardApi(mfToken, authorization, X_CONNECTOR_BASE_URL)
            const actualJson = await response.json()
            testUtils.replaceCardsUuid(actualJson)
            expect(mfCommons.validateCard(actualJson).valid).to.eql(true)
            const expCardsResponse = require('./connector/response/success-with-one-card-object')
            expect(actualJson).to.eql(expCardsResponse)
        })
    })

    describe('test-document-api', () => {
        it('compare buffer from the getDocument API', async () => {
            let fileBuffer
            const mfToken = mfCommons.mockMfServer.getMfToken({
                username: 'bvandana',
                audience: 'https://my-host:3030/abc/api/getDocument/123/?filename=dummyDocument'
            })
            const authorization = 'good-token'
            const response = await callDocumentApi(mfToken, authorization, X_CONNECTOR_BASE_URL, 'GET')
            fileBuffer = Buffer.from('attachment', 'utf-8')
            expect(response.body._readableState.buffer.head.data).to.eql(fileBuffer)
            expect(response.status).to.eql(200)
            expect(response.statusText).to.eql('OK')
        })
    })

    describe('test-document-api', () => {
        it('Check for 200 response of HEAD request to document API', async () => {
            const mfToken = mfCommons.mockMfServer.getMfToken({
                username: 'bvandana',
                audience: 'https://my-host:3030/abc/api/getDocument/123/?filename=dummyDocument'
            })
            const authorization = 'good-token'
            const response = await callDocumentApi(mfToken, authorization, X_CONNECTOR_BASE_URL, 'HEAD')
            expect(response.status).to.eql(200)
        })
    })


})

const callDiscovery = async () => {
    const options = {}
    testUtils.addXForwardedHeaders(options)
    return fetch(`http://localhost:${CONNECTOR_PORT}/`, options)
}

const callCardApi = async (mfToken, adobeToken, xConnectorBaseUrl) => {
    const options = {
        method: 'POST',
        headers: {
            Authorization: `Bearer ${mfToken}`,
            'X-Connector-Authorization':`${adobeToken}`,
            'X-Connector-Base-Url' :`${xConnectorBaseUrl}`,
            'X-Routing-Prefix': `http://localhost:${MF_SERVER_PORT}/connectors/123/card/`
        }
    }
    testUtils.addXForwardedHeaders(options)
    return fetch(`http://localhost:${CONNECTOR_PORT}/api/cards/requests`, options)
}

const callDocumentApi = async (mfToken, adobeToken, xConnectorBaseUrl, requestType) => {
    const options = {
        method: `${requestType}`,
        headers: {
            Authorization: `Bearer ${mfToken}`,
            'X-Connector-Authorization':`${adobeToken}`,
            'X-Connector-Base-Url' :`${xConnectorBaseUrl}`,
            'X-Routing-Prefix': `http://localhost:${MF_SERVER_PORT}/connectors/123/card/`
        }
    }
    testUtils.addXForwardedHeaders(options)
    return fetch(`http://localhost:${CONNECTOR_PORT}/api/getDocument/123/?filename=dummyDocument`, options)
}

