/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const { v4: uuidv4 } = require('uuid')
const sha1 = require('sha1')
const learningServices = require('../services/learning-services')
const mfCommons = require('@vmw/mobile-flows-connector-commons')

const curriculumsToCards = async (res, curriculumList) => {
  const learningParts = []
  const cards = await Promise.all(curriculumList.map(curriculum => curriculumToCards(res, curriculum, learningParts)))
  return {
    cards: cards,
    learningParts: learningParts
  }
}

const curriculumToCards = async (res, curriculum, learningParts) => {
  const id = curriculum.basicdetail.curriculum.id
  let learningModule
  try {
    learningModule = await learningServices.retrieveModuleDetails(res, id)
  } catch (e) {
    mfCommons.logReq(res, 'Could not get details of learning module: %s, %s', id, e.message)
    throw e
  }

  rememberLearningsParts(learningModule, learningParts)

  const curriculumName = curriculum.basicdetail.curriculum.displayName
  const progress = curriculum.basicdetail.status.displayName
  const dueDate = curriculum.basicdetail.targetDate
  const deepLinkUrl = learningModule.deepLinkUrls[0]

  const hash = sha1(id + curriculumName + progress + dueDate + deepLinkUrl)

  const card = {
    id: uuidv4(),
    backend_id: id,
    hash: hash,
    header: {
      title: 'Saba - New learning available'
    },
    image: {
      href: 'https://s3.amazonaws.com/vmw-mf-assets/connector-images/hub-saba.png'
    },
    body: {
      fields: [
        {
          type: 'GENERAL',
          title: 'Curriculum Name',
          description: curriculumName
        },
        {
          type: 'GENERAL',
          title: 'Progress',
          description: progress
        },
        {
          type: 'GENERAL',
          title: 'Due date',
          description: getDisplayDueDate(dueDate)
        }
      ]
    },
    actions: [
      {
        id: uuidv4(),
        action_key: 'OPEN_IN',
        label: 'Open learning',
        completed_label: 'Open learning',
        type: 'GET',
        primary: true,
        remove_card_on_completion: false,
        allow_repeated: true,
        url: {
          href: deepLinkUrl
        }
      }
    ]
  }

  addPathOptionsField(card, learningModule)

  // If there is exactly 1 path then we will also show module details. Each module in the path is a section in the card.
  // This is done to prevent overloading the card with too much of information.
  if (learningModule.paths && learningModule.paths.length === 1) {
    card.body.fields.push(...getSections(learningModule.paths[0]))
  }

  return card
}

/*
 * This function can be used to build a union of all courses related to Curriculum or Certifications.
 * Later while making cards for Courses it helps to remove duplicates.
 */
const rememberLearningsParts = (learningModule, partsState) => {
  if (learningModule.paths) {
    learningModule.paths.forEach(path => {
      if (path.learningModules) {
        path.learningModules.forEach(module => {
          if (module.learningInterventions) {
            module.learningInterventions.forEach(intervention => {
              partsState.push(intervention.part_id.id)
            })
          }
        })
      }
    })
  }
}

const addPathOptionsField = (card, learningModule) => {
  if (learningModule.paths) {
    const pathOptions = learningModule.paths.map(p => p.name).join(', ')
    card.body.fields.push({
      type: 'GENERAL',
      title: 'Path Options',
      description: pathOptions
    })
  }
}

const getSections = (singlePath) => {
  const sectionFields = []
  if (singlePath.learningModules) {
    singlePath.learningModules.forEach(pathModule => {
      const moduleSection = getModuleSection(pathModule)
      if (moduleSection) {
        sectionFields.push(moduleSection)
      }
    })
  }
  return sectionFields
}

const getModuleSection = (pathModule) => {
  const items = []
  if (pathModule.learningInterventions) {
    pathModule.learningInterventions.forEach(learningIntervention => {
      const item = {
        type: 'GENERAL',
        title: 'Course',
        description: learningIntervention.part_id.displayName
      }
      items.push(item)
    })
  }

  if (items.length > 0) {
    return {
      type: 'SECTION',
      title: pathModule.name,
      items: items
    }
  } else {
    return null
  }
}

const certificationsToCards = async (res, certificationList) => {
  const learningParts = []
  const cards = await Promise.all(certificationList.map(certification => singleCertificationCard(res, certification, learningParts)))
  return {
    cards: cards,
    learningParts: learningParts
  }
}

