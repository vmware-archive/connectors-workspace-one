/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

const rp = require('request-promise')
const { expect } = require('chai')
const testUtils = require('./test-utils')

const baseUrl = 'http://localhost:3000'
const mockMobileFlows = require('./mock-mf-server')
const mockLinkedin = require('./mock-linkedin')

function callDiscovery () {
  const options = {
    method: 'GET',
    uri: `${baseUrl}/`,
    resolveWithFullResponse: true,
    json: true,
    qs: {},
    headers: {
      'X-Forwarded-Proto': 'https',
      'X-Forwarded-Prefix': '/abc',
      'X-Forwarded-Host': 'my-host',
      'X-Forwarded-Port': '3030'
    }
  }
  return rp(options)
}

function callHealth () {
  const options = {
    method: 'GET',
    uri: `${baseUrl}/health`,
    resolveWithFullResponse: true,
    json: true
  }
  return rp(options)
}

describe('Chatbot LinkedIn Learning connector tests', () => {
  let server

  before(() => {
    if (!server) {
      mockMobileFlows.start()
      mockLinkedin.start()

      process.env.MF_JWT_PUB_KEY_URI = testUtils.MF_JWT_PUB_KEY_URI
      try {
        delete require.cache[require.resolve('../index')]
        server = require('../index')
      } catch (e) {
        console.log('Something went wrong!', e)
        expect.fail('Something went wrong!')
      }
    }
  })

  after(done => {
    if (server) {
      server.close(() => {
        mockMobileFlows.stop(() => {
          mockLinkedin.stop(() => {
            done()
          })
        })
      })
    } else {
      done()
    }
  })

  describe('Discovery', () => {
    it('should return correct metadata', async () => {
      const resp = await callDiscovery()
      expect(resp.statusCode).to.eql(200)

      const expDiscovery = require('./connector/response/discovery')
      expect(resp.body).to.eql(expDiscovery)
    })
  })

  describe('Health', () => {
    it('should return status of up', async () => {
      const resp = await callHealth()
      expect(resp.statusCode).to.eql(200)
      expect(resp.body.status).to.eql('UP')
    })
  })

  describe('User Top Picks', () => {
    it('should return user top picks course object', async () => {
      const resp = await userTopPicks('jdoe', true, true)
      expect(resp.statusCode).to.eql(200)

      const expCourseObject = require('./connector/response/userTopPicks')
      resp.body.objects[0].itemDetails.id = 'random'
      resp.body.objects[1].itemDetails.id = 'random'
      resp.body.objects[2].itemDetails.id = 'random'
      expect(resp.body).to.eql(expCourseObject)
    })
  })

  describe('User Top Picks No Result', () => {
    it('should return no result object', async () => {
      const resp = await userTopPicks('userzero', true, true)
      expect(resp.statusCode).to.eql(200)

      const expCourseObject = require('./connector/response/noResult')
      expect(resp.body).to.eql(expCourseObject)
    })
  })

  describe('User Top Picks Missing BASE URL', () => {
    it('should return missing BASE URL Error', async () => {
      let errorRes
      try {
        await await userTopPicks('jdoe', false, true)
      } catch (e) {
        errorRes = e.response
      }
      expect(errorRes.statusCode).to.eql(400)
      expect(errorRes.body).to.eql({ message: 'The x-connector-base-url is required' })
    })
  })

  describe('User Top Picks Missing Connector Authorization', () => {
    it('should return missing connector authorization', async () => {
      let errorRes
      try {
        await await userTopPicks('jdoe', true, false)
      } catch (e) {
        errorRes = e.response
      }
      expect(errorRes.statusCode).to.eql(400)
      expect(errorRes.body).to.eql({ message: 'The x-connector-authorization is required' })
    })
  })

  describe('New Courses', () => {
    it('should return new course object', async () => {
      const resp = await newCourses('jdoe', true, true)
      expect(resp.statusCode).to.eql(200)

      const expCourseObject = require('./connector/response/newCourses')
      resp.body.objects[0].itemDetails.id = 'random'
      resp.body.objects[1].itemDetails.id = 'random'
      resp.body.objects[2].itemDetails.id = 'random'
      expect(resp.body).to.eql(expCourseObject)
    })
  })

  describe('New Courses No Result', () => {
    it('should return no result object', async () => {
      const resp = await newCourses('userzero', true, true)
      expect(resp.statusCode).to.eql(200)

      const expCourseObject = require('./connector/response/noResult')
      expect(resp.body).to.eql(expCourseObject)
    })
  })

  describe('New Courses Missing BASE URL', () => {
    it('should return missing BASE URL Error', async () => {
      let errorRes
      try {
        await await newCourses('jdoe', false, true)
      } catch (e) {
        errorRes = e.response
      }
      expect(errorRes.statusCode).to.eql(400)
      expect(errorRes.body).to.eql({ message: 'The x-connector-base-url is required' })
    })
  })

  describe('New Courses Picks Missing Connector Authorization', () => {
    it('should return missing connector authorization', async () => {
      let errorRes
      try {
        await await newCourses('jdoe', true, false)
      } catch (e) {
        errorRes = e.response
      }
      expect(errorRes.statusCode).to.eql(400)
      expect(errorRes.body).to.eql({ message: 'The x-connector-authorization is required' })
    })
  })

  describe('Keyword Search', () => {
    it('should return keyword search object', async () => {
      const resp = await keywordSeacrch('jdoe', true, true)
      expect(resp.statusCode).to.eql(200)

      const expCourseObject = require('./connector/response/keywordSearch')
      resp.body.objects[0].itemDetails.id = 'random'
      resp.body.objects[1].itemDetails.id = 'random'
      resp.body.objects[2].itemDetails.id = 'random'
      expect(resp.body).to.eql(expCourseObject)
    })
  })

  describe('Keyword Search No Result', () => {
    it('should return no result object', async () => {
      const resp = await keywordSeacrch('userzero', true, true)
      expect(resp.statusCode).to.eql(200)

      const expCourseObject = require('./connector/response/noResult')
      expect(resp.body).to.eql(expCourseObject)
    })
  })

  describe('Keyword Search Missing BASE URL', () => {
    it('should return missing BASE URL Error', async () => {
      let errorRes
      try {
        await await keywordSeacrch('jdoe', false, true)
      } catch (e) {
        errorRes = e.response
      }
      expect(errorRes.statusCode).to.eql(400)
      expect(errorRes.body).to.eql({ message: 'The x-connector-base-url is required' })
    })
  })

  describe('Keyword Search Picks Missing Connector Authorization', () => {
    it('should return missing connector authorization', async () => {
      let errorRes
      try {
        await await keywordSeacrch('jdoe', true, false)
      } catch (e) {
        errorRes = e.response
      }
      expect(errorRes.statusCode).to.eql(400)
      expect(errorRes.body).to.eql({ message: 'The x-connector-authorization is required' })
    })
  })

  const userTopPicks = (username, shouldIncludeBaseURL, shouldIncludeConnectorAuth) => {
    const mfToken = mockMobileFlows.getMfTokenFor(username, 'https://my-host:3030/abc/bot/actions/new-courses')
    const options = {
      method: 'GET',
      uri: `${testUtils.CONNECTOR_URL}/bot/actions/top-picks`,
      resolveWithFullResponse: true,
      json: true,
      qs: {},
      form: {
        description: 'get user top pick course'
      },
      headers: getHeaders(username, mfToken, shouldIncludeBaseURL, shouldIncludeConnectorAuth)
    }
    testUtils.addXForwardedHeaders(options)
    return rp(options)
  }

  const newCourses = (username, shouldIncludeBaseURL, shouldIncludeConnectorAuth) => {
    const mfToken = mockMobileFlows.getMfTokenFor(username, 'https://my-host:3030/abc/bot/actions/new-courses')
    const options = {
      method: 'GET',
      uri: `${testUtils.CONNECTOR_URL}/bot/actions/new-courses`,
      resolveWithFullResponse: true,
      json: true,
      qs: {},
      form: {
        description: 'get user new course'
      },
      headers: getHeaders(username, mfToken, shouldIncludeBaseURL, shouldIncludeConnectorAuth)
    }
    testUtils.addXForwardedHeaders(options)

    return rp(options)
  }
  const keywordSeacrch = (username, shouldIncludeBaseURL, shouldIncludeConnectorAuth) => {
    const mfToken = mockMobileFlows.getMfTokenFor(username, 'https://my-host:3030/abc/bot/actions/new-courses')
    const options = {
      method: 'POST',
      uri: `${testUtils.CONNECTOR_URL}/bot/actions/keyword-search`,
      resolveWithFullResponse: true,
      json: true,
      qs: {},
      form: {
        description: 'get user keyword search course'
      },
      headers: getHeaders(username, mfToken, shouldIncludeBaseURL, shouldIncludeConnectorAuth)
    }
    testUtils.addXForwardedHeaders(options)
    return rp(options)
  }

  const getHeaders = (username, mfToken, shouldIncludeBaseURL, shouldIncludeConnectorAuth) => {
    if (!shouldIncludeBaseURL) {
      return headersWithoutBaseURL(username, mfToken)
    }
    if (!shouldIncludeConnectorAuth) {
      return headersWithoutConnectorAuthorization(username, mfToken)
    }
    return {
      Authorization: `Bearer ${mfToken}`,
      'X-Connector-Base-Url': testUtils.BACKEND_BASE_URL,
      'X-Connector-Authorization': testUtils.BACKEND_ACCESS_TOKEN,
      'Content-Type': 'application/x-www-form-urlencoded',
      'X-Routing-Template': testUtils.MF_X_ROUTING_TEMPLATE,
      'x-request-id': username
    }
  }
  const headersWithoutBaseURL = (username, mfToken) => {
    return {
      Authorization: `Bearer ${mfToken}`,
      'X-Connector-Authorization': testUtils.BACKEND_ACCESS_TOKEN,
      'Content-Type': 'application/x-www-form-urlencoded',
      'X-Routing-Template': testUtils.MF_X_ROUTING_TEMPLATE,
      'x-request-id': username
    }
  }

  const headersWithoutConnectorAuthorization = (username, mfToken) => {
    return {
      Authorization: `Bearer ${mfToken}`,
      'X-Connector-Base-Url': testUtils.BACKEND_BASE_URL,
      'Content-Type': 'application/x-www-form-urlencoded',
      'X-Routing-Template': testUtils.MF_X_ROUTING_TEMPLATE,
      'x-request-id': username
    }
  }
})
