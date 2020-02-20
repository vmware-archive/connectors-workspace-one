const result = (baseurl) => {
  return {
    size: 1,
    start: 0,
    limit: 50,
    isLastPage: true,
    _links: {
      self: `${baseurl}/servicedesk`,
      base: 'https://mobileflows.atlassian.net',
      context: ''
    },
    values: [
      {
        id: '1',
        projectId: '10000',
        projectName: 'First service desk project',
        projectKey: 'FSDP',
        _links: {
          self: `${baseurl}/servicedesk/1`
        }
      }
    ]
  }
}

module.exports = result
