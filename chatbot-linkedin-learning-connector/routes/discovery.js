/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

exports.root = function (req, res) {
  const baseUrl = urlPrefix(req)
  const discovery = {
    image: {
      href: 'https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-linkedin-learning.png'
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

function urlPrefix (req) {
  const proto = req.header('x-forwarded-proto') || 'http'
  const host = req.header('x-forwarded-host')
  const port = req.header('x-forwarded-port')
  const path = req.header('x-forwarded-prefix') || ''

  if (host && port) {
    return `${proto}://${host}:${port}${path}`
  }

  if (host && !port) {
    return `${proto}://${host}${path}`
  }

  return `${proto}://${req.headers.host}${path}`
}
