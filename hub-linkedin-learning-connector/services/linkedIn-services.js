/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'
const rp = require('request-promise')

/**
 * Fetch new course from linkedin learning
 * @param res - Response object
 */
const getNewCourses = async (res) => {
  const previousDate = new Date()
  previousDate.setDate(previousDate.getDate() - 3) // need to decide how many previous date we want
  const options = {
    uri: getLinkedinURI(res.locals.baseUrl),
    method: 'GET',
    json: true,
    qs: {
      q: 'criteria',
      'assetFilteringCriteria.assetTypes[0]': 'COURSE',
      'assetPresentationCriteria.sortBy': 'RELEVANCE',
      'assetFilteringCriteria.lastModifiedAfter': previousDate.getTime(),
      count: 1,
      'assetFilteringCriteria.locales[1].language': 'en', // need to update code when backend support localization
      'assetFilteringCriteria.locales[1].country': 'US'
    },
    headers: {
      authorization: res.locals.connectorAuthorization,
      'x-request-id': res.locals.xReqId
    }
  }
  return rp(options)
    .then(courses => courses.elements)
}

/**
 * Helper method to get linkedin API URI
 */
const getLinkedinURI = (baseUrl) => {
  if (baseUrl.slice(-1) === '/') {
    return `${baseUrl}learningAssets`
  }
  return `${baseUrl}/learningAssets`
}

module.exports = {
  getNewCourses
}
