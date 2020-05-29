
'use strict'

const utility = require('../utils/utility');

const root = (req, res) => {
  const baseUrl = utility.derivedBaseUrl(req)
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
