/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

const rp = require('request-promise')
const { expect } = require('chai')
const testUtils = require('./test-utils')

const baseUrl = 'http://localhost:3000'
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const mockLinkedin = require('./mock-linkedin')
const MF_SERVER_PORT = 5000

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

describe('Hub LinkedIn Learning connector tests', () => {
  let server

  before(() => {
    if (!server) {
      mfCommons.mockMfServer.start(MF_SERVER_PORT)
      mockLinkedin.start()

      process.env.MF_JWT_PUB_KEY_URI = `http://localhost:${MF_SERVER_PORT}/security/public-key`
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
        mfCommons.mockMfServer.stop(() => {
          mockLinkedin.stop(() => {
            done()
          })
        })
      })
    } else {
      done()
    }
  })

  describe('Health', () => {
    it('should return status of up', async () => {
      const resp = await callHealth()
      expect(resp.statusCode).to.eql(200)
      expect(resp.body.status).to.eql('UP')
    })
  })

  describe('Discovery', () => {
    it('should return correct metadata', async () => {
      const resp = await callDiscovery()
      expect(resp.statusCode).to.eql(200)

      const expDiscovery = require('./connector/response/discovery')
      expect(resp.body).to.eql(expDiscovery)
    })
  })

  describe('New Courses', () => {
    it('should return new course object', async () => {
      const resp = await newCourses('jdoe', true, true)
      expect(resp.statusCode).to.eql(200)
      const expCourseObject = require('./connector/response/newCourses')

      resp.body.objects[0].id = 'random'
      resp.body.objects[0].backend_id = 'random'
      resp.body.objects[0].creation_date = 'random'
      resp.body.objects[0].hash = 'random'
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

  const newCourses = (username, shouldIncludeBaseURL, shouldIncludeConnectorAuth) => {
    const mfToken = mfCommons.mockMfServer.getMfToken({
      username: username,
      audience: 'https://my-host:3030/abc/api/cards'
    })
    const options = {
      method: 'POST',
      uri: `${testUtils.CONNECTOR_URL}/api/cards`,
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
