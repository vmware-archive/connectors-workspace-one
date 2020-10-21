/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const { expect } = require('chai')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const MF_SERVER_PORT = 5000
const fetch = require('node-fetch')
const testUtils = require('./test-utils')
const CONNECTOR_PORT = 3000
const X_CONNECTOR_BASE_URL = 'http://localhost:4000'
const mockDynamics = require('./mock-dynamics')

describe('MS Dynamics Test Case', () => {
  let server

  before(() => {
    if (!server) {
      mfCommons.mockMfServer.start(MF_SERVER_PORT)
      mockDynamics.start()
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

  after(() => {
    if (server) {
      server.close()
      mfCommons.mockMfServer.stop()
      mockDynamics.stop()
    }
  })

  describe('Health', () => {
    it('should return status of up', async () => {
      const response = await fetch(`http://localhost:${CONNECTOR_PORT}/health`)
      const actualJson = await response.json()
      expect(response.status).to.eql(200)
      expect(actualJson.status).to.eql('UP')
    })
  })

  describe('Discovery', () => {
    it('should return correct metadata', async () => {
      const response = await callDiscovery()
      const actualJson = await response.json()
      expect(mfCommons.validateDiscovery(actualJson).valid).to.eql(true)
      const expDiscovery = require('./connector/response/discovery-metadata')
      expect(response.status).to.eql(200)
      expect(actualJson).to.eql(expDiscovery)
    })
  })

  describe('X-Connector headers', () => {
    it('should reject if backend base URL is not present', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'ansoni',
        audience: 'https://my-host:3030/abc/bot/actions/pendingCases'
      })
      const response = await callBotApi(mfToken, 'good-token', '', 'GET')
      const actualJson = await response.json()
      expect(response.status).to.eql(400)
      expect(actualJson.message).to.eql('The x-connector-base-url is required')
    })
  })

  describe('with-single-case', () => {
    it('with single case', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'ansoni',
        audience: 'https://my-host:3030/abc/bot/actions/pendingCases'
      })
      const authorization = 'DYNAMICS_TOKEN_FOR_USER_ONE'
      const response = await callBotApi(mfToken, authorization, X_CONNECTOR_BASE_URL, 'GET')
      const actualJson = await response.json()
      testUtils.replaceCardsUuid(actualJson)
      expect(mfCommons.validateBotObject(actualJson).valid).to.eql(true)
      const expBotObject = require('./connector/response/connector-response-with-single-case')
      expect(actualJson).to.eql(expBotObject)
    })
  })

  describe('with-multiple-cases', () => {
    it('with multiple cases', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'ansoni',
        audience: 'https://my-host:3030/abc/bot/actions/pendingCases'
      })
      const authorization = 'DYNAMICS_TOKEN_FOR_USER_TWO'
      const response = await callBotApi(mfToken, authorization, X_CONNECTOR_BASE_URL, 'GET')
      const actualJson = await response.json()
      testUtils.replaceCardsUuid(actualJson)
      expect(mfCommons.validateBotObject(actualJson).valid).to.eql(true)
      const expBotObject = require('./connector/response/connector-response-with-multiple-cases')
      expect(actualJson).to.eql(expBotObject)
    })
  })

  describe('with-no-cases', () => {
    it('with no cases', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'ansoni',
        audience: 'https://my-host:3030/abc/bot/actions/pendingCases'
      })
      const authorization = 'DYNAMICS_TOKEN_FOR_USER_THREE'
      const response = await callBotApi(mfToken, authorization, X_CONNECTOR_BASE_URL, 'GET')
      const actualJson = await response.json()
      expect(mfCommons.validateBotObject(actualJson).valid).to.eql(true)
      const expBotObject = require('./connector/response/connector-respose-with-no-cases')
      expect(actualJson).to.eql(expBotObject)
    })
  })

  const callDiscovery = async () => {
    const options = {}
    testUtils.addXForwardedHeaders(options)
    return fetch(`http://localhost:${CONNECTOR_PORT}/`, options)
  }

  const callBotApi = async (mfToken, dynamicsToken, xConnectorBaseUrl, requestType) => {
    const options = {
      method: `${requestType}`,
      headers: {
        Authorization: `Bearer ${mfToken}`,
        'X-Connector-Authorization': `${dynamicsToken}`,
        'X-Connector-Base-Url': `${xConnectorBaseUrl}`
      }
    }
    testUtils.addXForwardedHeaders(options)
    return fetch(`http://localhost:${CONNECTOR_PORT}/bot/actions/pendingCases`, options)
  }
})
