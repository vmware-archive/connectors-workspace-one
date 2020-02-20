/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Discovery is the process by which a connector explains its capabilties to the Mobile Flows server
 * The request is an unauthenticated GET and the response is JSON
 */
const urljoin = require('url-join')

/**
 * Express looks for X-Forwarded-Proto
 * @param  {} req express request
 */
const protocol = (req) => {
  return req.protocol
}

/**
 * request.hostname is broken in Express 4 so we have to deal with the X-Forwarded- headers ourselves
 * @param  {} req express request
 */
const derivedBaseUrl = (req) => {
  const host = req.headers['x-forwarded-host'] || req.headers.host
  const proto = req.headers['x-forwarded-proto'] || protocol(req)
  const forwardedPort = req.headers['x-forwarded-port']
  const forwardedPrefix = req.headers['x-forwarded-prefix'] || ''

  if (forwardedPort && forwardedPrefix) {
    return `${proto}://${host}:${forwardedPort}${forwardedPrefix}`
  } else {
    return `${proto}://${host}`
  }
}

const imageURL = (req) => {
  return 'https://s3.amazonaws.com/vmw-mf-assets/connector-images/jira-service-desk.png'
}
/**
 * Combine the various path components to make the URL route to the action
 * @param  {} req
 * @param  {} virtualURL
 */
const prepareURL = (req, virtualURL) => {
  const routingPrefix = req.headers['x-routing-prefix'] || ''
  const forwardedPrefix = req.headers['x-forwarded-prefix'] || ''
  return urljoin(routingPrefix, forwardedPrefix, virtualURL)
}

/**
 * Return the discovery JSON response that describes the capabilities of this connector
 * @param  {} req express request
 * @param  {} res express response
 */
const discovery = (req, res) => {
  const baseURL = derivedBaseUrl(req)

  const discoveryJSON = {
    image: {
      href: `${imageURL(req)}`
    },
    object_types: {
      card: {
        pollable: true,
        doc: {
          href: 'https://vmwaresamples.github.io/card-connectors-guide/#schema/herocard-response-schema.json'
        },
        endpoint: {
          href: `${baseURL}/cards`
        }
      }
    }
  }
  if (process.env.DEBUG) {
    console.log('discovery requested')
  }
  res.json(discoveryJSON)
}

module.exports = {
  discovery,
  imageURL,
  prepareURL
}
