/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { describe } = require('mocha')
const { expect } = require('chai')
const chai = require('chai')
const { discoveryController } = require('../../routes/discovery-controller')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('dicovery-controller', () => {
  describe('discoveryController', () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
      }
    }
    let discoveryJson = ''
    const mockResp = {
      json: (jsonIn) => {
        discoveryJson = jsonIn
      }
    }
    it('Check discovery response', async () => {
      discoveryController(mockReq, mockResp)
      expect(typeof discoveryJson).to.equal('object')
      expect(discoveryJson.image.href).to.equal('https://my-host:3030/my-path-prefix/images/connector.png')
      expect(discoveryJson.object_types.card.doc.href).to.equal('https://github.com/vmware-samples/card-connectors-guide/wiki/Card-Responses')
      expect(discoveryJson.object_types.card.endpoint.href).to.equal('https://my-host:3030/my-path-prefix/cards')
    })
  })
})
