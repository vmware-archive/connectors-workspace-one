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
  it('it should generate cards for Assigned Deals With All Fields', async () => {
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
        mfJwt: { email: 'srini.gargeya@vmware.com' }},
      json: (jsonIn) => {
        respBody = jsonIn
      },
      status: (sc) => {
        statusCode = sc
        return mockResp
      }
    }
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.resolve(userInfoMockResp))
    const stagesMap = sinon.stub(service,'getDealStagesMap').returns(Promise.resolve(dealStagesOutput))
    const openDeals = sinon.stub(service, 'getAssignedDeals').returns(Promise.resolve(myOpenDeals))
    const companyInfo = sinon.stub(service,'getCompanyInfoFromId').returns(Promise.resolve(companyResp))
    const contactInfo = sinon.stub(service,'getContactNamesOfDeal').returns(Promise.resolve(contactResp))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    stagesMap.restore()
    openDeals.restore()
    companyInfo.restore()
    contactInfo.restore()
  })

  it('it should generate cards for Assigned Deals With Out Associations', async () => {
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
        mfJwt: { email: 'srini.gargeya@vmware.com' }},
      json: (jsonIn) => {
        respBody = jsonIn
      },
      status: (sc) => {
        statusCode = sc
        return mockResp
      }
    }
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.resolve(userInfoMockResp))
    const stagesMap = sinon.stub(service,'getDealStagesMap').returns(Promise.resolve(dealStagesOutput))
    const openDeals = sinon.stub(service, 'getAssignedDeals').returns(Promise.resolve(myOpenDealsWithOutAssociations))
    const companyInfo = sinon.stub(service,'getCompanyInfoFromId').returns(Promise.resolve(companyResp))
    const contactInfo = sinon.stub(service,'getContactNamesOfDeal').returns(Promise.resolve(contactResp))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    stagesMap.restore()
    openDeals.restore()
    companyInfo.restore()
    contactInfo.restore()
  })
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
        mfJwt: { email: 'srini.gargeya@vmware.com' }},
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

  it('it should return empty cards', async () => {
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
        mfJwt: { email: 'srini.gargeya@vmware.com' }},
      json: (jsonIn) => {
        respBody = jsonIn
      },
      status: (sc) => {
        statusCode = sc
        return mockResp
      }
    }
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.resolve(userInfoMockResp))
    const stagesMap = sinon.stub(service,'getDealStagesMap').returns(Promise.resolve(dealStagesOutput))
    const openDeals = sinon.stub(service, 'getAssignedDeals').returns(Promise.resolve([]))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    stagesMap.restore()
    openDeals.restore()
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
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 501}))))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(backendStatus).to.equal(501)
    expect(statusCode).to.equal(500)
    getUserInfoStub.restore()
  })
})

