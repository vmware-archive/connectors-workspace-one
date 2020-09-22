/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { describe } = require('mocha')
const { expect } = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const service = require('../../services/dynamics-case-service')
const { cardsController } = require('../../routes/cards-request')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)


describe('cards-controller', () => {
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
    const activeCases = sinon.stub(service, 'getActiveCases').returns(Promise.resolve(activeCasesInfo))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    activeCases.restore()
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
    const getActiveCasesInfo = sinon.stub(service, 'getActiveCases').returns(Promise.resolve([]))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    getUserInfoStub.restore()
    getActiveCasesInfo.restore()
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
    const activeCases = sinon.stub(service, 'getActiveCases').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 401 }))))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(backendStatus).to.equal(401)
    expect(statusCode).to.equal(400)
    getUserInfoStub.restore()
    activeCases.restore()
  })
})



const userInfo ={

  "@odata.context": "https://xen21.crm8.dynamics.com/api/data/v9.0/$metadata#Microsoft.Dynamics.CRM.WhoAmIResponse",
  "BusinessUnitId": "f0a9a357-819e-ea11-a812-000d3af263eb",
  "UserId": "b6e02089-3c93-4fac-b5f5-43faf4dfbd81",
  "OrganizationId": "774c0b90-80f8-42b0-a5b8-dbabe7756771"

}
const activeCasesInfo = [
    {
      "@odata.etag": "W/\"2654797\"",
      "statecode@OData.Community.Display.V1.FormattedValue": "Active",
      "statecode": 0,
      "resolvebyslastatus@OData.Community.Display.V1.FormattedValue": "In Progress",
      "resolvebyslastatus": 1,
      "statuscode@OData.Community.Display.V1.FormattedValue": "In Progress",
      "statuscode": 1,
      "createdon@OData.Community.Display.V1.FormattedValue": "26-06-2020 15:01",
      "createdon": "2020-06-26T09:31:17Z",
      "ticketnumber": "CAS-01002-S8K4Y2",
      "incidentstagecode@OData.Community.Display.V1.FormattedValue": "Default Value",
      "incidentstagecode": 1,
      "severitycode@OData.Community.Display.V1.FormattedValue": "Default Value",
      "severitycode": 1,
      "_ownerid_value@OData.Community.Display.V1.FormattedValue": "Pavan xen22",
      "_ownerid_value@Microsoft.Dynamics.CRM.associatednavigationproperty": "ownerid",
      "_ownerid_value@Microsoft.Dynamics.CRM.lookuplogicalname": "systemuser",
      "_ownerid_value": "0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a",
      "servicestage@OData.Community.Display.V1.FormattedValue": "Identify",
      "servicestage": 0,
      "modifiedon@OData.Community.Display.V1.FormattedValue": "26-06-2020 15:01",
      "modifiedon": "2020-06-26T09:31:17Z",
      "int_upsellreferral@OData.Community.Display.V1.FormattedValue": "No",
      "int_upsellreferral": false,
      "title": "Test Case 3",
      "decremententitlementterm@OData.Community.Display.V1.FormattedValue": "Yes",
      "decremententitlementterm": true,
      "versionnumber@OData.Community.Display.V1.FormattedValue": "26,54,797",
      "versionnumber": 2654797,
      "prioritycode@OData.Community.Display.V1.FormattedValue": "High",
      "prioritycode": 1,
      "blockedprofile@OData.Community.Display.V1.FormattedValue": "No",
      "blockedprofile": false,
      "checkemail@OData.Community.Display.V1.FormattedValue": "No",
      "checkemail": false,
      "_modifiedby_value@OData.Community.Display.V1.FormattedValue": "Pavan xen22",
      "_modifiedby_value@Microsoft.Dynamics.CRM.lookuplogicalname": "systemuser",
      "_modifiedby_value": "0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a",
      "isdecrementing@OData.Community.Display.V1.FormattedValue": "No",
      "isdecrementing": false,
      "firstresponseslastatus@OData.Community.Display.V1.FormattedValue": "In Progress",
      "firstresponseslastatus": 1,
      "routecase@OData.Community.Display.V1.FormattedValue": "Yes",
      "routecase": true,
      "_subjectid_value@OData.Community.Display.V1.FormattedValue": "Products",
      "_subjectid_value@Microsoft.Dynamics.CRM.associatednavigationproperty": "subjectid",
      "_subjectid_value@Microsoft.Dynamics.CRM.lookuplogicalname": "subject",
      "_subjectid_value": "191de3d1-21d5-e411-80eb-c4346bad3638",
      "followuptaskcreated@OData.Community.Display.V1.FormattedValue": "No",
      "followuptaskcreated": false,
      "firstresponsesent@OData.Community.Display.V1.FormattedValue": "No",
      "firstresponsesent": false,
      "merged@OData.Community.Display.V1.FormattedValue": "No",
      "merged": false,
      "activitiescomplete@OData.Community.Display.V1.FormattedValue": "No",
      "activitiescomplete": false,
      "_createdby_value@OData.Community.Display.V1.FormattedValue": "Pavan xen22",
      "_createdby_value@Microsoft.Dynamics.CRM.lookuplogicalname": "systemuser",
      "_createdby_value": "0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a",
      "_owningbusinessunit_value@Microsoft.Dynamics.CRM.associatednavigationproperty": "owningbusinessunit",
      "_owningbusinessunit_value@Microsoft.Dynamics.CRM.lookuplogicalname": "businessunit",
      "_owningbusinessunit_value": "28f42def-e8b5-ea11-a812-000d3a3dfff4",
      "_customerid_value@OData.Community.Display.V1.FormattedValue": "Test Account 1",
      "_customerid_value@Microsoft.Dynamics.CRM.associatednavigationproperty": "customerid_account",
      "_customerid_value@Microsoft.Dynamics.CRM.lookuplogicalname": "account",
      "_customerid_value": "5350cb01-a1b6-ea11-a812-000d3a3e1af1",
      "_owninguser_value@Microsoft.Dynamics.CRM.lookuplogicalname": "systemuser",
      "_owninguser_value": "0cbe9aa4-4ac2-4c4c-bed6-11e461d23c0a",
      "incidentid": "6571371e-5049-4929-9523-fb82efb23914",
      "isescalated@OData.Community.Display.V1.FormattedValue": "No",
      "isescalated": false,
      "customercontacted@OData.Community.Display.V1.FormattedValue": "No",
      "customercontacted": false,
      "_firstresponsebykpiid_value": null,
      "customersatisfactioncode": null,
      "_existingcase_value": null,
      "_createdbyexternalparty_value": null,
      "_slaid_value": null,
      "utcconversiontimezonecode": null,
      "_primarycontactid_value@OData.Community.Display.V1.FormattedValue": "Test Contact 1",
      "_primarycontactid_value@Microsoft.Dynamics.CRM.associatednavigationproperty": "primarycontactid",
      "_primarycontactid_value@Microsoft.Dynamics.CRM.lookuplogicalname": "contact",
      "_primarycontactid_value": "f60113c5-a0b6-ea11-a812-000d3a3e1af1",
      "_createdonbehalfby_value": null,
      "messagetypecode": null,
      "_resolvebykpiid_value": null,
      "_owningteam_value": null,
      "onholdtime": null,
      "_entitlementid_value": null,
      "resolveby": null,
      "numberofchildincidents": null,
      "_slainvokedid_value": null,
      "entityimage_timestamp": null,
      "stageid": null,
      "overriddencreatedon": null,
      "_productid_value@OData.Community.Display.V1.FormattedValue": "ArmBand 100",
      "_productid_value@Microsoft.Dynamics.CRM.associatednavigationproperty": "productid",
      "_productid_value@Microsoft.Dynamics.CRM.lookuplogicalname": "product",
      "_productid_value": "7147c934-1de7-e611-80f4-e0071b661f01",
      "importsequencenumber": null,
      "responseby": null,
      "_modifiedonbehalfby_value": null,
      "_parentcaseid_value": null,
      "timezoneruleversionnumber": null,
      "_accountid_value": null,
      "emailaddress": null,
      "_modifiedbyexternalparty_value": null,
      "actualserviceunits": null,
      "traversedpath": null,
      "contractservicelevelcode": null,
      "lastonholdtime": null,
      "_contractdetailid_value": null,
      "productserialnumber": null,
      "int_customereffort": null,
      "_contactid_value": null,
      "exchangerate": null,
      "_msdyn_iotalert_value": null,
      "influencescore": null,
      "entityimageid": null,
      "_kbarticleid_value": null,
      "_contractid_value": null,
      "billedserviceunits": null,
      "processid": "00000000-0000-0000-0000-000000000000",
      "followupby": null,
      "escalatedon": null,
      "casetypecode": null,
      "entityimage": null,
      "entityimage_url": null,
      "_masterid_value": null,
      "_socialprofileid_value": null,
      "caseorigincode": null,
      "_msdyn_incidenttype_value": null,
      "sentimentvalue": null,
      "_transactioncurrencyid_value": null,
      "description": "Products information needed"
    }
  ]

