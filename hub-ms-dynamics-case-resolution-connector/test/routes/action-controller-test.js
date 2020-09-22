/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const {describe} = require('mocha')
const {expect} = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const {addNoteAboutCase,MarkCaseAsResolved,MarkCaseAsCancelled} = require('../../routes/actions-controller')
const service = require('../../services/dynamics-case-service')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('addNoteAboutCase', () => {
    it('comment addition should be successful', async () => {
        const mockReq = {
            body: {
                comment: 'Adding new comment',
                caseId: 'a72155ab-ae68-47a9-affa-d1132bd3081a'
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
        const addNoteAboutCaseStub = sinon.stub(service, 'addNotesToCase').returns(Promise.resolve(JSON.stringify({statusCode: 204})))
        await addNoteAboutCase(mockReq, mockResp)
        expect(statusCode).to.equal(200)
        addNoteAboutCaseStub.restore()
    })
    it('comment addition should throw 401 error', async () => {
        const mockReq = {
            body: {
                comment: 'Adding new comment',
                caseId: 'a72155ab-ae68-47a9-affa-d1132bd3081a'
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
        const addNoteAboutCaseStub = sinon.stub(service, 'addNotesToCase').returns(Promise.reject(new Error(JSON.stringify({statusCode: 401}))))
        await addNoteAboutCase(mockReq, mockResp)
        expect(statusCode).to.equal(400)
        expect(backendStatus).to.equal(401)
        expect(respBody.method).to.equal('addNoteAboutCase')
        addNoteAboutCaseStub.restore()
    })

    it('comment addition should throw 500 error on non 401 error', async () => {
        const mockReq = {
            body: {
                comment: 'Adding new comment',
                caseId: 'a72155ab-ae68-47a9-affa-d1132bd3081a'
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
        const addNoteAboutCaseStub = sinon.stub(service, 'addNotesToCase').returns(Promise.reject(new Error(JSON.stringify({statusCode: 422}))))
        await addNoteAboutCase(mockReq, mockResp)
        expect(statusCode).to.equal(500)
        expect(backendStatus).to.equal(422)
        expect(respBody.method).to.equal('addNoteAboutCase')
        addNoteAboutCaseStub.restore()
    })
})

describe('MarkCaseAsResolved', () => {
    it('comment addition should be successful', async () => {
        const mockReq = {
            body: {
                comment: 'Adding new comment',
                caseId: 'a72155ab-ae68-47a9-affa-d1132bd3081a'
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
        const addNoteAboutCaseStub = sinon.stub(service, 'resolveCase').returns(Promise.resolve(JSON.stringify({statusCode: 204})))
        await MarkCaseAsResolved(mockReq, mockResp)
        expect(statusCode).to.equal(200)
        addNoteAboutCaseStub.restore()
    })

    it('comment addition should throw 401 error', async () => {
        const mockReq = {
            body: {
                comment: 'Adding new comment',
                caseId: 'a72155ab-ae68-47a9-affa-d1132bd3081a'
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
        const addNoteAboutCaseStub = sinon.stub(service, 'resolveCase').returns(Promise.reject(new Error(JSON.stringify({statusCode: 401}))))
        await MarkCaseAsResolved(mockReq, mockResp)
        expect(statusCode).to.equal(400)
        expect(backendStatus).to.equal(401)
        expect(respBody.method).to.equal('MarkCaseAsResolved')
        addNoteAboutCaseStub.restore()
    })

    it('comment addition should throw 500 error on non 401 error', async () => {
        const mockReq = {
            body: {
                comment: 'Adding new comment',
                caseId: 'a72155ab-ae68-47a9-affa-d1132bd3081a'
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
        const addNoteAboutCaseStub = sinon.stub(service, 'resolveCase').returns(Promise.reject(new Error(JSON.stringify({statusCode: 422}))))
        await MarkCaseAsResolved(mockReq, mockResp)
        expect(statusCode).to.equal(500)
        expect(backendStatus).to.equal(422)
        expect(respBody.method).to.equal('MarkCaseAsResolved')
        addNoteAboutCaseStub.restore()
    })
})

describe('MarkCaseAsCancelled', () => {
    it('comment addition should be successful', async () => {
        const mockReq = {
            body: {
                caseId: 'a72155ab-ae68-47a9-affa-d1132bd3081a'
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
        const addNoteAboutCaseStub = sinon.stub(service, 'cancelCase').returns(Promise.resolve(JSON.stringify({statusCode: 204})))
        await MarkCaseAsCancelled(mockReq, mockResp)
        expect(statusCode).to.equal(200)
        addNoteAboutCaseStub.restore()
    })

    it('comment addition should throw 401 error', async () => {
        const mockReq = {
            body: {
                comment: 'Adding new comment',
                caseId: 'a72155ab-ae68-47a9-affa-d1132bd3081a'
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
        const addNoteAboutCaseStub = sinon.stub(service, 'cancelCase').returns(Promise.reject(new Error(JSON.stringify({statusCode: 401}))))
        await MarkCaseAsCancelled(mockReq, mockResp)
        expect(statusCode).to.equal(400)
        expect(backendStatus).to.equal(401)
        expect(respBody.method).to.equal('MarkCaseAsCancelled')
        addNoteAboutCaseStub.restore()
    })

    it('comment addition should throw 500 error on non 401 error', async () => {
        const mockReq = {
            body: {
                comment: 'Adding new comment',
                caseId: 'a72155ab-ae68-47a9-affa-d1132bd3081a'
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
        const addNoteAboutCaseStub = sinon.stub(service, 'cancelCase').returns(Promise.reject(new Error(JSON.stringify({statusCode: 422}))))
        await MarkCaseAsCancelled(mockReq, mockResp)
        expect(statusCode).to.equal(500)
        expect(backendStatus).to.equal(422)
        expect(respBody.method).to.equal('MarkCaseAsCancelled')
        addNoteAboutCaseStub.restore()
    })
})



