/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { describe } = require('mocha')
const { expect } = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const nock = require('nock')
const { getUserInfo, getMyOpenCases, actionsOnCase,  getContactInfo, getAccountInfo, postFeedItemToCase } = require('../../services/sfdc-case-service')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('Service API tests', () => {
  describe('getUserInfo', () => {
    it('Get userInfo', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .get('/services/oauth2/userinfo')
        .reply(200, userInfoMockResp)
      const response = await getUserInfo({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      })
      expect(typeof response).to.equal('object')
      expect(response.user_id).to.equal('0056g0000049jc6AAA')
    })

    it('Get userInfo for empty resp', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .get('/services/oauth2/userinfo')
        .reply(200, "")
      const response = await getUserInfo({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      })
      expect(response).to.equal("")
    })

    it('get users in network error', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .get('/services/oauth2/userinfo')
        .reply(403,'Bad_OAuth_Token')
      try {
        await getUserInfo({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://bfvmw.my.salesforce.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        })
        sinon.assert.fail('getUsers should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })

  describe('getMyOpenCases', () => {
    it('getMyOpenCases valid flow', async () => {
       nock('https://bfvmw.my.salesforce.com')
        .get("/services/data/v47.0/query")
        .query(true)
        .reply(200, myOpenCases)
      const response = await getMyOpenCases({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, '0056g0000049jc6AAA' )
      expect(typeof response).to.equal('object')
      expect(response.length).to.equal(3)
     expect(response[0].Id).to.equal('5006g00000AboHzAAJ')
    })

    it('getMyOpenCases valid flow for empty resp', async () => {
       nock('https://bfvmw.my.salesforce.com')
        .get("/services/data/v47.0/query")
        .query(true)
        .reply(200, "") 
      const response = await getMyOpenCases({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, '0056g0000049jc6AAA' )
      expect(response).to.equal("")
    })

    it('getMyOpenCases in network error', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .get("/services/data/v47.0/query")
        .query(true)
        .reply(401)
      try {
        await getMyOpenCases({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://bfvmw.my.salesforce.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, '0056g0000049jc6AAA')
        sinon.assert.fail('getMyOpenCases should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })

  describe('actionsOnCase', () => {
    it('actionsOnCase happy path', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .patch('/services/data/v47.0/sobjects/Case/1234',
        {
          "Status": "close"
        }
        )
        .reply(204, actionCaseInfo)
      const response = await actionsOnCase({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, "close", '1234')
      expect(response).to.equal(204)
    })

    it('actionsOnCase in network error', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .patch('/services/data/v47.0/sobjects/Case/1234',
        {
          "Status": "close"
        }
        )
        .reply(403,'missing_oauth_token')
      try {
        await actionsOnCase({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://bfvmw.my.salesforce.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, "close", '1234')
        sinon.assert.fail('actionsOnCase should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  }) 

  describe('getContactInfo', () => {
    it('getContactInfo happy path', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .get('/services/data/v47.0/query')
        .query(true)
        .reply(200, contactInfo)
      const response = await getContactInfo({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, "0016g00000Dhur2AAB")
      expect(typeof response).to.equal('object')
      expect(response.length).to.equal(1)
     expect(response[0].Id).to.equal('0016g00000Dhur2AAB')
     expect(response[0].Name).to.equal('Edward Stamos')
    })

    it('contactInfo is empty', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .get('/services/data/v47.0/query')
        .query(true)
        .reply(200, "")
      const response = await getContactInfo({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, "0016g00000Dhur2AAB")
      expect(response).to.equal("")
    })

    it('getContactInfo in network error', async () => {
      nock('https://bfvmw.my.salesforce.com')
      .get('/services/data/v47.0/query')
      .query(true)
      .reply(401)
      try { await getContactInfo({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://bfvmw.my.salesforce.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, '0016g00000Dhur2AAB')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }   
    })
    it('getContactInfo in network error for empty result', async () => {
      nock('https://bfvmw.my.salesforce.com')
      .get('/services/data/v47.0/query')
      .query(true)
      .reply(400, "")
      try { await getContactInfo({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://bfvmw.my.salesforce.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, '0016g00000Dhur2AAB')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(400)
        expect(error.message).to.equal("")
      }   
    })
  })

  describe('getAccountInfo', () => {
    it('getAccountInfo happy path', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .get('/services/data/v47.0/query')
        .query(true)
        .reply(200, accountInfo)
      const response = await getAccountInfo({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, "0016g00000CPlwuAAD")
      expect(typeof response).to.equal('object')
      expect(response.length).to.equal(1)
     expect(response[0].Name).to.equal('SteveBallmer')
     expect(response[0].Id).to.equal('0016g00000CPlwuAAD')
    })

    it('getAccountInfo for empty resp', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .get('/services/data/v47.0/query')
        .query(true)
        .reply(200, "")
      const response = await getAccountInfo({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, "0016g00000CPlwuAAD")
      expect(response).to.equal("")
    })

    it('getAccountInfo in network error handle', async () => {
      nock('https://bfvmw.my.salesforce.com')
      .get('/services/data/v47.0/query')
      .query(true)
      .reply(401)
      try {
        await getAccountInfo({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://bfvmw.my.salesforce.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, '0016g00000CPlwuAAD')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
        }      
    })
})

describe('postFeedItemToCase', () => {
  it('postFeedItemToCase happy path', async () => {
    nock('https://bfvmw.my.salesforce.com')
      .post('/services/data/v47.0/chatter/feed-elements',
      {
        "body": {
          "messageSegments": [
            {
              "type": "Text",
              "text": "test comment"
            }
          ]
        },
        "feedElementType": "FeedItem",
        "subjectId": "5006g00000AboHzAAJ"
      }
      )
      .reply(201)
    const response = await postFeedItemToCase({
      connectorAuthorization: 'Bearer connectorAuthorizationValue',
      apiBaseUrl: 'https://bfvmw.my.salesforce.com',
      mfJwt: { email: 'srini.gargeya@vmware.com' }
    }, "5006g00000AboHzAAJ","test comment")
    expect(response).to.equal(201)
  })

  it('postFeedItemToCase for empty resp', async () => {
    nock('https://bfvmw.my.salesforce.com')
      .post('/services/data/v47.0/chatter/feed-elements',
      {
        "body": {
          "messageSegments": [
            {
              "type": "Text",
              "text": "test comment"
            }
          ]
        },
        "feedElementType": "FeedItem",
        "subjectId": "5006g00000AboHzAAJ"
      })
      .reply(200, "")
    const response = await postFeedItemToCase({
      connectorAuthorization: 'Bearer connectorAuthorizationValue',
      apiBaseUrl: 'https://bfvmw.my.salesforce.com',
      mfJwt: { email: 'srini.gargeya@vmware.com' }
    }, "5006g00000AboHzAAJ","test comment")
    expect(response).to.equal(200)
  })

  it('postFeedItemToCase in network error handle', async () => {
    nock('https://bfvmw.my.salesforce.com')
    .post('/services/data/v47.0/chatter/feed-elements',
    {
      "body": {
        "messageSegments": [
          {
            "type": "Text",
            "text": "test comment"
          }
        ]
      },
      "feedElementType": "FeedItem",
      "subjectId": "5006g00000AboHzAAJ"
    })
    .reply(401)
    try {
      await postFeedItemToCase({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, '5006g00000AboHzAAJ','test comment')
    } catch (err) {
      const error = JSON.parse(err.message)
      expect(error.statusCode).to.equal(401)
      }
  })
})
})

const userInfoMockResp = {
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

const myOpenCases = {
  "totalSize": 3,
  "done": true,
  "records": [
    {
      "attributes": {
        "type": "Case",
        "url": "/services/data/v47.0/sobjects/Case/5006g00000AboHzAAJ"
      },
      "Id": "5006g00000AboHzAAJ",
      "OwnerId": "0056g0000049jc6AAA",
      "Subject": "Sample Case 2: The widgets we received are the wrong size.",
      "Status": "New",
      "CaseNumber": "00001001"
    },
    {
      "attributes": {
        "type": "Case",
        "url": "/services/data/v47.0/sobjects/Case/5006g00000AboI0AAJ"
      },
      "Id": "5006g00000AboI0AAJ",
      "OwnerId": "0056g0000049jc6AAA",
      "Subject": "Sample Case 3: Cannot track our order.",
      "Status": "On Hold",
      "CaseNumber": "00001002"
    },
    {
      "attributes": {
        "type": "Case",
        "url": "/services/data/v47.0/sobjects/Case/5006g00000ENTPYAA5"
      },
      "Id": "5006g00000ENTPYAA5",
      "OwnerId": "0056g000004BDUuAAO",
      "Subject": "Test Subject 2",
      "Status": "New",
      "CaseNumber": "00001004"
    }
  ]
}

const actionCaseInfo ={
  'status':204
}

const accountInfo = {
   "totalSize": 1,
  "done": true,
  "records": [
    {
      "attributes": {
        "type": "Account",
        "url": "/services/data/v47.0/sobjects/Account/0016g00000Dhur2AAB"
      },
      "Name": "SteveBallmer",
      "Id": "0016g00000CPlwuAAD"
    }
  ]
}

const contactInfo = {
  "totalSize": 1,
  "done": true,
  "records": [
    {
      "attributes": {
        "type": "Contact",
        "url": "/services/data/v47.0/sobjects/Contact/0036g00000AYl7OAAT"
      },
      "Name": "Edward Stamos",
      "Id":"0016g00000Dhur2AAB"
    }
  ]
}

