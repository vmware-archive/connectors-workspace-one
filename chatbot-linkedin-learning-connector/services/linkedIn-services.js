/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'
const rp = require('request-promise')

/**
 * Fetch the top pick courses
 * @param res - Response object
 */
const getUserTopPicks = async (res) => {
  const options = {
    uri: getLinkedinURI(res.locals.backendBaseUrl),
    method: 'GET',
    json: true,
    qs: {
      q: 'criteria',
      'assetFilteringCriteria.assetTypes[0]': 'COURSE',
      'assetPresentationCriteria.sortBy': 'RELEVANCE',
      count: 3,
      'assetFilteringCriteria.locales[1].language': 'en', // need to update code when backend support localization
      'assetFilteringCriteria.locales[1].country': 'US'
    },
    headers: {
      authorization: res.locals.backendAuthorization,
      'x-request-id': res.locals.xRequestId
    }
  }

  return rp(options)
    .then(courses => courses.elements)
}

/**
 * Fetch new course from linkedin learning
 * @param res - Response object
 */
const getNewCourses = async (res) => {
  const options = {
    uri: getLinkedinURI(res.locals.backendBaseUrl),
    method: 'GET',
    json: true,
    qs: {
      q: 'criteria',
      'assetFilteringCriteria.assetTypes[0]': 'COURSE',
      'assetPresentationCriteria.sortBy': 'RECENCY',
      'assetFilteringCriteria.lastModifiedAfter': getPreviousMonthDateInMilliSeconds(),
      count: 3,
      'assetFilteringCriteria.locales[1].language': 'en', // need to update code when backend support localization
      'assetFilteringCriteria.locales[1].country': 'US'
    },
    headers: {
      authorization: res.locals.backendAuthorization,
      'x-request-id': res.locals.xRequestId
    }

  }
  return rp(options)
    .then(courses => courses.elements)
}

/**
 * Fetch courses by given keyword search
 * @param res - Response object
 * @param keyword - get courses by specific keyword
 */
const geKeywordSearch = async (res, keyword) => {
  const options = {
    uri: getLinkedinURI(res.locals.backendBaseUrl),
    method: 'GET',
    json: true,
    qs: {
      q: 'criteria',
      'assetFilteringCriteria.assetTypes[0]': 'COURSE',
      'assetFilteringCriteria.keyword': keyword,
      count: 3,
      'assetFilteringCriteria.locales[1].language': 'en', // need to update code when backend support localization
      'assetFilteringCriteria.locales[1].country': 'US'
    },
    headers: {
      authorization: res.locals.backendAuthorization,
      'x-request-id': res.locals.xRequestId
    }
  }
  return rp(options)
    .then(courses => courses.elements)
}

/**
 * Helper method to get last month date in milliseconds
 */
const getPreviousMonthDateInMilliSeconds = () => {
  const date = new Date()
  date.setMonth(date.getMonth() - 1)
  return date.getTime()
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
  getUserTopPicks,
  getNewCourses,
  geKeywordSearch
}
