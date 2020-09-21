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
const { getUserInfo, updateTaskStatus,  getOpenTasks} = require('../../services/sfdc-task-service')
const sinonChai = require('sinon-chai')
const { response } = require('express')
const e = require('express')
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

    it('get userInfo in network error', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .get('/services/oauth2/userinfo')
        .reply(401)

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

  describe('getOpenTasks', () => {
    it('getOpenTasks valid flow', async () => {
       nock('https://bfvmw.my.salesforce.com')
        .get("/services/data/v47.0/query")
        .query(true)
        .reply(200, openTasks)
      const response = await getOpenTasks({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, 'b6e02089-3c93-4fac-b5f5-43faf4dfbd81' )
      expect(typeof response).to.equal('object')
      expect(response.length).to.equal(7)
      expect(response[0].Id).to.equal('00T6g00000D3CNyEAN')
    })

    it('getOpenTasks in network error', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .get("/services/data/v47.0/query")
        .query(true)
        .reply(401)
      try {
        await getOpenTasks({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://bfvmw.my.salesforce.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, 'b6e02089-3c93-4fac-b5f5-43faf4dfbd81')
        sinon.assert.fail('getOpenTasks should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })

  describe('updateTaskStatus', () => {
    it('updateTaskStatus happy path', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .patch('/services/data/v47.0/sobjects/Task/123',  {'Status':'completed'})
        .reply(204, actionTaskInfo)
      const response = await updateTaskStatus({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://bfvmw.my.salesforce.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, "123", "completed")
      console.log("response")
      console.log(response)
      expect(response).to.equal(204)

    })
    it('updateTaskStatus in network error', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .patch('/services/data/v47.0/sobjects/Task/123',  {'Status':'completed'})
        .reply(401)
      try {
        await updateTaskStatus({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://bfvmw.my.salesforce.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, '123', 'completed')
        sinon.assert.fail('updateTaskStatus should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })

    it('updateTaskStatus in network error status 403', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .patch('/services/data/v47.0/sobjects/Task/123',  {'Status':'completed'})
        .reply(403, 'Bad_OAuth_Token')
      try {
        await updateTaskStatus({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://bfvmw.my.salesforce.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, '123', 'completed')
        sinon.assert.fail('updateTaskStatus should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
    it('updateTaskStatus in network error status 403 with different err', async () => {
      nock('https://bfvmw.my.salesforce.com')
        .patch('/services/data/v47.0/sobjects/Task/123',
        {'Status':'completed'})
        .reply(403, 'missing_oauth_token')
      try {
        await updateTaskStatus({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://bfvmw.my.salesforce.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, '123', 'completed')
        sinon.assert.fail('updateTaskStatus should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })
})


const userInfoMockResp ={
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

const actionTaskInfo ={
  'status':204
}


const openTasks = {
  "totalSize": 7,
  "done": true,
  "records": [
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
  "Name": "Acme-customer"
  },
  "Subject": "Send Quote",
  "ActivityDate": "2020-02-20",
  "OwnerId": "0056g000004AiJXAA0",
  "Priority": "High",
  "Description": "Prepare a quote for WS1 and send it to Tim"
  },
  {
  "attributes": {
  "type": "Task",
  "url": "/services/data/v47.0/sobjects/Task/00T6g00000D5AZOEA3"
  },
  "Id": "00T6g00000D5AZOEA3",
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
  "Name": "Acme-customer"
  },
  "Subject": "Send Quote",
  "ActivityDate": "2020-02-20",
  "OwnerId": "0056g000004AeKUAA0",
  "Priority": "Normal",
  "Description": "Work with Tim and send an updated quote."
  },
  {
  "attributes": {
  "type": "Task",
  "url": "/services/data/v47.0/sobjects/Task/00T6g00000D5B35EAF"
  },
  "Id": "00T6g00000D5B35EAF",
  "Status": "In Progress",
  "Who": {
  "attributes": {
  "type": "Name",
  "url": "/services/data/v47.0/sobjects/Contact/0036g00000BdtFAAAZ"
  },
  "Name": "Jason Small",
  "Type": "Contact"
  },
  "What": {
  "attributes": {
  "type": "Name",
  "url": "/services/data/v47.0/sobjects/Account/0016g00000CYNtEAAX"
  },
  "Type": "Account",
  "Name": "Acme-si"
  },
  "Subject": "Call",
  "ActivityDate": "2020-02-20",
  "OwnerId": "0056g000004AeKUAA0",
  "Priority": "Normal",
  "Description": "Call Jason and get an update on the budget for WS1 deployment."
  },
  {
  "attributes": {
  "type": "Task",
  "url": "/services/data/v47.0/sobjects/Task/00T6g00000D5DwCEAV"
  },
  "Id": "00T6g00000D5DwCEAV",
  "Status": "Not Started",
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
  "Name": "Acme-customer"
  },
  "Subject": "Follow-up w/ Tim",
  "ActivityDate": "2020-02-21",
  "OwnerId": "0056g000004AeKUAA0",
  "Priority": "Normal",
  "Description": null
  },
  {
  "attributes": {
  "type": "Task",
  "url": "/services/data/v47.0/sobjects/Task/00T6g00000D5oz9EAB"
  },
  "Id": "00T6g00000D5oz9EAB",
  "Status": "Not Started",
  "Who": {
  "attributes": {
  "type": "Name",
  "url": "/services/data/v47.0/sobjects/Lead/00Q6g000004KOX4EAO"
  },
  "Name": "Andy Smith",
  "Type": "Lead"
  },
  "What": null,
  "Subject": "call new lead",
  "ActivityDate": "2020-02-21",
  "OwnerId": "0056g000004AeKUAA0",
  "Priority": "Normal",
  "Description": null
  },
  {
  "attributes": {
  "type": "Task",
  "url": "/services/data/v47.0/sobjects/Task/00T6g00000D5s2YEAR"
  },
  "Id": "00T6g00000D5s2YEAR",
  "Status": "Not Started",
  "Who": {
  "attributes": {
  "type": "Name",
  "url": "/services/data/v47.0/sobjects/Lead/00Q6g000004KOX4EAO"
  },
  "Name": "Andy Smith",
  "Type": "Lead"
  },
  "What": null,
  "Subject": "send email",
  "ActivityDate": null,
  "OwnerId": "0056g000004AeKUAA0",
  "Priority": "Normal",
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
  "What": {
  "attributes": {
  "type": "Name",
  "url": "/services/data/v47.0/sobjects/Opportunity/0066g00001MmKl3AAF"
  },
  "Type": "Opportunity",
  "Name": "Opp 24 Feb"
  },
  "Subject": "Test",
  "ActivityDate": null,
  "OwnerId": "0056g000004AfGUAA0",
  "Priority": "Normal",
  "Description": null
  }
  ]
  }




