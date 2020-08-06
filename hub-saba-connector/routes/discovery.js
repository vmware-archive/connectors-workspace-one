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
      href: 'https://s3.amazonaws.com/vmw-mf-assets/connector-images/hub-saba.png'
    },
    object_types: {
      card: {
        pollable: true,
        endpoint: {
          href: `${baseUrl}/api/cards/requests`
        }
      }
    }
  }
  return res.json(discovery)
}
