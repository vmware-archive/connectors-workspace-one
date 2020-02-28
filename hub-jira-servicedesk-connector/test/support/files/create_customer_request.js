const result = (baseUrl, summary, description) => {
  return {
    _expands: [
      'participant',
      'status',
      'sla',
      'requestType',
      'serviceDesk',
      'attachment',
      'action',
      'comment'
    ],
    issueId: '10082',
    issueKey: 'FSDP-79',
    requestTypeId: '1',
    serviceDeskId: '1',
    createdDate: {
      iso8601: '2019-09-12T05:17:35-0400',
      jira: '2019-09-12T05:17:35.285-0400',
      friendly: 'Today 5:17 AM',
      epochMillis: 1568279855285
    },
    reporter: {
      accountId: '5c99cea5c430371a3c67ea01',
      name: 'makrands',
      key: 'makrands',
      displayName: 'makrands@vmware.com',
      active: true,
      timeZone: 'America/New_York',
      _links: {
        jiraRest: `${baseUrl}/ex/jira/cac1784e-ee56-43f1-a806-593cb9c22d00/rest/api/2/user?accountId=5c99cea5c430371a3c67ea01`,
        avatarUrls: {
          '48x48': 'https://secure.gravatar.com/avatar/86d34d026430f7936a1cbdb10b928bfa?d=https%3A%2F%2Favatar-management--avatars.us-west-2.prod.public.atl-paas.net%2Finitials%2FM-3.png&size=48&s=48',
          '24x24': 'https://secure.gravatar.com/avatar/86d34d026430f7936a1cbdb10b928bfa?d=https%3A%2F%2Favatar-management--avatars.us-west-2.prod.public.atl-paas.net%2Finitials%2FM-3.png&size=24&s=24',
          '16x16': 'https://secure.gravatar.com/avatar/86d34d026430f7936a1cbdb10b928bfa?d=https%3A%2F%2Favatar-management--avatars.us-west-2.prod.public.atl-paas.net%2Finitials%2FM-3.png&size=16&s=16',
          '32x32': 'https://secure.gravatar.com/avatar/86d34d026430f7936a1cbdb10b928bfa?d=https%3A%2F%2Favatar-management--avatars.us-west-2.prod.public.atl-paas.net%2Finitials%2FM-3.png&size=32&s=32'
        },
        self: `${baseUrl}/ex/jira/cac1784e-ee56-43f1-a806-593cb9c22d00/rest/api/2/user?accountId=5c99cea5c430371a3c67ea01`
      }
    },
    requestFieldValues: [
      {
        fieldId: 'summary',
        label: 'Summary',
        value: `${summary}`
      },
      {
        fieldId: 'description',
        label: 'Description',
        value: `${description}`,
        renderedValue: {
          html: `<p>${description}</p>`
        }
      },
      {
        fieldId: 'attachment',
        label: 'Attachment',
        value: [

        ]
      }
    ],
    currentStatus: {
      status: 'Waiting for support',
      statusCategory: 'INDETERMINATE',
      statusDate: {
        iso8601: '2019-09-12T05:17:35-0400',
        jira: '2019-09-12T05:17:35.285-0400',
        friendly: 'Today 5:17 AM',
        epochMillis: 1568279855285
      }
    },
    _links: {
      web: 'https://mobileflows.atlassian.net/servicedesk/customer/portal/1/FSDP-79',
      self: `${baseUrl}/ex/jira/cac1784e-ee56-43f1-a806-593cb9c22d00/rest/servicedeskapi/request/10082`
    }
  }
}

module.exports = result
