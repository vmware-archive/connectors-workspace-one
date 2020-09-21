/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { describe } = require('mocha')
const { expect } = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const service = require('../../services/hubspot-services')
const { cardsController } = require('../../routes/cards-requests')
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
        authContext: { eml: 'srini.gargeya@vmware.com' }
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
    const getHubSpotIdFromMailStu = sinon.stub(service, 'getHubSpotIdFromMail').returns(Promise.resolve(hubMailInfo))
    const newTickets = sinon.stub(service, 'getAssignedTickets').returns(Promise.resolve(getMyNewTickets))
    const ticketStagesMapInfo = sinon.stub(service, 'getTicketStagesMap').returns(Promise.resolve(ticketStagesMap))
    const getCompanyInfoFromId = sinon.stub(service, 'getCompanyInfoFromId').returns(Promise.resolve(companyInfo))
    const getContactNamesOfTicket = sinon.stub(service, 'getContactNamesOfTicket').returns(Promise.resolve(contactInfo))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    getHubSpotIdFromMailStu.restore()
    newTickets.restore()
    ticketStagesMapInfo.restore()
    getCompanyInfoFromId.restore()
    getContactNamesOfTicket.restore()
  })

  it('it should generate cards with empty contacts info ', async () => {
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
    const getHubSpotIdFromMailStu = sinon.stub(service, 'getHubSpotIdFromMail').returns(Promise.resolve(hubMailInfo))
    const newTickets = sinon.stub(service, 'getAssignedTickets').returns(Promise.resolve(getMyNewTicketswithEmptyAssociations))
    const ticketStagesMapInfo = sinon.stub(service, 'getTicketStagesMap').returns(Promise.resolve(ticketStagesMap))
    const getCompanyInfoFromId = sinon.stub(service, 'getCompanyInfoFromId').returns(Promise.resolve(companyInfo))
    const getContactNamesOfTicket = sinon.stub(service, 'getContactNamesOfTicket').returns(Promise.resolve(contactInfo))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    getHubSpotIdFromMailStu.restore()
    newTickets.restore()
    ticketStagesMapInfo.restore()
    getCompanyInfoFromId.restore()
    getContactNamesOfTicket.restore()
  })

  it('it should generate cards based on time', async () => {
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
    const getHubSpotIdFromMailStu = sinon.stub(service, 'getHubSpotIdFromMail').returns(Promise.resolve(hubMailInfo))
    const newTickets = sinon.stub(service, 'getAssignedTickets').returns(Promise.resolve(getMyNewTickets))
    const ticketStagesMapInfo = sinon.stub(service, 'getTicketStagesMap').returns(Promise.resolve(ticketStagesMap))
    const getCompanyInfoFromId = sinon.stub(service, 'getCompanyInfoFromId').returns(Promise.resolve(companyInfo))
    const getContactNamesOfTicket = sinon.stub(service, 'getContactNamesOfTicket').returns(Promise.resolve(contactInfobasedonTime))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    getHubSpotIdFromMailStu.restore()
    newTickets.restore()
    ticketStagesMapInfo.restore()
    getCompanyInfoFromId.restore()
    getContactNamesOfTicket.restore()
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
        authContext: { eml: 'srini.gargeya@vmware.com' }
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
    const getHubSpotIdFromMailStu = sinon.stub(service, 'getHubSpotIdFromMail').returns(Promise.resolve(hubMailInfo))
    const newTickets = sinon.stub(service, 'getAssignedTickets').returns(Promise.resolve([]))
    const ticketStagesMap = sinon.stub(service, 'getTicketStagesMap').returns(Promise.resolve([]))
    const getCompanyInfoFromId = sinon.stub(service, 'getCompanyInfoFromId').returns(Promise.resolve([]))
    const getContactNamesOfTicket = sinon.stub(service, 'getContactNamesOfTicket').returns(Promise.resolve([]))
    
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    getHubSpotIdFromMailStu.restore()
    newTickets.restore()
    ticketStagesMap.restore()
    getCompanyInfoFromId.restore()
    getContactNamesOfTicket.restore()
   
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

  it('cards should throw 401 if getNewTickets api throws 401 error', async () => {
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
    const getHubSpotIdFromMailStu = sinon.stub(service, 'getHubSpotIdFromMail').returns(Promise.resolve({"Id":"1234"}))
    const newTickets = sinon.stub(service, 'getAssignedTickets').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 401 }))))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(backendStatus).to.equal(401)
    expect(statusCode).to.equal(400)
    getUserInfoStub.restore()
    getHubSpotIdFromMailStu.restore()
    newTickets.restore()
    
  })
})

