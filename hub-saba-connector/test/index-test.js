/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const fetch = require('node-fetch')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const { expect } = require('chai')
const mockSaba = require('./mock-saba')
const testUtils = require('./test-utils')

const MF_SERVER_PORT = 5000
const CONNECTOR_PORT = 3000

const X_CONNECTOR_BASE_URL = 'http://localhost:4000'
const X_CONNECTOR_AUTHORIZATION = 'user1:password1'

describe('Saba connector tests', () => {
  let server

  before(() => {
    if (!server) {
      mfCommons.mockMfServer.start(MF_SERVER_PORT)
      mockSaba.start()

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
      mockSaba.stop()
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

      const expDiscovery = require('./connector/response/discovery')
      expect(response.status).to.eql(200)
      expect(actualJson).to.eql(expDiscovery)
    })
  })

  describe('X-Connector headers', () => {
    it('should reject if backend base URL is not present', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'harshas',
        audience: 'https://my-host:3030/abc/api/cards/requests'
      })
      const response = await callCardRequests(mfToken, '', X_CONNECTOR_AUTHORIZATION)
      const actualJson = await response.json()

      expect(response.status).to.eql(400)
      expect(actualJson.message).to.eql('Backend API base URL is required')
    })
  })

  describe('X-Connector headers', () => {
    it('should reject if backend authorization is not present', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'harshas',
        audience: 'https://my-host:3030/abc/api/cards/requests'
      })
      const response = await callCardRequests(mfToken, X_CONNECTOR_BASE_URL, undefined)
      const actualJson = await response.json()

      expect(response.status).to.eql(400)
      expect(actualJson.message).to.eql('Failed to acquire Saba certificate. Please Confirm service credentials are correct')
    })
  })

  describe('MF token audience claim', () => {
    it('should reject unauthorized', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'harshas',
        audience: 'https://different-connector.com/api/cards/requests'
      })
      const response = await callCardRequests(mfToken, X_CONNECTOR_BASE_URL, X_CONNECTOR_AUTHORIZATION)

      expect(response.status).to.eql(401)
    })
  })

  /*
   * Covers test for:
   * - A curriculum card
   * - A certification cad
   * - A course card
   * - Multiple modules in a path
   * - Multiple paths in a learning.
   */
  describe('Card requests', () => {
    it('is success', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'harshas',
        audience: 'https://my-host:3030/abc/api/cards/requests'
      })
      const response = await callCardRequests(mfToken, X_CONNECTOR_BASE_URL, X_CONNECTOR_AUTHORIZATION)
      const actualJson = await response.json()

      testUtils.replaceCardsUuid(actualJson)

      const expCardsResponse = require('./connector/response/cards-success.json')

      expect(response.status).to.eql(200)
      expect(actualJson).to.eql(expCardsResponse)
    })
  })

  describe('Card requests', () => {
    it('for empty learnings is success', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'sumit',
        audience: 'https://my-host:3030/abc/api/cards/requests'
      })
      const response = await callCardRequests(mfToken, X_CONNECTOR_BASE_URL, X_CONNECTOR_AUTHORIZATION)
      const actualJson = await response.json()

      const expCardsResponse = require('./connector/response/cards-success-sumit.json')

      expect(response.status).to.eql(200)
      expect(actualJson).to.eql(expCardsResponse)
    })
  })

  describe('Card requests', () => {
    it('for 1 course type learning only is success', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'fisher',
        audience: 'https://my-host:3030/abc/api/cards/requests'
      })
      const response = await callCardRequests(mfToken, X_CONNECTOR_BASE_URL, X_CONNECTOR_AUTHORIZATION)
      const actualJson = await response.json()

      testUtils.replaceCardsUuid(actualJson)

      const expCardsResponse = require('./connector/response/cards-success-fisher.json')

      expect(response.status).to.eql(200)
      expect(actualJson).to.eql(expCardsResponse)
    })
  })

  describe('Card requests', () => {
    it('for unrelated curriculum and a course', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'ryan',
        audience: 'https://my-host:3030/abc/api/cards/requests'
      })
      const response = await callCardRequests(mfToken, X_CONNECTOR_BASE_URL, X_CONNECTOR_AUTHORIZATION)
      const actualJson = await response.json()

      testUtils.replaceCardsUuid(actualJson)

      const expCardsResponse = require('./connector/response/cards-success-ryan.json')

      expect(response.status).to.eql(200)
      expect(actualJson).to.eql(expCardsResponse)
    })
  })

  /*
   * The mock Saba mimics that certificate is now revoked at the time user1 request cards.
   */
  describe('Saba certificate revoked', () => {
    it('should indicate unauthorized in the x-backend-status', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'user1',
        audience: 'https://my-host:3030/abc/api/cards/requests'
      })
      const response = await callCardRequests(mfToken, X_CONNECTOR_BASE_URL, X_CONNECTOR_AUTHORIZATION)

      expect(response.status).to.eql(400)
      expect(response.headers.get('x-backend-status')).to.eql('401')
    })
  })

  /*
   * This is to make sure our code doesn't fail to report backend status on an error
   * which is different from unauthorized case.
   */
  describe('Any http error response from Saba', () => {
    it('should send same status in the x-backend-status', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'user2',
        audience: 'https://my-host:3030/abc/api/cards/requests'
      })
      const response = await callCardRequests(mfToken, X_CONNECTOR_BASE_URL, X_CONNECTOR_AUTHORIZATION)

      expect(response.status).to.eql(500)
      expect(response.headers.get('x-backend-status')).to.eql('500')
    })
  })

  describe('Bogus users trying for cards', () => {
    it('should return empty objects', async () => {
      const mfToken = mfCommons.mockMfServer.getMfToken({
        username: 'bogus',
        audience: 'https://my-host:3030/abc/api/cards/requests'
      })
      const response = await callCardRequests(mfToken, X_CONNECTOR_BASE_URL, X_CONNECTOR_AUTHORIZATION)
      const actualJson = await response.json()

      expect(response.status).to.eql(200)
      expect(actualJson).to.eql({ objects: [] })
    })
  })
})

const callDiscovery = async () => {
  const options = {}
  testUtils.addXForwardedHeaders(options)
  return fetch(`http://localhost:${CONNECTOR_PORT}/`, options)
}

const callCardRequests = async (mfToken, xConnectorBaseUrl, xConnectorAuthorization) => {
  const options = {
    method: 'POST',
    body: {},
    headers: {
      Authorization: `Bearer ${mfToken}`,
      'X-Connector-Base-Url': xConnectorBaseUrl,
      'X-Connector-Authorization': xConnectorAuthorization,
      'X-Routing-Prefix': `http://localhost:${MF_SERVER_PORT}/connectors/123/card/`
    }
  }
  testUtils.addXForwardedHeaders(options)
  return fetch(`http://localhost:${CONNECTOR_PORT}/api/cards/requests`, options)
}
