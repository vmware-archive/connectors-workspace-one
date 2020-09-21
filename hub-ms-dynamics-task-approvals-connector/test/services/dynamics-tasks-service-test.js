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
const { getUserInfo, getMyActiveTasks, markTaskAsCompleted, closeTask, getCurrentUserMailFromId } = require('../../services/dynamics-tasks-service')
const sinonChai = require('sinon-chai')
const { response } = require('express')
const e = require('express')
chai.use(sinonChai)

describe('Service API tests', () => {
  describe('getUserInfo', () => {
    it('Get userInfo', async () => {
      nock('https://org.crm8.dynamics.com')
        .get('/api/data/v9.0/WhoAmI')
        .reply(200, userInfoMockResp)
      const response = await getUserInfo({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://org.crm8.dynamics.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      })
      expect(typeof response).to.equal('object')
      expect(response.UserId).to.equal('0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a')
    })

    it('Get userInfo for empty resp', async () => {
      nock('https://org.crm8.dynamics.com')
        .get('/api/data/v9.0/WhoAmI')
        .reply(200, "")
      const response = await getUserInfo({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://org.crm8.dynamics.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      })
      expect(response).to.equal("")
    })

    it('get users in network error', async () => {
      nock('https://org.crm8.dynamics.com')
        .get('/api/data/v9.0/WhoAmI')
        .reply(401)
      try {
        await getUserInfo({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://org.crm8.dynamics.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        })
        sinon.assert.fail('getUsers should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  
  })

  describe('getCurrentUserActiveTasks', () => {
    it('getActiveTasks valid flow', async () => {
       nock('https://org.crm8.dynamics.com')
        .get("/api/data/v9.0/tasks")
        .query(true)
        .reply(200, userTasks)
      const response = await getMyActiveTasks({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://org.crm8.dynamics.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, '0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a' )
      expect(typeof response).to.equal('object')
      expect(response.length).to.equal(1)
     expect(response[0].subject).to.equal('Provide product information')
    })

    it('getActiveTasks in network error', async () => {
      nock('https://org.crm8.dynamics.com')
        .get("/api/data/v9.0/tasks")
        .query(true)
        .reply(401)
      try {
        await getMyActiveTasks({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://org.crm8.dynamics.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, '0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a')
        sinon.assert.fail('getActiveTasks should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })

  describe('markTaskAsCompleted', () => {
    it('markTaskAsCompleted valid path', async () => {
      nock('https://org.crm8.dynamics.com',)
        .patch(uri => uri.includes('1c4095e5-60c6-ea11-a812-000d3a3e1af1'),
        {
          "statecode": 1,
          "statuscode": -1
      }
      )
        .reply(204,taskInfo)
      const response = await markTaskAsCompleted({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://org.crm8.dynamics.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, "1c4095e5-60c6-ea11-a812-000d3a3e1af1")
      expect(response).to.equal(204)
    })

    it('markTaskAsCompleted in network error', async () => {
      nock('https://org.crm8.dynamics.com')
      .patch(uri => uri.includes('1c4095e5-60c6-ea11-a812-000d3a3e1af1'),
      {
        "statecode": 1,
        "statuscode": -1
    }
    )
      .reply(401)
      try {
        await markTaskAsCompleted({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://org.crm8.dynamics.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, '1c4095e5-60c6-ea11-a812-000d3a3e1af1')
        sinon.assert.fail('addNotesToTask should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })

  describe('CloseTask', () => {
    it('closeTask valid flow', async () => {
      nock('https://org.crm8.dynamics.com')
      .patch(uri => uri.includes('1c4095e5-60c6-ea11-a812-000d3a3e1af1'),
      {
        "statecode":2,
        "statuscode":6
      } 
      )
      .query(true)
        .reply(204,taskInfo)
      const response = await closeTask({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://org.crm8.dynamics.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      },'1c4095e5-60c6-ea11-a812-000d3a3e1af1')
      expect(response).to.equal(204)
    })

    it('closeTask in network error', async () => {
      nock('https://org.crm8.dynamics.com')
      .patch(uri => uri.includes('1c4095e5-60c6-ea11-a812-000d3a3e1af1'),
      {
        "statecode":2,
        "statuscode":6
      } 
      )
      .query(true)
        .reply(401)
      try {
        await closeTask({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://org.crm8.dynamics.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, '1c4095e5-60c6-ea11-a812-000d3a3e1af1')
        sinon.assert.fail('resolveTask should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })

  describe('getCurrentUserMailFromId', () => {
    it('getCurrentUserMailFromId Valid Flow', async () => {
      nock('https://org.crm8.dynamics.com')
      .get(uri => uri.includes('0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a'))
      .query(true)
        .reply(200, ownerInfo)
      const response = await getCurrentUserMailFromId({
        connectorAuthorization: 'Bearer connectorAuthorizationValue',
        apiBaseUrl: 'https://org.crm8.dynamics.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, "0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a")
      expect(typeof response).to.equal('string')
     expect(response).to.equal('pavan@xen22.onmicrosoft.com')
    })

    it('getCurrentUserMailFromId in network error', async () => {
      nock('https://org.crm8.dynamics.com')
      .get(uri => uri.includes('0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a'))
        .query(true)
      .reply(401)
      try { await getCurrentUserMailFromId({
          connectorAuthorization: 'Bearer connectorAuthorizationValue',
          apiBaseUrl: 'https://org.crm8.dynamics.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, "0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a")
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }   
    })  
  })
})

const userInfoMockResp = {
  "@odata.context": "https://org.crm8.dynamics.com/api/data/v9.0/$metadata#Microsoft.Dynamics.CRM.WhoAmIResponse",
  "BusinessUnitId": "28f42def-e8b5-ea11-a812-000d3a3dfff4",
  "UserId": "0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a",
  "OrganizationId": "5a9af9ec-9024-4887-8068-a0053d12584d"
}

const userTasks = {
  "@odata.context": "https://org.crm8.dynamics.com/api/data/v9.0/$metadata#tasks(subject,description,activityid,prioritycode,createdon,scheduledend,statuscode,scheduleddurationminutes,actualdurationminutes,_regardingobjectid_value)",
  "@Microsoft.Dynamics.CRM.totalrecordcount": -1,
  "@Microsoft.Dynamics.CRM.totalrecordcountlimitexceeded": false,
  "value": [
    {
      "@odata.etag": "W/\"4260710\"",
      "subject": "Provide product information",
      "description": "Product information is missing in the catalog",
      "activityid": "1c4095e5-60c6-ea11-a812-000d3a3e1af1",
      "prioritycode@OData.Community.Display.V1.FormattedValue": "Low",
      "prioritycode": 0,
      "createdon@OData.Community.Display.V1.FormattedValue": "15-07-2020 11:33",
      "createdon": "2020-07-15T06:03:26Z",
      "scheduledend@OData.Community.Display.V1.FormattedValue": "15-07-2020 08:00",
      "scheduledend": "2020-07-15T02:30:00Z",
      "statuscode@OData.Community.Display.V1.FormattedValue": "Not Started",
      "statuscode": 2,
      "scheduleddurationminutes@OData.Community.Display.V1.FormattedValue": "0",
      "scheduleddurationminutes": 0,
      "actualdurationminutes@OData.Community.Display.V1.FormattedValue": "30",
      "actualdurationminutes": 30,
      "_regardingobjectid_value@OData.Community.Display.V1.FormattedValue": "SU John",
      "_regardingobjectid_value@Microsoft.Dynamics.CRM.associatednavigationproperty": "regardingobjectid_contact_task",
      "_regardingobjectid_value@Microsoft.Dynamics.CRM.lookuplogicalname": "contact",
      "_regardingobjectid_value": "7017e397-5ec6-ea11-a812-000d3a3e1af1"
    }
  ]
}

const ownerInfo = {
  "@odata.context": "https://org.crm8.dynamics.com/api/data/v9.0/$metadata#systemusers(internalemailaddress)/$entity",
  "@odata.etag": "W/\"3714569\"",
  "internalemailaddress": "pavan@xen22.onmicrosoft.com",
  "systemuserid": "0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a",
  "ownerid": "0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a"
}

const taskInfo ={
  'status':204
}


