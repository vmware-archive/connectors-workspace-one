/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'
const utils = require('../utils/utility')

exports.root = async (req, res) => {
  const baseUrl = utils.urlPrefix(req)
  const discovery = {
    image: {
      href: utils.LINKEDIN_LOGO_PATH
    },
    object_types: {
      card: {
        pollable: true,
        endpoint: {
          href: `${baseUrl}/api/cards`
        }
      }
    }
  }
  return res.json(discovery)
}