const userInfo = {
  "token": "CPP55ui_LhIDAQEBGOqz9wMg0OTEBSjS6Q0yGQCb8pDWHiWNQdQIidEHjkVrmPOMoOoIBE06GgAKAkEAAAyAwgcIAAAAAQAAAAAAAAAYwAAfQhkAm_KQ1u4skUNi1LnTg9HoQhBucVVmiEqs",
  "user": "srini@backflipt.com",
  "hub_domain": "backflipt.com",
  "scopes": [
    "contacts",
    "oauth",
    "tickets"
  ],
  "scope_to_scope_group_pks": [
    10,
    12,
    18,
    25,
    31,
    51,
    52,
    64,
    66,
    71,
    72,
    73,
    74,
    75,
    84,
    113,
    180,
    181,
    191,
    192,
    201,
    202,
    203,
    204,
    205
  ],
  "hub_id": 8247786,
  "app_id": 226514,
  "expires_in": 19565,
  "user_id": 11612752,
  "token_type": "access"
}


const getMyNewTickets = [
{
"id": "183534428",
"properties": {
"content": "Installation steps are not clear!",
"createdate": "2020-08-20T08:10:32.669Z",
"hs_lastmodifieddate": "2020-08-21T08:51:32.426Z",
"hs_object_id": "183534428",
"hs_pipeline": "0",
"hs_pipeline_stage": "1",
"hs_ticket_priority": "MEDIUM",
"hubspot_owner_id": "50103167",
"last_reply_date": null,
"source_type": "EMAIL",
"subject": "Product Installation Issue"
},
"createdAt": "2020-08-20T08:10:32.669Z",
"updatedAt": "2020-08-21T08:51:32.426Z",
"associations": {
"companies": {
"results": [
{
"id": "4320142817",
"type": "ticket_to_company"
}
]
},
"contacts": {
"results": [
{
"id": "201",
"type": "ticket_to_contact"
}
]
}
},
"archived": false
},
{
  "id": "183534428",
  "properties": {
  "content": "Installation steps are not clear!",
  "createdate": "2020-08-20T08:10:32.669Z",
  "hs_lastmodifieddate": "2020-08-21T08:51:32.426Z",
  "hs_object_id": "183534428",
  "hs_pipeline": "0",
  "hs_pipeline_stage": "1",

  "hubspot_owner_id": "50103167",
  "last_reply_date": null,
 
  "subject": "Product Installation Issue"
  },
  "createdAt": "2020-08-20T08:10:32.669Z",
  "updatedAt": "2020-08-21T08:51:32.426Z",
  "associations": {
  "companies": {
  "results": [
  {
  "id": "4320142817",
  "type": "ticket_to_company"
  }
  ]
  },
  "contacts": {
  "results": [
  {
  "id": "201",
  "type": "ticket_to_contact"
  }
  ]
  }
  },
  "archived": false
  }]

const getMyNewTicketswithEmptyAssociations = [
  {
  "id": "183534428",
  "properties": {
  "content": "Installation steps are not clear!",
  "createdate": "2020-08-20T08:10:32.669Z",
  "hs_lastmodifieddate": "2020-08-21T08:51:32.426Z",
  "hs_object_id": "183534428",
  "hs_pipeline": "0",
  "hs_pipeline_stage": "1",
  "hs_ticket_priority": "MEDIUM",
  "hubspot_owner_id": "50103167",
  "last_reply_date": null,
  "source_type": "EMAIL",
  "subject": "Product Installation Issue"
  },
  "createdAt": "2020-08-20T08:10:32.669Z",
  "updatedAt": "2020-08-21T08:51:32.426Z",
  "associations": {
  "companies": {
  "results": []
  },
  "contacts": {
  "results": []
  }
  },
  "archived": false
  }
  ]




const contactInfo = [{'phone':"",'email':"srini@backflipt.com", 'time':"2020-08-28T16:17:01.353Z", 'name':"Srini Gargeya", 'vid':"50137194"}, {'phone':"",'email':"kumar@backflipt.com", 'time':"2020-08-24T16:17:01.353Z", 'name':"Kumar Gargeya", 'vid':"50137195"}]


const contactInfobasedonTime = [{'phone':"",'email':"srini@backflipt.com", 'time':"2020-08-10T16:17:01.353Z", 'name':"Srini Gargeya", 'vid':"50137194"}, {'phone':"",'email':"kumar@backflipt.com", 'time':"2020-08-24T16:17:01.353Z", 'name':"Kumar Gargeya", 'vid':"50137195"}]



const ticketStagesMap = {"1":{"label":"New", "isClosed":"false"}, "4": {"label":"Closed", "isClosed":"true"}}


  const companyInfo = "HubSpot, Inc."


  const hubMailInfo ={
  "id": " ",
  "email": "srini@backflipt.com",
  "firstName": "Srini",
  "lastName": "Gargeya",
  "userId": 4490908,
  "createdAt": "2020-08-13T16:17:01.353Z",
  "updatedAt": "2020-08-13T16:27:01.595Z",
  "archived": false
  }
