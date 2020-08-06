/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const mfCommons = require('@vmw/mobile-flows-connector-commons')

const userService = require('../services/user-services')
const learningServices = require('../services/learning-services')
const cardObjects = require('../utils/card-objects')
const backendAuth = require('../utils/backend-auth')

const handleCardRequest = async (req, res) => {
  let employeeResponse
  try {
    employeeResponse = await userService.retrieveEmployeeId(res)
  } catch (e) {
    return await handleSabaError(res, e, 'Failed to retrieve employee id')
  }

  if (employeeResponse.results.length !== 1) {
    // User with the email id might not exist or there are more than 1 user for same email id.
    mfCommons.logReq(res, 'Could not identify a unique user in the Saba system. Found %d users', employeeResponse.results.length)
    return res.status(200).json({ objects: [] })
  }

  const employeeId = employeeResponse.results[0].id
  mfCommons.logReq(res, 'Retrieve learning for employee: ' + employeeId)

  const learningCards = []
  try {
    const [curriculum, certification, enrollments] = await Promise.all([
      getCurriculum(res, employeeId),
      getCertification(res, employeeId),
      retrieveCourseEnrollments(res, employeeId)
    ])

    // Filter out to include only those we need for making cards.
    const independentEnrollments = filterEnrollments(enrollments, curriculum.learningParts, certification.learningParts)

    mfCommons.logReq(res, 'Number of independent courses to produce cards: %d', independentEnrollments.length)
    const courseEnrollmentCards = await cardObjects.enrollmentsToCards(res, independentEnrollments)

    learningCards.push(...curriculum.cards)
    learningCards.push(...certification.cards)
    learningCards.push(...courseEnrollmentCards)
  } catch (e) {
    return await handleSabaError(res, e, 'Failed to make cards')
  }

  return res.status(200).json({
    objects: learningCards
  })
}

const getCurriculum = async (res, employeeId) => {
  let curriculumList
  try {
    curriculumList = await learningServices.retrieveCurriculum(res, employeeId)
      .then(response => response.results)
  } catch (e) {
    mfCommons.logReq(res, 'Failed to retrieve curriculum, %s', e.message)
    throw e
  }

  mfCommons.logReq(res, 'Number of Curriculum retrieved: %d', curriculumList.length)
  return await cardObjects.curriculumsToCards(res, curriculumList)
}

const getCertification = async (res, employeeId) => {
  let certificationList
  try {
    certificationList = await learningServices.retrieveCertifications(res, employeeId)
      .then(responseJson => responseJson.results)
  } catch (e) {
    mfCommons.logReq(res, 'Failed to retrieve certifications, %s', e.message)
    throw e
  }

  mfCommons.logReq(res, 'Number of Certifications retrieved: %d', certificationList.length)
  return await cardObjects.certificationsToCards(res, certificationList)
}

const retrieveCourseEnrollments = async (res, employeeId) => {
  let enrollmentsList
  try {
    enrollmentsList = await learningServices.retrieveEnrollments(res, employeeId)
      .then(responseJson => responseJson.results)
  } catch (e) {
    mfCommons.logReq(res, 'Failed to retrieve enrollments, %s', e.message)
    throw e
  }

  mfCommons.logReq(res, 'Number of Enrollments retrieved: %d', enrollmentsList.length)
  return enrollmentsList
}

/*
 * Enrollment list retrieved from Saba API includes all courses. We don't need to produce a card
 * if we have already produced one for the related Curriculum or Accreditation. This method would filter out such courses
 * (Notice that people can take courses without actually enrolling to an entire Curriculum for example.)
 *
 * @param  enrollmentsList - List of enrollments obtained from Saba API.
 * @param  curriculumLearningParts - List of course-ids related to all the user-enrolled Curriculum
 * @return  certificationLearningParts - List of course-ids related to all the user-enrolled Certification
 */
const filterEnrollments = (enrollmentsList, curriculumLearningParts, certificationLearningParts) => {
  const learningParts = [...curriculumLearningParts, ...certificationLearningParts]
  return enrollmentsList.filter(
    e => !learningParts.includes(e.offering_temp_id.id)
  )
}

const handleSabaError = async (res, error, message = 'Saba API has failed.') => {
  mfCommons.logReq(res, message)

  let responseBody
  if (error.status) {
    responseBody = await error.response.text()
    mfCommons.logReq(res, error.message + ', ' + responseBody)
  }

  if (error.status === 500 && isJson(responseBody) && JSON.parse(responseBody).errorCode === 123) {
    // It means unauthorized. Maybe Saba certificate is expired or revoked.
    backendAuth.clearTokens(res)
    return res
      .status(400)
      .header('X-Backend-Status', 401)
      .json({ error: error.message })
  } else {
    return res
      .status(500)
      .header('X-Backend-Status', error.status)
      .json({ error: error.message })
  }
}

const isJson = (str) => {
  try {
    JSON.parse(str)
  } catch (e) {
    return false
  }
  return true
}

module.exports = {
  handleCardRequest
}
