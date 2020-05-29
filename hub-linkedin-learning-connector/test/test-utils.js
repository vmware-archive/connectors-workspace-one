'use strict'

const connectorUrl = 'http://localhost:3000'
const mockMfUrl = 'http://localhost:5000'
const mockBackendBaseUrl = 'http://localhost:4000'
const mockLinkedInTokenEndPoint = 'http://localhost:4000/oauth/v2/accessToken'
const connectorAuthorization = 'someRandomValue'

const mockMfPublicKeyUrl = `${mockMfUrl}/public-key`
const mockMfXRoutingPrefix = `${mockMfUrl}/connectors/123/botDiscovery/`
const mockMfXRoutingTemplate = `${mockMfUrl}/connectors/123/INSERT_OBJECT_TYPE/`

const addXForwardedHeaders = (rpOptions) => {
  rpOptions.headers['X-Forwarded-Proto'] = 'https'
  rpOptions.headers['X-Forwarded-Prefix'] = '/abc'
  rpOptions.headers['X-Forwarded-Host'] = 'my-host'
  rpOptions.headers['X-Forwarded-Port'] = '3030'
}

module.exports = Object.freeze({
  CONNECTOR_URL: connectorUrl,
  MF_JWT_PUB_KEY_URI: mockMfPublicKeyUrl,
  BACKEND_BASE_URL: mockBackendBaseUrl,
  BACKEND_TOKEN_ENDPOINT_URL: mockLinkedInTokenEndPoint,
  MF_X_ROUTING_PREFIX: mockMfXRoutingPrefix,
  MF_X_ROUTING_TEMPLATE: mockMfXRoutingTemplate,
  addXForwardedHeaders: addXForwardedHeaders,
  BACKEND_ACCESS_TOKEN: connectorAuthorization
})
