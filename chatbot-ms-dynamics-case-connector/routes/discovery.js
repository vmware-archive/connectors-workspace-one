/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'
const mfCommons = require('@vmw/mobile-flows-connector-commons')


exports.root = function (req, res) {
  const baseUrl = mfCommons.getConnectorBaseUrl(req)
  const discovery = {
    image: {
      href: 'https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-ms-dynamics.png'
    },
    object_types: {
      botDiscovery: {
        endpoint: {
          href: `${baseUrl}/bot/discovery`
        }
      }
    }
  }
  return res.json(discovery)
}
