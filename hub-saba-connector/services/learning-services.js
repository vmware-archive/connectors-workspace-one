/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const fetch = require('node-fetch')
const utils = require('../utils/utils')

const retrieveCurriculum = async (res, employeeId) => {
  const options = {
    headers: {
      SabaCertificate: res.locals.sabaCertificate
    }
  }
  const queryQ = encodeURIComponent(`(assignee==${employeeId},status_description_curr!=100)`)
  return fetch(`${res.locals.backendBaseUrl}/v1/learning/heldlearningevent?type=curriculum&q=${queryQ}&includeDetails=true&count=10&startPage=0`, options)
    .then(utils.withExceptionForHttpError)
    .then(response => response.json())
}

const retrieveCertifications = async (res, employeeId) => {
  const options = {
    headers: {
      SabaCertificate: res.locals.sabaCertificate
    }
  }
  const queryQ = encodeURIComponent(`(assignee==${employeeId},status_description_cert!=100)`)
  return fetch(`${res.locals.backendBaseUrl}/v1/learning/heldlearningevent?type=certification&q=${queryQ}&includeDetails=true&count=10&startPage=0`, options)
    .then(utils.withExceptionForHttpError)
    .then(response => response.json())
}

const retrieveEnrollments = async (res, employeeId) => {
  const options = {
    headers: {
      SabaCertificate: res.locals.sabaCertificate
    }
  }
  return fetch(`${res.locals.backendBaseUrl}/v1/people/${employeeId}/enrollments/search?type=internal&includeDetails=TRUE&count=50`, options)
    .then(utils.withExceptionForHttpError)
    .then(response => response.json())
}

/*
 * Gives all details of a learning module
 * learningModuleId - The id of a single Curriculum or a certification.
 */
const retrieveModuleDetails = async (res, learningModuleId) => {
  const options = {
    headers: {
      SabaCertificate: res.locals.sabaCertificate
    }
  }
  return fetch(`${res.locals.backendBaseUrl}/v1/learningmodule/${learningModuleId}`, options)
    .then(utils.withExceptionForHttpError)
    .then(response => response.json())
}

const retrieveEnrollmentDetails = async (res, enrollmentId) => {
  const options = {
    headers: {
      SabaCertificate: res.locals.sabaCertificate
    }
  }
  return fetch(`${res.locals.backendBaseUrl}/v1/enrollments/${enrollmentId}/sections:regdetail`, options)
    .then(utils.withExceptionForHttpError)
    .then(response => response.json())
}

module.exports = {
  retrieveCurriculum,
  retrieveCertifications,
  retrieveEnrollments,
  retrieveModuleDetails,
  retrieveEnrollmentDetails
}
