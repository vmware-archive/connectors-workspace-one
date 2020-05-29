'use strict'

const rp = require('request-promise-native')
const { expect } = require('chai')
const testUtils = require('./test-utils')

const baseUrl = 'http://localhost:3000'
const mockMfServer = require('./mock-mf-server')
const mockZoomServer = require('./mock-zoom-server')

function callHealth () {
    const options = {
        method: 'GET',
        uri: `${baseUrl}/health`,
        resolveWithFullResponse: true,
        json: true
    }
    return rp(options)
}

const callDiscovery = async () => {
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

const zoomRecordings = (mfAuth, connectorAuth, connectorBaseUrl) => {
    const options = {
      method: 'POST',
      uri: `${testUtils.CONNECTOR_URL}/api/cards`,
      resolveWithFullResponse: true,
      json: true,
      qs: {},
      headers: {
        Authorization: `Bearer ${mfAuth}`,
        'X-Connector-Base-Url': connectorBaseUrl,
        'X-Connector-Authorization': connectorAuth,
        'Content-Type': 'application/x-www-form-urlencoded',
        'x-request-id': 'username'
      }
    }
    return rp(options)
}

describe('Hub Zoom recording connector tests', () => {
    let server

    before(() => {
        if (!server) {
            mockMfServer.start()
            mockZoomServer.start();

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
                mockMfServer.stop(() => {
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
            const resp = await callHealth()
            expect(resp.statusCode).to.eql(200)
            expect(resp.body.status).to.eql('UP')
        })
    })

    describe('Discovery', () => {
        it('should return correct metadata', async () => {
            const discovery = require('./discovery')
            const resp = await callDiscovery()
            expect(resp.statusCode).to.eql(200)
            expect(resp.body).to.eql(discovery)
        })
    })

    describe('Zoom recordings', () => {

        it('should return 401 if authorization header is missing', async () => {
            const token = mockMfServer.getMfTokenFor('https://my-host:3030/abc/api/cards')
            const baseUrl = testUtils.BACKEND_BASE_URL
            let error
            try {
                await zoomRecordings('', token, baseUrl)
            } catch(err) {
                error = err
            }
            expect(error.statusCode).to.eql(401)
            expect(error.error.message).to.contain('Missing authorization header')
        })

        it('should return 401 if authorization verification failed', async () => {
            const token = mockMfServer.getMfTokenFor('https://my-host:3030/abc/api/cards')
            const baseUrl = testUtils.BACKEND_BASE_URL
            const invalidAuth = 'some-invalid-auth'
            let error
            try {
                await zoomRecordings(invalidAuth, token, baseUrl)
            } catch(err) {
                error = err
            }
            expect(error.statusCode).to.eql(401)
            expect(error.error.message).to.contain('Identity verification failed!')
        })

        it('should return 400 if x-connector-base-url value is missing', async () => {
            const token = mockMfServer.getMfTokenFor('https://my-host:3030/abc/api/cards')
            let error
            try {
                await zoomRecordings(token, token, '')
            } catch(err) {
                error = err
            }
            expect(error.statusCode).to.eql(400)
            expect(error.error.message).to.contain('The x-connector-base-url is required')
        })

        it('should return 400 if x-connector-authorization value is missing', async () => {
            const token = mockMfServer.getMfTokenFor('https://my-host:3030/abc/api/cards')
            const baseUrl = testUtils.BACKEND_BASE_URL
            let error
            try {
                await zoomRecordings(token, '', baseUrl)
            } catch(err) {
                error = err
            }
            expect(error.statusCode).to.eql(400)
            expect(error.error.message).to.contain('The x-connector-authorization is required')
        })

        it('should return new recordings', async () => {
            const token = mockMfServer.getMfTokenFor('https://my-host:3030/abc/api/cards')
            const baseUrl = testUtils.BACKEND_BASE_URL
            const recordings = require('./zoom-recordings')
            const resp = await zoomRecordings(token, token, baseUrl)
            expect(resp.statusCode).to.eql(200)
            expect(resp.body.objects.length).to.eql(recordings.meetings.length)
            expect(resp.body.objects[0].backend_id).to.eql('4752962893')
            expect(resp.body.objects[0].header.title).to.eql('Cloud Recording Now Available')
            expect(resp.body.objects[0].image.href).to.eql('https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-zoom.png')
        })

    })


})
