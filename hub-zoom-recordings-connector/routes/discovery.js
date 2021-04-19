/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const mfCommons = require('@vmw/mobile-flows-connector-commons')

const root = (req, res) => {
  const baseUrl = mfCommons.getConnectorBaseUrl(req)
  const discovery = {
    image: {
      href: 'https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-zoom.png'
    },
    object_types: {
      card: {
        pollable: true,
        endpoint: {
          href: `${baseUrl}/api/cards`
        },
        doc: {
          href: 'https://vmwaresamples.github.io/card-connectors-guide/#schema/herocard-response-schema.json'
        }
      }
    }
  }

  return res.json(discovery)
}

module.exports = {
  root
}
