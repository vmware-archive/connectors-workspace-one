/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const {describe} = require('mocha')
const {expect} = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const {completeTask,cancelTask} = require('../../routes/actions-controller')
const service = require('../../services/dynamics-tasks-service')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('completeTask', () => {
    it('completing task should be successful', async () => {
        const mockReq = {
            body: {
                taskId: '78a0fa11-70c6-ea11-a812-000d3a3e1af1'
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
        const completeTaskStub = sinon.stub(service, 'markTaskAsCompleted').returns(Promise.resolve(JSON.stringify({statusCode: 204})))
        await completeTask(mockReq, mockResp)
        expect(statusCode).to.equal(200)
        completeTaskStub.restore()
    })

    it('completing task should throw 401 error', async () => {
        const mockReq = {
            body: {
                taskId: '78a0fa11-70c6-ea11-a812-000d3a3e1af1'
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
        const completeTaskStub = sinon.stub(service, 'markTaskAsCompleted').returns(Promise.reject(new Error(JSON.stringify({statusCode: 401}))))
        await completeTask(mockReq, mockResp)
        expect(statusCode).to.equal(400)
        expect(backendStatus).to.equal(401)
        expect(respBody.method).to.equal('completeTask')
        completeTaskStub.restore()
    })

    it('completing task should throw 500 error on non 401 error', async () => {
        const mockReq = {
            body: {
                taskId: '78a0fa11-70c6-ea11-a812-000d3a3e1af1'
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
        const completeTaskStub = sinon.stub(service, 'markTaskAsCompleted').returns(Promise.reject(new Error(JSON.stringify({statusCode: 422}))))
        await completeTask(mockReq, mockResp)
        expect(statusCode).to.equal(500)
        expect(backendStatus).to.equal(422)
        expect(respBody.method).to.equal('completeTask')
        completeTaskStub.restore()
    })
})

describe('cancelTask', () => {
    it('closing task should be successful', async () => {
        const mockReq = {
            body: {
                taskId: '78a0fa11-70c6-ea11-a812-000d3a3e1af1'
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
        const cancelTaskStub = sinon.stub(service, 'closeTask').returns(Promise.resolve(JSON.stringify({statusCode: 204})))
        await cancelTask(mockReq, mockResp)
        expect(statusCode).to.equal(200)
        cancelTaskStub.restore()
    })

    it('closing task should throw 401 error', async () => {
        const mockReq = {
            body: {
                taskId: '78a0fa11-70c6-ea11-a812-000d3a3e1af1'
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
        const cancelTaskStub = sinon.stub(service, 'closeTask').returns(Promise.reject(new Error(JSON.stringify({statusCode: 401}))))
        await cancelTask(mockReq, mockResp)
        expect(statusCode).to.equal(400)
        expect(backendStatus).to.equal(401)
        expect(respBody.method).to.equal('cancelTask')
        cancelTaskStub.restore()
    })

    it('closing task should throw 500 error on non 401 error', async () => {
        const mockReq = {
            body: {
                taskId: '78a0fa11-70c6-ea11-a812-000d3a3e1af1'
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
        const cancelTaskStub = sinon.stub(service, 'closeTask').returns(Promise.reject(new Error(JSON.stringify({statusCode: 422}))))
        await cancelTask(mockReq, mockResp)
        expect(statusCode).to.equal(500)
        expect(backendStatus).to.equal(422)
        expect(respBody.method).to.equal('cancelTask')
        cancelTaskStub.restore()
    })
})

