/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const DEFAULT_PORT = 3000

const connectorLogoURL = (baseUrl) => {
  return `${baseUrl}/images/connector.png`
}

module.exports = {
  DEFAULT_PORT,
  connectorLogoURL
}
