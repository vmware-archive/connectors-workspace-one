/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const DEFAULT_PORT = 3003

const connectorLogoURL = (baseUrl) => {
  return "https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-salesforce.png"
}

module.exports = {
  DEFAULT_PORT,
  connectorLogoURL
}
