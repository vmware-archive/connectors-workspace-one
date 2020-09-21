/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const {describe} = require('mocha')
const {expect} = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const {performAction, updateStatus} = require('../../routes/actions-controller')
const service = require('../../services/hubspot-services')
const sinonChai = require('sinon-chai')
const { updateTicketStatus } = require('../../services/hubspot-services')
chai.use(sinonChai)

describe('AddingCommentToDealAndLoggingACall', () => {
    it('comment addition to deal should be successful', async () => {
        const mockReq = {
            body: {
                "actionType":"NOTE",
                "ticketId":"2776312614",
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
        const postCommentStub = sinon.stub(service, 'performActionOnTicket').returns(Promise.resolve(JSON.stringify({statusCode: 204})))
        await performAction(mockReq, mockResp)
        expect(statusCode).to.equal(200)
        postCommentStub.restore()
    })

    it('comment addition should throw 401 error', async () => {
        const mockReq = {
            body: {
                "actionType":"NOTE",
                "ticketId":"2776312614",
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
        const postCommentFailStub = sinon.stub(service, 'performActionOnTicket').returns(Promise.reject(new Error(JSON.stringify({statusCode: 401}))))
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
                "ticketId":"2776312614",
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
        const postCommentStub = sinon.stub(service, 'performActionOnTicket').returns(Promise.reject(new Error(JSON.stringify({statusCode: 422}))))
        await performAction(mockReq, mockResp)
        expect(statusCode).to.equal(500)
        expect(backendStatus).to.equal(422)
        expect(respBody.method).to.equal('performAction')
        postCommentStub.restore()
    })
})



describe('update ticket status', () => {
    it('update status of ticket should be successful', async () => {
        const mockReq = {
            body: {
                "actionType":"close",
                "ticketId":"2776312614"
               
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
        const ticketStausStub = sinon.stub(service, 'updateTicketStatus').returns(Promise.resolve(JSON.stringify({statusCode: 204})))
        await updateStatus(mockReq, mockResp)
        expect(statusCode).to.equal(200)
        ticketStausStub.restore()
    })

    it('update status should throw 401 error', async () => {
        const mockReq = {
            body: {
                "actionType":"close",
                "ticketId":"2776312614"
               
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
        const ticketStausFailStub = sinon.stub(service, 'updateTicketStatus').returns(Promise.reject(new Error(JSON.stringify({statusCode: 401}))))
        await updateStatus(mockReq, mockResp)
        expect(statusCode).to.equal(400)
        expect(backendStatus).to.equal(401)
        expect(respBody.method).to.equal('updateStatus')
        ticketStausFailStub.restore()
    })

    it('status update should throw 500 error on non 401 error', async () => {
        const mockReq = {
            body: {
                "actionType":"close",
                "ticketId":"2776312614"
            
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
        const ticketStausStub = sinon.stub(service, 'updateTicketStatus').returns(Promise.reject(new Error(JSON.stringify({statusCode: 422}))))
        await updateStatus(mockReq, mockResp)
        expect(statusCode).to.equal(500)
        expect(backendStatus).to.equal(422)
        expect(respBody.method).to.equal('updateStatus')
        ticketStausStub.restore()
    })
})