const userInfoMockResp = {
  "token": "CPP55ui_LhIDAQEBGOqz9wMg0OTEBSjS6Q0yGQCb8pDWHiWNQdQIidEHjkVrmPOMoOoIBE06GgAKAkEAAAyAwgcIAAAAAQAAAAAAAAAYwAAfQhkAm_KQ1u4skUNi1LnTg9HoQhBucVVmiEqs",
  "user": "kumar@backflipt.com",
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

const myOpenDeals = [
    {
      "portalId": 8247786,
      "dealId": 2789520462,
      "isDeleted": false,
      "associations": {
        "associatedVids": [],
        "associatedCompanyIds": [],
        "associatedDealIds": [],
        "associatedTicketIds": []
      },
      "properties": {
        "dealname": {
          "value": "7Deal-$0",
          "timestamp": 1597300905392,
          "source": "CRM_UI",
          "sourceId": "pavan@backflipt.com",
          "versions": [
            {
              "name": "dealname",
              "value": "7Deal-$0",
              "timestamp": 1597300905392,
              "sourceId": "pavan@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "amount": {
          "value": "0",
          "timestamp": 1597300905392,
          "source": "CRM_UI",
          "sourceId": "pavan@backflipt.com",
          "versions": [
            {
              "name": "amount",
              "value": "0",
              "timestamp": 1597300905392,
              "sourceId": "pavan@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "closedate": {
          "value": "1598856093402",
          "timestamp": 1597300905392,
          "source": "CRM_UI",
          "sourceId": "pavan@backflipt.com",
          "versions": [
            {
              "name": "closedate",
              "value": "1598856093402",
              "timestamp": 1597300905392,
              "sourceId": "pavan@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "hubspot_owner_id": {
          "value": "50103167",
          "timestamp": 1597300905392,
          "source": "CRM_UI",
          "sourceId": "pavan@backflipt.com",
          "versions": [
            {
              "name": "hubspot_owner_id",
              "value": "50103167",
              "timestamp": 1597300905392,
              "sourceId": "pavan@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "hs_lastmodifieddate": {
          "value": "1597300907726",
          "timestamp": 1597300907726,
          "source": "CALCULATED",
          "sourceId": null,
          "versions": [
            {
              "name": "hs_lastmodifieddate",
              "value": "1597300907726",
              "timestamp": 1597300907726,
              "source": "CALCULATED",
              "sourceVid": []
            }
          ]
        },
        "dealstage": {
          "value": "closedlost",
          "timestamp": 1597300905392,
          "source": "CRM_UI",
          "sourceId": "pavan@backflipt.com",
          "versions": [
            {
              "name": "dealstage",
              "value": "closedlost",
              "timestamp": 1597300905392,
              "sourceId": "pavan@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "hs_createdate": {
          "value": "1597300905392",
          "timestamp": 1597300905392,
          "source": "CONTACTS",
          "sourceId": "CRM_UI",
          "versions": [
            {
              "name": "hs_createdate",
              "value": "1597300905392",
              "timestamp": 1597300905392,
              "sourceId": "CRM_UI",
              "source": "CONTACTS",
              "sourceVid": []
            }
          ]
        },
        "days_to_close": {
          "value": "18",
          "timestamp": 1597300905392,
          "source": "CALCULATED",
          "sourceId": null,
          "versions": [
            {
              "name": "days_to_close",
              "value": "18",
              "timestamp": 1597300905392,
              "source": "CALCULATED",
              "sourceVid": []
            }
          ]
        },
        "dealtype": {
          "value": "existingbusiness",
          "timestamp": 1597300905392,
          "source": "CRM_UI",
          "sourceId": "pavan@backflipt.com",
          "versions": [
            {
              "name": "dealtype",
              "value": "existingbusiness",
              "timestamp": 1597300905392,
              "sourceId": "pavan@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        }
      },
      "imports": [],
      "stateChanges": []
    },  
    {
      "portalId": 8247786,
      "dealId": 2776312614,
      "isDeleted": false,
      "associations": {
        "associatedVids": [51,151],
        "associatedCompanyIds": [4294592507],
        "associatedDealIds": [],
        "associatedTicketIds": []
      },
      "properties": {
        "dealname": {
          "value": "Workspace one",
          "timestamp": 1597036631500,
          "source": "CRM_UI",
          "sourceId": "kumar@backflipt.com",
          "versions": [
            {
              "name": "dealname",
              "value": "Workspace one",
              "timestamp": 1597036631500,
              "sourceId": "kumar@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "amount": {
          "value": "100000",
          "timestamp": 1597036631500,
          "source": "CRM_UI",
          "sourceId": "kumar@backflipt.com",
          "versions": [
            {
              "name": "amount",
              "value": "100000",
              "timestamp": 1597036631500,
              "sourceId": "kumar@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "closedate": {
          "value": "1598850937213",
          "timestamp": 1597036631500,
          "source": "CRM_UI",
          "sourceId": "kumar@backflipt.com",
          "versions": [
            {
              "name": "closedate",
              "value": "1598850937213",
              "timestamp": 1597036631500,
              "sourceId": "kumar@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "hubspot_owner_id": {
          "value": "50035835",
          "timestamp": 1597036631500,
          "source": "CRM_UI",
          "sourceId": "kumar@backflipt.com",
          "versions": [
            {
              "name": "hubspot_owner_id",
              "value": "50035835",
              "timestamp": 1597036631500,
              "sourceId": "kumar@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "hs_lastmodifieddate": {
          "value": "1597070565671",
          "timestamp": 1597070565671,
          "source": "CALCULATED",
          "sourceId": null,
          "versions": [
            {
              "name": "hs_lastmodifieddate",
              "value": "1597070565671",
              "timestamp": 1597070565671,
              "source": "CALCULATED",
              "sourceVid": []
            }
          ]
        },
        "dealstage": {
          "value": "appointmentscheduled",
          "timestamp": 1597036631500,
          "source": "CRM_UI",
          "sourceId": "kumar@backflipt.com",
          "versions": [
            {
              "name": "dealstage",
              "value": "appointmentscheduled",
              "timestamp": 1597036631500,
              "sourceId": "kumar@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "hs_createdate": {
          "value": "1597036631500",
          "timestamp": 1597036631500,
          "source": "CONTACTS",
          "sourceId": "CRM_UI",
          "versions": [
            {
              "name": "hs_createdate",
              "value": "1597036631500",
              "timestamp": 1597036631500,
              "sourceId": "CRM_UI",
              "source": "CONTACTS",
              "sourceVid": []
            }
          ]
        },
        "days_to_close": {
          "value": "21",
          "timestamp": 1597036631500,
          "source": "CALCULATED",
          "sourceId": null,
          "versions": [
            {
              "name": "days_to_close",
              "value": "21",
              "timestamp": 1597036631500,
              "source": "CALCULATED",
              "sourceVid": []
            }
          ]
        },
        "dealtype": {
          "value": "newbusiness",
          "timestamp": 1597036631500,
          "source": "CRM_UI",
          "sourceId": "kumar@backflipt.com",
          "versions": [
            {
              "name": "dealtype",
              "value": "newbusiness",
              "timestamp": 1597036631500,
              "sourceId": "kumar@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        }
      },
      "imports": [],
      "stateChanges": []
    }
  ]

  const myOpenDealsWithOutAssociations = [ 
    {
      "portalId": 8247786,
      "dealId": 2776312614,
      "isDeleted": false,
      "associations": {
        "associatedVids": [],
        "associatedCompanyIds": [],
        "associatedDealIds": [],
        "associatedTicketIds": []
      },
      "properties": {
        "dealname": {
          "value": "Workspace one",
          "timestamp": 1597036631500,
          "source": "CRM_UI",
          "sourceId": "kumar@backflipt.com",
          "versions": [
            {
              "name": "dealname",
              "value": "Workspace one",
              "timestamp": 1597036631500,
              "sourceId": "kumar@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "hubspot_owner_id": {
          "value": "50035835",
          "timestamp": 1597036631500,
          "source": "CRM_UI",
          "sourceId": "kumar@backflipt.com",
          "versions": [
            {
              "name": "hubspot_owner_id",
              "value": "50035835",
              "timestamp": 1597036631500,
              "sourceId": "kumar@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        },
        "notes_last_contacted": {
          "value": "1597070565671",
          "timestamp": 1597070565671,
          "source": "CALCULATED",
          "sourceId": null,
          "versions": [
            {
              "name": "hs_lastmodifieddate",
              "value": "1597070565671",
              "timestamp": 1597070565671,
              "source": "CALCULATED",
              "sourceVid": []
            }
          ]
        },
        "dealstage": {
          "value": "appointmentscheduled",
          "timestamp": 1597036631500,
          "source": "CRM_UI",
          "sourceId": "kumar@backflipt.com",
          "versions": [
            {
              "name": "dealstage",
              "value": "appointmentscheduled",
              "timestamp": 1597036631500,
              "sourceId": "kumar@backflipt.com",
              "source": "CRM_UI",
              "sourceVid": []
            }
          ]
        }
      },
      "imports": [],
      "stateChanges": []
    }
  ]

const dealStagesOutput = {
  '4074848': { label: 'Pre Qualified To Buy', probability: '1.0' },
  appointmentscheduled: { label: 'Appointment Scheduled', probability: '0.2' },
  closedwon: { label: 'Closed Won', probability: '1.0' },
  presentationscheduled: { label: 'Presentation Scheduled', probability: '0.6' },
  closedlost: { label: 'Closed Lost', probability: '0.0' },
  qualifiedtobuy: { label: 'Qualified To Buy', probability: '0.4' },
  decisionmakerboughtin: { label: 'Decision Maker Bought-In', probability: '0.8' },
  contractsent: { label: 'Contract Sent', probability: '0.9' }
}

const companyResp = "HubSpot, Inc."

const contactResp = [
  {
    phone: '',
    email: '',
    time: '1597052179122',
    name: 'Brian Halligan (Sample Contact)',
    vid: 51
  },
  {
    phone: '555-444-1122',
    email: 'pete@cdk.com',
    time: '',
    name: ' ',
    vid: 151
  }
]
