/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { describe } = require('mocha')
const { expect } = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const service = require('../../services/sfdc-task-service')
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
    const openTasksStub = sinon.stub(service, 'getOpenTasks').returns(Promise.resolve(OpenTasks))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    openTasksStub.restore()

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
    const openTasksStub = sinon.stub(service, 'getOpenTasks').returns(Promise.resolve(OpenTasks))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    openTasksStub.restore()
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
    const openTasksStub = sinon.stub(service, 'getOpenTasks').returns(Promise.resolve([]))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    openTasksStub.restore()
  
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
    const OpenTasks = sinon.stub(service, 'getOpenTasks').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 401 }))))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(backendStatus).to.equal(401)
    expect(statusCode).to.equal(400)
    getUserInfoStub.restore()
    OpenTasks.restore()
  })
})


const userInfo ={
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
const OpenTasks = [
  {
  "attributes": {
  "type": "Task",
  "url": "/services/data/v47.0/sobjects/Task/00T6g00000D3CNyEAN"
  },
  "Id": "00T6g00000D3CNyEAN",
  "Status": "In Progress",
  "Who": {
  "attributes": {
  "type": "Name",
  "url": "/services/data/v47.0/sobjects/Contact/0036g00000BdtE7AAJ"
  },
  "Name": "Tim Kelly",
  "Type": "Contact"
  },
  "What": {
  "attributes": {
  "type": "Name",
  "url": "/services/data/v47.0/sobjects/Account/0016g00000CYNqKAAX"
  },
  "Type": "Account",
  "Name": ""
  },
  "Subject": "",
  "ActivityDate": "2020-02-20",
  "OwnerId": "0056g000004AiJXAA0",
  "Priority": "High",
  "Description": "Prepare a quote for WS1 and send it to Tim"
  },
  {
    "attributes": {
    "type": "Task",
    "url": "/services/data/v47.0/sobjects/Task/00T6g00000D6vCsEAJ"
    },
    "Id": "00T6g00000D6vCsEAJ",
    "Status": null,
    "Who": null,
    "What": {
    "attributes": {
    "type": "Name",
    "url": "/services/data/v47.0/sobjects/Opportunity/0066g00001MmKl3AAF"
    },
    "Type": "Opportunity",
    "Name": undefined
    },
    "Subject": "Test",
    "ActivityDate": null,
    "OwnerId": "0056g000004AfGUAA0",
    "Priority": "",
    "Description": null
    },
    {
      "attributes": {
      "type": "Task",
      "url": "/services/data/v47.0/sobjects/Task/00T6g00000D6vCsEAJ"
      },
      "Id": "00T6g00000D6vCsEAJ",
      "Status": "Not Started",
      "Who": null,
      "What": null,
      "Subject": null,
      "ActivityDate": null,
      "OwnerId": "0056g000004AfGUAA0",
      "Priority": "Normal",
      "Description": null
      }]

