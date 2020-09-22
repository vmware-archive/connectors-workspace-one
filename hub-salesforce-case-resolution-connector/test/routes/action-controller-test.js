/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const {describe} = require('mocha')
const {expect} = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const {postComment,pendingAction,closeAction} = require('../../routes/actions-controller')
const service = require('../../services/sfdc-case-service')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('postComment', () => {
    it('comment addition should be successful', async () => {
        const mockReq = {
            body: {
                comments: 'Adding Test comment',
                caseId: '5006g00000AboHzAAJ'
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
        const postCommentStub = sinon.stub(service, 'postFeedItemToCase').returns(Promise.resolve(JSON.stringify({statusCode: 204})))
        await postComment(mockReq, mockResp)
        expect(statusCode).to.equal(200)
        postCommentStub.restore()
    })

    it('comment addition should throw 401 error', async () => {
        const mockReq = {
            body: {
                comments: 'Adding new comment',
                caseId: '5006g00000AboHzAAJ'
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
        const postCommentFailStub = sinon.stub(service, 'postFeedItemToCase').returns(Promise.reject(new Error(JSON.stringify({statusCode: 401}))))
        await postComment(mockReq, mockResp)
        expect(statusCode).to.equal(400)
        expect(backendStatus).to.equal(401)
        expect(respBody.method).to.equal('postComment')
        postCommentFailStub.restore()
    })

    it('comment addition should throw 500 error on non 401 error', async () => {
        const mockReq = {
            body: {
                comments: 'Adding new comment',
                caseId: '5006g00000AboHzAAJ'
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
        const postCommentStub = sinon.stub(service, 'postFeedItemToCase').returns(Promise.reject(new Error(JSON.stringify({statusCode: 422}))))
        await postComment(mockReq, mockResp)
        expect(statusCode).to.equal(500)
        expect(backendStatus).to.equal(422)
        expect(respBody.method).to.equal('postComment')
        postCommentStub.restore()
    })
})

describe('pendingAction', () => {
    it('changing status to OnHold Action SuccessFull', async () => {
        const mockReq = {
            body: {
                caseId: '5006g00000AboHzAAJ',
                actionType: 'On Hold'
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
        const pendingActionStub = sinon.stub(service, 'actionsOnCase').returns(Promise.resolve(JSON.stringify({statusCode: 204})))
        await pendingAction(mockReq, mockResp)
        expect(statusCode).to.equal(200)
        pendingActionStub.restore()
    })

    it('changing status to OnHold should throw 401 error', async () => {
        const mockReq = {
            body: {
                caseId: '5006g00000AboHzAAJ',
                actionType: 'On Hold'
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
        const pendingActionStub = sinon.stub(service, 'actionsOnCase').returns(Promise.reject(new Error(JSON.stringify({statusCode: 401}))))
        await pendingAction(mockReq, mockResp)
        expect(statusCode).to.equal(400)
        expect(backendStatus).to.equal(401)
        expect(respBody.method).to.equal('pendingAction')
        pendingActionStub.restore()
    })

    it('changing status to OnHold should throw 500 error on non 401 error', async () => {
        const mockReq = {
            body: {
                caseId: '5006g00000AboHzAAJ',
                actionType: 'On Hold'
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
        const pendingActionStub = sinon.stub(service, 'actionsOnCase').returns(Promise.reject(new Error(JSON.stringify({statusCode: 422}))))
        await pendingAction(mockReq, mockResp)
        expect(statusCode).to.equal(500)
        expect(backendStatus).to.equal(422)
        expect(respBody.method).to.equal('pendingAction')
        pendingActionStub.restore()
    })
})



