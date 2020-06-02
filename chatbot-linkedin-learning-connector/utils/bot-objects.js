
const { v4: uuid } = require('uuid')

/*
 * Create bot objects form linkedIn learning response.
 * @param res - LinkedIn learning response object.
 * @param workflowId - Chatbot workflow Id.
 */
const forBotObjects = (res, workflowId) => {
  if (res.length === 0) {
    return [getItemForZeroResults()]
  }
  const courseObjects = res.map(course => forSingleCourse(course, workflowId))
  courseObjects.unshift(beginningMessage(workflowId))
  courseObjects.push(endMessage(workflowId))
  return courseObjects
}

/*
 * Create single bot object form cousre response.
 * @param courseRes - LinkedIn learning response object.
 * @param workflowId - Chatbot workflow Id.
 */
const forSingleCourse = (courseRes, workflowId) => {
  return {
    itemDetails: {
      id: uuid(),
      title: courseRes.title.value + ' ' + courseRes.details.urls.webLaunch,
      subtitle: getContributerNames(courseRes.details.contributors),
      description: courseRes.details.description.value,
      shortDescription: courseRes.details.shortDescription.value,
      url: {
        href: courseRes.details.urls.webLaunch
      },
      image: {
        href: courseRes.details.images.primary
      },
      workflowId: workflowId,
      workflowStep: 'Complete',
      type: 'text'
    }
  }
}

/*
 * Helper method to create beginning message for chat bot
 * @param workflowId - Chatbot workflow Id.
 */
const beginningMessage = (workflowId) => {
  return {
    itemDetails: {
      id: uuid(),
      title: 'Here are the top courses I found:',
      workflowId: workflowId,
      workflowStep: 'Complete',
      type: 'text'
    }
  }
}

/*
 * Helper method to create end message for chat bot
 * @param workflowId - Chatbot workflow Id.
 */
const endMessage = (workflowId) => {
  return {
    itemDetails: {
      id: uuid(),
      title: 'Did you find what you’re looking for? If not, you can view more courses here: https://www.linkedin.com/learning/',
      workflowId: workflowId,
      workflowStep: 'Complete',
      type: 'text'
    }
  }
}

/*
 * Helper method to create no result message when no course found
 */
const getItemForZeroResults = () => {
  return {
    itemDetails: {
      title: 'I’m sorry. I could not find any courses related to that search. You can view more courses here: https://www.linkedin.com/learning/',
      description: 'Please try again with different search',
      type: 'text'
    }
  }
}

/*
 * Helper method to get contributer name from response
 * @param contributers - contributers response.
 */
const getContributerNames = (contributers) => {
  return contributers.map(ticket => getSingleAuthorName(ticket)).join(', ')
}

const getSingleAuthorName = (contributer) => {
  return contributer.name.value
}

module.exports = {
  forBotObjects
}
