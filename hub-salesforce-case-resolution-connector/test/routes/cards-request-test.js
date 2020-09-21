/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { describe } = require('mocha')
const { expect } = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const service = require('../../services/sfdc-case-service')
const { cardsController } = require('../../routes/cards-request')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('cards-controller', () => {
  it('it should generate cards for suject and contact info empty', async () => {
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
    const openCases = sinon.stub(service, 'getMyOpenCases').returns(Promise.resolve(emptyContactCasesInfo))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    openCases.restore()
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
    const openCases = sinon.stub(service, 'getMyOpenCases').returns(Promise.resolve(myOpenCases))
    const getContactInfoStu = sinon.stub(service, 'getContactInfo').returns(Promise.resolve(contactInfo))
    const getAccountInfoStu = sinon.stub(service, 'getAccountInfo').returns(Promise.resolve(accountInfo))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    openCases.restore()
    getContactInfoStu.restore()
    getAccountInfoStu.restore()
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
    const openCases = sinon.stub(service, 'getMyOpenCases').returns(Promise.resolve([]))
    const contactInfoStu = sinon.stub(service, 'getContactInfo').returns(Promise.resolve([]))
    const acocountInfostu = sinon.stub(service, 'getAccountInfo').returns(Promise.resolve([]))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    openCases.restore()
    contactInfoStu.restore()
    acocountInfostu.restore()
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

  it('cards should throw 401 if getActiveCases api throws 401 error', async () => {
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
    const openCases = sinon.stub(service, 'getMyOpenCases').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 401 }))))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(backendStatus).to.equal(401)
    expect(statusCode).to.equal(400)
    getUserInfoStub.restore()
    openCases.restore()
  })
})

const userInfo = {
  "sub": "https://login.salesforce.com/id/00D6g000006wre4EAA/0056g0000049jc6AAA",
  "user_id": "0056g0000049jc6AAA",
  "organization_id": "00D6g000006wre4EAA",
  "preferred_username": "bfvmw@backflipt.com",
  "nickname": "User15814947397442788858",
  "name": "Backflipt VMware",
  "email": "support@backflipt.com",
  "email_verified": true,
  "given_name": "Backflipt",
  "family_name": "VMware",
  "zoneinfo": "America/Los_Angeles",
  "photos": {
    "picture": "https://bfvmw--c.documentforce.com/profilephoto/005/F",
    "thumbnail": "https://bfvmw--c.documentforce.com/profilephoto/005/T"
  },
  "profile": "https://bfvmw.my.salesforce.com/0056g0000049jc6AAA",
  "picture": "https://bfvmw--c.documentforce.com/profilephoto/005/F",
  "address": {
    "country": "US"
  },
  "urls": {
    "enterprise": "https://bfvmw.my.salesforce.com/services/Soap/c/{version}/00D6g000006wre4",
    "metadata": "https://bfvmw.my.salesforce.com/services/Soap/m/{version}/00D6g000006wre4",
    "partner": "https://bfvmw.my.salesforce.com/services/Soap/u/{version}/00D6g000006wre4",
    "rest": "https://bfvmw.my.salesforce.com/services/data/v{version}/",
    "sobjects": "https://bfvmw.my.salesforce.com/services/data/v{version}/sobjects/",
    "search": "https://bfvmw.my.salesforce.com/services/data/v{version}/search/",
    "query": "https://bfvmw.my.salesforce.com/services/data/v{version}/query/",
    "recent": "https://bfvmw.my.salesforce.com/services/data/v{version}/recent/",
    "tooling_soap": "https://bfvmw.my.salesforce.com/services/Soap/T/{version}/00D6g000006wre4",
    "tooling_rest": "https://bfvmw.my.salesforce.com/services/data/v{version}/tooling/",
    "profile": "https://bfvmw.my.salesforce.com/0056g0000049jc6AAA",
    "feeds": "https://bfvmw.my.salesforce.com/services/data/v{version}/chatter/feeds",
    "groups": "https://bfvmw.my.salesforce.com/services/data/v{version}/chatter/groups",
    "users": "https://bfvmw.my.salesforce.com/services/data/v{version}/chatter/users",
    "feed_items": "https://bfvmw.my.salesforce.com/services/data/v{version}/chatter/feed-items",
    "feed_elements": "https://bfvmw.my.salesforce.com/services/data/v{version}/chatter/feed-elements",
    "custom_domain": "https://bfvmw.my.salesforce.com"
  },
  "active": true,
  "user_type": "STANDARD",
  "language": "en_US",
  "locale": "en_US",
  "utcOffset": -28800000,
  "updated_at": "2020-04-20T07:44:12Z"
}

