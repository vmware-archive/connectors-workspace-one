/*
 * Create card objects form linkedIn learning response.
 * @param res - LinkedIn learning response object.
 */
const uuidV4 = require('uuid/v4')
const sha1 = require('sha1')
const utils = require('../utils/utility')

const forCardObjects = (res) => {
  return res.map(course => forSingleCourse(course))
}

/*
 * Create single bot object form cousre response.
 * @param courseRes - LinkedIn learning response object.
 */
const forSingleCourse = (courseRes) => {
  const backendId = courseRes.urn
  return {
    id: uuidV4(),
    name: 'LinkedIn Learning',
    creation_date: new Date().toISOString(),
    backend_id: backendId,
    hash: sha1(backendId),
    header: {
      title: 'LinkedIn Learning: New course available',
      subtitle: [
        'View course'
      ],
      links: {
        title: courseRes.details.urls.webLaunch,
        subtitle: [
          courseRes.details.urls.webLaunch
        ]
      }
    },
    image: {
      href: utils.LINKEDIN_LOGO_PATH
    },
    body: {
      fields: [
        {
          type: 'GENERAL',
          title: 'Title',
          description: courseRes.title.value
        },
        {
          type: 'GENERAL',
          title: 'Instructor',
          description: getContributerNames(courseRes.details.contributors)
        },
        {
          type: 'GENERAL',
          title: 'Duration',
          description: convertSecondsToReadableFormat(courseRes.details.timeToComplete.duration)
        },
        {
          type: 'SECTION',
          title: 'About Course',
          items: [
            {
              type: 'GENERAL',
              title: 'Description',
              description: courseRes.details.description.value
            },
            {
              type: 'GENERAL',
              title: 'Category',
              description: getCategories(courseRes.details.classifications)
            },
            {
              type: 'GENERAL',
              title: 'Released',
              description: new Date(courseRes.details.publishedAt).toISOString()
            },
            {
              type: 'GENERAL',
              title: 'Updated at',
              description: new Date(courseRes.details.lastUpdatedAt).toISOString()
            },
            {
              type: 'GENERAL',
              title: 'Difficulty',
              description: courseRes.details.level
            }
          ]
        }
      ]
    }
  }
}

/*
 * Helper method to get contributer name from response
 * @param contributers - contributers response.
 */
const getContributerNames = (contributers) => {
  return contributers.map(contributer => getSingleAuthorName(contributer)).join(', ')
}

const getSingleAuthorName = (contributer) => {
  return contributer.name.value
}

/*
 * Helper method to get course categories from response
 * @param categories - course category response.
 */
const getCategories = (categories) => {
  return categories.map(category => getSingleCategory(category)).join(', ')
}

const getSingleCategory = (category) => {
  return category.associatedClassification.name.value
}

/*
 * Helper method to convert seconds into Day hour format similar to linkedIn website
 * @param seconds - duration of course.
 */
const convertSecondsToReadableFormat = (seconds) => {
  const days = Math.floor(seconds / (24 * 60 * 60))
  seconds -= days * (24 * 60 * 60)
  const hours = Math.floor(seconds / (60 * 60))
  seconds -= hours * (60 * 60)
  const minutes = Math.floor(seconds / (60))
  return ((days > 0) ? (days + ' day, ') : '') + hours + 'h ' + minutes + 'm'
}

module.exports = {
  forCardObjects
}
