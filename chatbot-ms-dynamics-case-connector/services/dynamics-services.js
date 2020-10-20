/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const axios = require('axios')
const errorObj = (err) => {
  return new Error(JSON.stringify({
    statusCode: err.response.status,
    message: err.response.data
  }))
}

const getUserInfo = async (res) => {
  const options = {
    headers: {
      Authorization: res.locals.backendAuthorization,
      accept: 'application/json'
    }
  }
  const resp = await axios
    .get(`${res.locals.backendBaseUrl}/api/data/v9.0/WhoAmI`, options)
    .then(r => r.data)
    .catch(err => {
      throw errorObj(err)
    })
  return resp || ''
}

const getActiveCases = async (res, userid) => {
  const options = {
    headers: {
      Authorization: res.locals.backendAuthorization,
      accept: 'application/json',
      Prefer: 'odata.include-annotations=*'
    }
  }
  const resp = await axios
    .get(`${res.locals.backendBaseUrl}/api/data/v9.0/incidents?$filter= _ownerid_value eq ${userid} and statecode eq 0`, options)
    .then(r => r.data)
    .catch(err => {
      throw errorObj(err)
    })
  return resp.value || ''
}

module.exports = {
  getUserInfo,
  getActiveCases

}
