/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { describe } = require('mocha')
const { expect } = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const service = require('../../services/dynamics-tasks-service')
const { cardsController, removeEmptyKeys } = require('../../routes/cards-request')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('cards-controller', () => {
  it('it should generate cards for empty userinfo', async () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
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
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.resolve(""))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
  })

  it('it should generate cards ', async () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
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
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.resolve(userInfo))
    const myTasksInfoStub = sinon.stub(service, 'getMyActiveTasks').returns(Promise.resolve(myTasksInfo))
    const getCurrentUserMailFromIdStub = sinon.stub(service, 'getCurrentUserMailFromId').returns(Promise.resolve(currentUserMailInfo)) 
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    myTasksInfoStub.restore()
    getCurrentUserMailFromIdStub.restore()
  })
  
  it('it should empty cards', async () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
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
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.resolve([]))
    const getMyActiveTasksStub = sinon.stub(service, 'getMyActiveTasks').returns(Promise.resolve([]))
    const getCurrentUserMailFromIdStub = sinon.stub(service, 'getCurrentUserMailFromId').returns(Promise.resolve([]))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    getMyActiveTasksStub.restore()
    getCurrentUserMailFromIdStub.restore()
  })

  it('cards should throw 401 if userInfo api throws 401 error', async () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
      }
    }
    let respBody = ''
    let statusCode = ''
    let backendStatus = ''
    const mockResp = {
      locals: {
        mfRoutingPrefix: 'https://mf-server/conn123/card/'
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
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 401 }))))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(backendStatus).to.equal(401)
    expect(statusCode).to.equal(400)
    getUserInfoStub.restore()
  })

  it('cards should throw 500 if userInfo api throws 5xx error', async () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
      }
    }
    let respBody = ''
    let statusCode = ''
    let backendStatus = ''
    const mockResp = {
      locals: {
        mfRoutingPrefix: 'https://mf-server/conn123/card/'
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
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 501 }))))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(backendStatus).to.equal(501)
    expect(statusCode).to.equal(500)
    getUserInfoStub.restore()
  })

  it('cards should throw 401 if getActiveTasks api throws 401 error', async () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
      }
    }
    let respBody = ''
    let statusCode = ''
    let backendStatus = ''
    const mockResp = {
      locals: {
        mfRoutingPrefix: 'https://mf-server/conn123/card/'
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
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.resolve(userInfo))
    const getMyActiveTasksStub = sinon.stub(service, 'getMyActiveTasks').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 401 }))))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(backendStatus).to.equal(401)
    expect(statusCode).to.equal(400)
    getUserInfoStub.restore()
    getMyActiveTasksStub.restore()
  })

})

const userInfo = {
  "@odata.context": "https://org.crm8.dynamics.com/api/data/v9.0/$metadata#Microsoft.Dynamics.CRM.WhoAmIResponse",
  "BusinessUnitId": "28f42def-e8b5-ea11-a812-000d3a3dfff4",
  "UserId": "0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a",
  "OrganizationId": "5a9af9ec-9024-4887-8068-a0053d12584d"
}
const myTasksInfo =  [
    {
      "@odata.etag": "W/\"4260710\"",
      "subject": "Provide product information",
      "description": "Product information is missing in the catalog",
      "activityid": "1c4095e5-60c6-ea11-a812-000d3a3e1af1",
      "prioritycode@odata.community.display.v1.formattedvalue": "Low",
      "prioritycode": 0,
      "createdon@odata.community.display.v1.formattedvalue": "15-07-2020 11:33",
      "createdon": "2020-07-15T06:03:26Z",
      "scheduledend@odata.community.display.v1.formattedvalue": "15-07-2020 08:00",
      "scheduledend": "2020-07-15T02:30:00Z",
      "statuscode@odata.community.display.v1.formattedvalue": "Not Started",
      "statuscode": 2,
      "scheduleddurationminutes@odata.community.display.v1.formattedvalue": "0",
      "scheduleddurationminutes": 0,
      "actualdurationminutes@odata.community.display.v1.formattedvalue": "30",
      "actualdurationminutes": 30,
      "_regardingobjectid_value@odata.community.display.v1.formattedvalue": "SU John",
      "_regardingobjectid_value@microsoft.dynamics.crm.associatednavigationproperty": "regardingobjectid_contact_task",
      "_regardingobjectid_value@microsoft.dynamics.crm.lookuplogicalname": "contact",
      "_regardingobjectid_value": "7017e397-5ec6-ea11-a812-000d3a3e1af1"
    },
    {
      "@odata.etag": "W/\"4260767\"",
      "subject": "Damaged Product",
      "description": null,
      "activityid": "78a0fa11-70c6-ea11-a812-000d3a3e1af1",
      "prioritycode@odata.community.display.v1.formattedvalue": "High",
      "prioritycode": 2,
      "createdon@odata.community.display.v1.formattedvalue": "15-07-2020 13:22",
      "createdon": "2020-07-15T07:52:04Z",
      "scheduledend@odata.community.display.v1.formattedvalue": "15-07-2020 08:00",
      "scheduledend": "2020-07-15T02:30:00Z",
      "statuscode@odata.community.display.v1.formattedvalue": "Not Started",
      "statuscode": 2,
      "scheduleddurationminutes@odata.community.display.v1.formattedvalue": "0",
      "scheduleddurationminutes": 0,
      "actualdurationminutes@odata.community.display.v1.formattedvalue": "30",
      "actualdurationminutes": 30,
      "_regardingobjectid_value@odata.community.display.v1.formattedvalue": "Stanford University",
      "_regardingobjectid_value@microsoft.dynamics.crm.associatednavigationproperty": "regardingobjectid_account_task",
      "_regardingobjectid_value@microsoft.dynamics.crm.lookuplogicalname": "account",
      "_regardingobjectid_value": "ae86a2b0-60c6-ea11-a812-000d3a3e1af1"
    }
  ]

  const currentUserMailInfo =  "pavan@xen22.onmicrosoft.com"
  




