'use strict'

const fetch = require('node-fetch')
const { expect } = require('chai')
const testUtils = require('./test-utils')

const baseUrl = 'http://localhost:3000'
const MF_SERVER_PORT = 5000

const mfCommons = require('@vmw/mobile-flows-connector-commons')
const mockZoomServer = require('./mock-zoom-server')

function callHealth () {
  return fetch(`${baseUrl}/health`)
}

const callDiscovery = async () => {
  const options = {
    headers: {
    }
  }
  testUtils.addXForwardedHeaders(options)

  return fetch(`${baseUrl}`, options)
}

const zoomRecordings = (mfAuth, connectorAuth, connectorBaseUrl) => {
  const options = {
    method: 'POST',
    body: {},
    headers: {
      Authorization: `Bearer ${mfAuth}`,
      'X-Connector-Base-Url': connectorBaseUrl,
      'X-Connector-Authorization': connectorAuth,
      'x-request-id': 'username'
    }
  }
  testUtils.addXForwardedHeaders(options)

  return fetch(`${testUtils.CONNECTOR_URL}/api/cards`, options)
}

describe('Hub Zoom recording connector tests', () => {
  let server

  before(() => {
    if (!server) {
      mfCommons.mockMfServer.start(MF_SERVER_PORT)
      mockZoomServer.start()

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
          mockZoomServer.stop(() => {
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
      const response = await callHealth()
      const actualJson = await response.json()

      expect(response.status).to.eql(200)
      expect(actualJson.status).to.eql('UP')
    })
  })

  describe('Discovery', () => {
    it('should return correct metadata', async () => {
      const discovery = require('./discovery')
      const response = await callDiscovery()
      const actualJson = await response.json()

      expect(response.status).to.eql(200)
      expect(actualJson).to.eql(discovery)
    })
  })

  describe('Zoom recordings', () => {
    it('should return 401 if authorization header is missing', async () => {
      const token = mfCommons.mockMfServer.getMfToken({
        username: 'username',
        audience: 'https://my-host:3030/abc/api/cards'
      })
      const baseUrl = testUtils.BACKEND_BASE_URL
      const response = await zoomRecordings('', token, baseUrl)
      const actualJson = await response.json()

      expect(response.status).to.eql(401)
      expect(actualJson.message).to.contain('Failed to validate MobileFlows Jwt')
    })

    it('should return 401 if authorization verification failed', async () => {
      const token = mfCommons.mockMfServer.getMfToken({
        username: 'username',
        audience: 'https://my-host:3030/abc/api/cards'
      })
      const baseUrl = testUtils.BACKEND_BASE_URL
      const invalidAuth = 'some-invalid-auth'
      const response = await zoomRecordings(invalidAuth, token, baseUrl)
      const actualJson = await response.json()

      expect(response.status).to.eql(401)
      expect(actualJson.message).to.contain('Failed to validate MobileFlows Jwt')
    })

    it('should return 400 if x-connector-base-url value is missing', async () => {
      const token = mfCommons.mockMfServer.getMfToken({
        username: 'username',
        audience: 'https://my-host:3030/abc/api/cards'
      })
      const response = await zoomRecordings(token, token, '')
      const actualJson = await response.json()

      expect(response.status).to.eql(400)
      expect(actualJson.message).to.contain('The x-connector-base-url is required')
    })

    it('should return 400 if x-connector-authorization value is missing', async () => {
      const token = mfCommons.mockMfServer.getMfToken({
        username: 'username',
        audience: 'https://my-host:3030/abc/api/cards'
      })
      const baseUrl = testUtils.BACKEND_BASE_URL
      const response = await zoomRecordings(token, '', baseUrl)
      const actualJson = await response.json()

      expect(response.status).to.eql(400)
      expect(actualJson.message).to.contain('The x-connector-authorization is required')
    })

    it('should return new recordings', async () => {
      const token = mfCommons.mockMfServer.getMfToken({
        username: 'username',
        audience: 'https://my-host:3030/abc/api/cards'
      })
      const baseUrl = testUtils.BACKEND_BASE_URL
      const recordings = require('./zoom-recordings')
      const response = await zoomRecordings(token, token, baseUrl)
      const actualJson = await response.json()

      expect(response.status).to.eql(200)
      expect(actualJson.objects.length).to.eql(recordings.meetings.length)
      expect(actualJson.objects[0].backend_id).to.eql('4752962893')
      expect(actualJson.objects[0].header.title).to.eql('Cloud Recording Now Available')
      expect(actualJson.objects[0].image.href).to.eql('https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-zoom.png')
    })
  })
})