const myOpenCases =  [
    {
      "attributes": {
        "type": "Case",
        "url": "/services/data/v47.0/sobjects/Case/5006g00000ENTNcAAP"
      },
      "Id": "5006g00000ENTNcAAP",
      "OwnerId": "0056g000004BDUuAAO",
      "ContactId": "0036g00000Dwqd8AAB",
      "AccountId": "0016g00000CaNUOAA3",
      "Priority": "High",
      "Reason": "New problem",
      "Origin": "Email",
      "Subject": "Test Subject Added",
      "Status": "Closed",
      "Type": "Problem",
      "CaseNumber": "00001003",
      "Description": "Test Description Added",
      "Comments": null,
      "CreatedDate": "2020-06-08T07:06:08.000+0000"
    },
    {
      "attributes": {
        "type": "Case",
        "url": "/services/data/v47.0/sobjects/Case/5006g00000ENTPYAA5"
      },
      "Id": "5006g00000ENTPYAA5",
      "OwnerId": "0056g000004BDUuAAO",
      "ContactId": "0036g00000IxnouAAB",
      "AccountId": "0016g00000Dhx6rAAB",
      "Priority": "High",
      "Reason": "New problem",
      "Origin": "Phone",
      "Subject": "Test Subject 2",
      "Status": "On Hold",
      "Type": "Question",
      "CaseNumber": "00001004",
      "Description": "Test Subject 2 description",
      "Comments": null,
      "CreatedDate": "2020-06-08T07:20:12.000+0000"
    }
  ]

const actionCaseInfo ={
  'status':204
}

const accountInfo = [
    {
      "attributes": {
        "type": "Account",
        "url": "/services/data/v47.0/sobjects/Account/0016g00000Dhur2AAB"
      },
      "Name": "SteveBallmer",
      "Id": "0016g00000CPlwuAAD"
    }
  ]

const contactInfo = [
    {
      "attributes": {
        "type": "Contact",
        "url": "/services/data/v47.0/sobjects/Contact/0036g00000AYl7OAAT"
      },
      "Name": "Edward Stamos",
      "Id":"0016g00000Dhur2AAB"
    }
  ]

const emptyContactCasesInfo =  [
  {
    "attributes": {
      "type": "Case",
      "url": "/services/data/v47.0/sobjects/Case/5006g00000ENTNcAAP"
    },
    "Id": "5006g00000ENTNcAAP",
    "OwnerId": "0056g000004BDUuAAO",
    "ContactId": null,
    "AccountId": null,
    "Priority": "High",
    "Reason": "New problem",
    "Origin": "Email",
    "Subject": "Test Subject Added",
    "Status": "Closed",
    "Type": "Problem",
    "CaseNumber": "00001003",
    "Description": "Test Description Added",
    "Comments": null,
    "CreatedDate": "2020-06-08T07:06:08.000+0000"
  },
  {
    "attributes": {
      "type": "Case",
      "url": "/services/data/v47.0/sobjects/Case/5006g00000ENTPYAA5"
    },
    "Id": "5006g00000ENTPYAA5",
    "OwnerId": "0056g000004BDUuAAO",
    "ContactId": null,
    "AccountId": null,
    "Priority": "High",
    "Reason": "New problem",
    "Origin": "Phone",
    "Subject": "Test Subject 2",
    "Status": "On Hold",
    "Type": "Question",
    "CaseNumber": "00001004",
    "Description": "Test Subject 2 description",
    "Comments": null,
    "CreatedDate": "2020-06-08T07:20:12.000+0000"
  }
]

const feedItemResp = {
  'status':204
}

