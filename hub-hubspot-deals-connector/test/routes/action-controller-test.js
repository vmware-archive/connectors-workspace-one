/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const {describe} = require('mocha')
const {expect} = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const {performAction} = require('../../routes/actions-controller')
const service = require('../../services/hubspot-services')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('AddingCommentToDealAndLoggingACall', () => {
    it('comment addition to deal should be successful', async () => {
        const mockReq = {
            body: {
                "actionType":"NOTE",
                "dealId":"2776312614",
                "ownerId":"50035835",
                "sourceId":"srini.gargeya@vmware.com"
            }
        }
        let respBody = ''
        let statusCode = ''
        const mockResp = {
            locals: {
                mfRoutingPrefix: 'https://mf-server/conn123/card/',
                mfJwt: { email: 'srini.gargeya@vmware.com' }
            },
            json: (jsonIn) => {
                respBody = jsonIn
            },
            status: (sc) => {
                statusCode = sc
                return mockResp
            }
        }
        const postCommentStub = sinon.stub(service, 'performActionOnDeal').returns(Promise.resolve(JSON.stringify({statusCode: 204})))
        await performAction(mockReq, mockResp)
        expect(statusCode).to.equal(200)
        postCommentStub.restore()
    })

    it('comment addition should throw 401 error', async () => {
        const mockReq = {
            body: {
                "actionType":"NOTE",
                "dealId":"2776312614",
                "ownerId":"50035835",
                "sourceId":"srini.gargeya@vmware.com"
            }
        }
        let respBody = ''
        let statusCode = ''
        let backendStatus = ''
        const mockResp = {
            locals: {
                mfRoutingPrefix: 'https://mf-server/conn123/card/',
                mfJwt: { email: 'srini.gargeya@vmware.com' }
              },
            json: (jsonIn) => {
                respBody = jsonIn
            },
            status: (sc) => {
                statusCode = sc
                return mockResp
            },
            header: (hdrKey, bckndSts) => {
                backendStatus = bckndSts
                return mockResp
            }
        }
        const postCommentFailStub = sinon.stub(service, 'performActionOnDeal').returns(Promise.reject(new Error(JSON.stringify({statusCode: 401}))))
        await performAction(mockReq, mockResp)
        expect(statusCode).to.equal(400)
        expect(backendStatus).to.equal(401)
        expect(respBody.method).to.equal('performAction')
        postCommentFailStub.restore()
    })

    it('comment addition should throw 500 error on non 401 error', async () => {
        const mockReq = {
            body: {
                "actionType":"NOTE",
                "dealId":"2776312614",
                "ownerId":"50035835",
                "sourceId":"srini.gargeya@vmware.com"
            }
        }
        let respBody = ''
        let statusCode = ''
        let backendStatus = ''
        const mockResp = {
            locals: {
                mfRoutingPrefix: 'https://mf-server/conn123/card/',
                mfJwt: { email: 'srini.gargeya@vmware.com' }
              },
            json: (jsonIn) => {
                respBody = jsonIn
            },
            status: (sc) => {
                statusCode = sc
                return mockResp
            },
            header: (hdrKey, bckndSts) => {
                backendStatus = bckndSts
                return mockResp
            }
        }
        const postCommentStub = sinon.stub(service, 'performActionOnDeal').returns(Promise.reject(new Error(JSON.stringify({statusCode: 422}))))
        await performAction(mockReq, mockResp)
        expect(statusCode).to.equal(500)
        expect(backendStatus).to.equal(422)
        expect(respBody.method).to.equal('performAction')
        postCommentStub.restore()
    })
})