const singleCertificationCard = async (res, certification, learningParts) => {
  const id = certification.basicdetail.certification_id.id

  let learningModule
  try {
    learningModule = await learningServices.retrieveModuleDetails(res, id)
  } catch (e) {
    mfCommons.logReq(res, 'Could not get details of learning module: %s, %s', id, e.message)
    throw e
  }

  rememberLearningsParts(learningModule, learningParts)

  const certificationName = certification.basicdetail.certification_id.displayName
  const progress = certification.basicdetail.status.displayName
  const dueDate = certification.basicdetail.targetDate
  const deepLinkUrl = learningModule.deepLinkUrls[0]

  const hash = sha1(id + certificationName + progress + dueDate + deepLinkUrl)

  const card = {
    id: uuidv4(),
    backend_id: id,
    hash: hash,
    header: {
      title: 'Saba - New learning available'
    },
    image: {
      href: 'https://s3.amazonaws.com/vmw-mf-assets/connector-images/hub-saba.png'
    },
    body: {
      fields: [
        {
          type: 'GENERAL',
          title: 'Accreditation Name',
          description: certificationName
        },
        {
          type: 'GENERAL',
          title: 'Progress',
          description: progress
        },
        {
          type: 'GENERAL',
          title: 'Due date',
          description: getDisplayDueDate(dueDate)
        }
      ]
    },
    actions: [
      {
        id: uuidv4(),
        action_key: 'OPEN_IN',
        label: 'Open learning',
        completed_label: 'Open learning',
        type: 'GET',
        primary: true,
        remove_card_on_completion: false,
        allow_repeated: true,
        url: {
          href: deepLinkUrl
        }
      }
    ]
  }

  addPathOptionsField(card, learningModule)

  // If there is exactly 1 path then we will also show module details. Each module in the path is a section in the card.
  // This is done to prevent overloading the card with too much of information.
  if (learningModule.paths && learningModule.paths.length === 1) {
    card.body.fields.push(...getSections(learningModule.paths[0]))
  }

  return card
}

const enrollmentsToCards = async (res, enrollmentsList) => {
  return Promise.all(enrollmentsList.map(enrollment => singleEnrollmentCard(res, enrollment)))
}

const singleEnrollmentCard = async (res, enrollment) => {
  const regId = enrollment.id

  let learningModule
  try {
    learningModule = await learningServices.retrieveModuleDetails(res, enrollment.offering_temp_id.id)
  } catch (e) {
    mfCommons.logReq(res, 'Could not get details of learning module: %s, %s, %s',
      JSON.stringify(enrollment.offering_temp_id),
      regId,
      e.message)
    throw e
  }

  let enrollmentDetails
  try {
    enrollmentDetails = await learningServices.retrieveEnrollmentDetails(res, enrollment.id)
  } catch (e) {
    mfCommons.logReq(res, 'Could not get details of enrollment: %s, %s, %s',
      JSON.stringify(enrollment.offering_temp_id),
      regId,
      e.message
    )
    throw e
  }

  const courseName = enrollment.class_id.displayName
  const progress = enrollmentDetails.registrationInfo.statusDescription
  const dueDate = enrollmentDetails.learningEventDetail.dueDate
  const deepLinkUrl = learningModule.deepLinkUrls[0]

  const hash = sha1(regId + courseName + progress + dueDate + deepLinkUrl)

  return {
    id: uuidv4(),
    backend_id: regId,
    hash: hash,
    header: {
      title: 'Saba - New learning available'
    },
    image: {
      href: 'https://s3.amazonaws.com/vmw-mf-assets/connector-images/hub-saba.png'
    },
    body: {
      fields: [
        {
          type: 'GENERAL',
          title: 'Course Name',
          description: courseName
        },
        {
          type: 'GENERAL',
          title: 'Progress',
          description: progress
        },
        {
          type: 'GENERAL',
          title: 'Due date',
          description: getDisplayDueDate(dueDate)
        }
      ]
    },
    actions: [
      {
        id: uuidv4(),
        action_key: 'OPEN_IN',
        label: 'Open learning',
        completed_label: 'Open learning',
        type: 'GET',
        primary: true,
        remove_card_on_completion: false,
        allow_repeated: true,
        url: {
          href: deepLinkUrl
        }
      }
    ]
  }
}

const getDisplayDueDate = (dueDate) => {
  if (dueDate) {
    // will this have timezone issues ?
    return new Date(dueDate).toISOString().substring(0, 10)
  } else {
    return '---'
  }
}

module.exports = {
  curriculumsToCards,
  certificationsToCards,
  enrollmentsToCards
}
