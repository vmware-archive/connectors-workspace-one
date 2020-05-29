
'use strict'

const moment = require('moment')
const rp = require('request-promise-native')

const getMyRecordings = async (res) => {
  const lastWeek = moment().subtract(7, 'days').format('YYYY-MM-DD')
  const today = moment().format('YYYY-MM-DD')
  const options = {
    uri: `${res.locals.baseUrl}/users/me/recordings?from=${lastWeek}&to=${today}`,
    method: 'GET',
    json: true,
    headers: {
      Authorization: res.locals.connectorAuthorization
    }
  }
  return rp(options).then(result => { return result.meetings })
}

module.exports = {
  getMyRecordings
}
