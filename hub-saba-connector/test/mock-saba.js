/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'
/*
 * **Normal working**
 * case-1: harshas@vmware.com
 * Has all types of learning assigned.
 *
 * case-2: sumit@vmware.com
 * Has no learnings at all
 *
 * case-3: fisher@vmware.com
 * Has neither Curriculum nor Accreditation. But there is one course assigned.
 *
 * case-4: jbard@vmware.com
 * Has 1 Curriculum and 1 course assigned. (The course isn't related to the Curriculum and hence should get 2 cards.)
 *
 * **Revoked certificate**
 * Coincidently when user1 tries card request the Saba certificate was revoked. - user1@vmware.com
 *
 * **Saba internal server error**
 * For one of the users Saba server has a bug in its logic - user2@vmware.com
 */
const mfCommons = require('@vmw/mobile-flows-connector-commons')

let fakeBackend

const SERVICE_USER = 'user1'
const SERVICE_PASSWORD = 'password1'
const SABA_CERTIFICATE = 'ABCD-good-XYZ'

const startFakeBackend = () => {
  const express = require('express')
  const app = express()
  app.use(express.json())

  app.use('/v1/*', (req, res, next) => {
    if (req.originalUrl === '/v1/login') {
      return next()
    }
    const sabaCertificate = req.header('sabacertificate')
    if (sabaCertificate !== SABA_CERTIFICATE) {
      mfCommons.log('Invalid or expired Saba certificate.')
      return res.status(500).json({
        errorCode: 123,
        errorMessage: '(123) Invalid or expired Certificate'
      })
    }

    next()
  })

  app.get('/v1/login', (req, res) => {
    const username = req.headers.user
    const password = req.headers.password

    if (username !== SERVICE_USER || password !== SERVICE_PASSWORD) {
      mfCommons.log('wrong service user credentials.')
      return res.status(500).json({
        errorCode: -4,
        errorMessage: 'The username or password that you entered is incorrect.'
      })
    }

    return res.json({
      '@type': 'SabaCertificate',
      certificate: SABA_CERTIFICATE
    })
  })

  app.get('/v1/people', (req, res) => {
    console.log('see query: ' + req.query.q)
    const userEmail = readFromQueryQ(req.query.q, 'username==')
    mfCommons.log('Get employee id for: %s', userEmail)

    if (req.query.type !== 'internal') {
      return res.status(500).json({
        errorMessage: 'Cannot find component'
      })
    }

    let responseStatus
    let responseJson
    switch (userEmail) {
      case 'user1@vmware.com':
        // certificate is revoked when this user tries card request.
        responseStatus = 500
        responseJson = {
          errorCode: 123,
          errorMessage: '(123) Invalid or expired Certificate'
        }
        break
      case 'user2@vmware.com':
        // certificate is revoked when this user tries card request.
        responseStatus = 500
        responseJson = {
          errorMessage: 'something went wrong while processing'
        }
        break
      case 'harshas@vmware.com':
        responseStatus = 200
        responseJson = require('./saba/response/employee-id.json')
        break
      case 'sumit@vmware.com':
        responseStatus = 200
        responseJson = require('./saba/response/employee-id-sumit.json')
        break
      case 'fisher@vmware.com':
        responseStatus = 200
        responseJson = require('./saba/response/employee-id-fisher.json')
        break
      case 'ryan@vmware.com':
        responseStatus = 200
        responseJson = require('./saba/response/employee-id-ryan.json')
        break
      default:
        responseStatus = 200
        responseJson = require('./saba/response/employee-id-bogus.json')
    }

    return res.status(responseStatus).json(responseJson)
  })

  app.get('/v1/people/:employeeId/enrollments/search', (req, res) => {
    const employeeId = req.params.employeeId

    let responseJson
    switch (employeeId) {
      case 'emplo000000000016551':
        responseJson = require('./saba/response/enrollments.json')
        break
      case 'emplo000000000016550':
        responseJson = require('./saba/response/enrollments-sumit.json')
        break
      case 'emplo000000000016552':
        responseJson = require('./saba/response/enrollments-fisher.json')
        break
      case 'emplo000000000415361':
        responseJson = require('./saba/response/enrollments-ryan.json')
        break
      default:
        throw new Error('Unexpected employee id.')
    }
    return res.json(responseJson)
  })

  app.get('/v1/learning/heldlearningevent', (req, res) => {
    const queryType = req.query.type
    const employeeId = readFromQueryQ(req.query.q, 'assignee==')
    mfCommons.log('Return held heldlearningevent for %s, %s', employeeId, queryType)

    let heldLearningJson
    switch (employeeId) {
      case 'emplo000000000016551':
        if (queryType === 'curriculum') {
          heldLearningJson = require('./saba/response/held-curriculum.json')
        } else if (queryType === 'certification') {
          heldLearningJson = require('./saba/response/held-certifications.json')
        }
        break
      case 'emplo000000000016550':
        if (queryType === 'curriculum') {
          heldLearningJson = require('./saba/response/held-curriculum-sumit.json')
        } else if (queryType === 'certification') {
          heldLearningJson = require('./saba/response/held-certifications-sumit.json')
        }
        break
      case 'emplo000000000016552':
        if (queryType === 'curriculum') {
          heldLearningJson = require('./saba/response/empty-held-curriculum.json')
        } else if (queryType === 'certification') {
          heldLearningJson = require('./saba/response/empty-held-certifications.json')
        }
        break
      case 'emplo000000000415361':
        if (queryType === 'curriculum') {
          heldLearningJson = require('./saba/response/held-curriculum-ryan.json')
        } else if (queryType === 'certification') {
          heldLearningJson = require('./saba/response/empty-held-certifications.json')
        }
        break
      default:
        throw new Error('Unexpected employee id.')
    }

    return res.json(heldLearningJson)
  })

  app.get('/v1/learningmodule/:moduleId', (req, res) => {
    const moduleId = req.params.moduleId
    mfCommons.log('learning module id: ' + moduleId)

    let moduleJson
    switch (moduleId) {
      case 'curra000000000005167':
        moduleJson = require('./saba/response/module-curra-5167.json')
        break
      case 'crtfy000000000967642':
        moduleJson = require('./saba/response/module-crtfy-7642.json')
        break
      case 'crtfy000000000970004':
        moduleJson = require('./saba/response/module-crtfy-0004.json')
        break
      case 'cours000000000007197':
        moduleJson = require('./saba/response/module-course-7197.json')
        break
      case 'cours000000000030552':
        moduleJson = require('./saba/response/module-course-0552.json')
        break
      default:
        throw new Error('Unexpected module id. ' + moduleId)
    }

    return res.json(moduleJson)
  })

  app.get('/v1/enrollments/:enrollmentId/sections:regdetail', (req, res) => {
    return res.json(require('./saba/response/enrollment-details-6949.json'))
  })

  fakeBackend = app.listen(4000, () => console.log('Fake backend listening on 4000'))
}

const readFromQueryQ = (q, readFilter) => {
  const qItems = q.slice(1, -1).split(',')
  const item = qItems.find(item => item.includes(readFilter))

  if (item) {
    return item.split(readFilter)[1]
  }
}

const start = () => {
  startFakeBackend()
}

const stop = () => {
  fakeBackend.close()
}

module.exports = {
  start,
  stop
}
