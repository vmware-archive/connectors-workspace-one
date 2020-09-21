/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { connectorLogoURL } = require('../config/config')
const mfCommons = require('@vmw/mobile-flows-connector-commons')

/**
 * Return the discovery JSON response that describes the capabilities of this connector
 * @param  {*} req express request
 * @param  {*} res express response
 */
const discoveryController = (req, res) => {
  const baseURL = mfCommons.getConnectorBaseUrl(req)
  const discoveryJSON = {
    image: {
      href: `${connectorLogoURL()}`
    },
    object_types: {
      card: {
        pollable: true,
        doc: {
          href: 'https://github.com/vmware-samples/card-connectors-guide/wiki/Card-Responses'
        },
        endpoint: {
          href: `${baseURL}/cards`
        }
      }
    }
  }
  mfCommons.log(discoveryJSON)
  res.json(discoveryJSON)
}

module.exports = {
  discoveryController
}
