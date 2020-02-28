const result = (baseURL, approvalIssueId, approvalId, decision) => {
  return {
    id: `${approvalId}`,
    name: 'Waiting for approval',
    finalDecision: `${decision}`,
    canAnswerApproval: false,
    approvers: [
      {
        approver: {
          accountId: '557058:af9709dc-fafa-4c0f-9f06-127a21adf666',
          name: 'admin',
          key: 'admin',
          displayName: 'David Shaw',
          active: true,
          timeZone: 'America/New_York',
          _links: {
            jiraRest: `${baseURL}ex/jira/cac1784e-ee56-43f1-a806-593cb9c22d00/rest/api/2/user?accountId=557058%3Aaf9709dc-fafa-4c0f-9f06-127a21adf666`,
            avatarUrls: {
              '48x48': 'https://secure.gravatar.com/avatar/62a8e368d9e3e10b0608ff1fb1b1ef7d?d=https%3A%2F%2Favatar-management--avatars.us-west-2.prod.public.atl-paas.net%2Finitials%2FDS-2.png&size=48&s=48',
              '24x24': 'https://secure.gravatar.com/avatar/62a8e368d9e3e10b0608ff1fb1b1ef7d?d=https%3A%2F%2Favatar-management--avatars.us-west-2.prod.public.atl-paas.net%2Finitials%2FDS-2.png&size=24&s=24',
              '16x16': 'https://secure.gravatar.com/avatar/62a8e368d9e3e10b0608ff1fb1b1ef7d?d=https%3A%2F%2Favatar-management--avatars.us-west-2.prod.public.atl-paas.net%2Finitials%2FDS-2.png&size=16&s=16',
              '32x32': 'https://secure.gravatar.com/avatar/62a8e368d9e3e10b0608ff1fb1b1ef7d?d=https%3A%2F%2Favatar-management--avatars.us-west-2.prod.public.atl-paas.net%2Finitials%2FDS-2.png&size=32&s=32'
            },
            self: `${baseURL}ex/jira/cac1784e-ee56-43f1-a806-593cb9c22d00/rest/api/2/user?accountId=557058%3Aaf9709dc-fafa-4c0f-9f06-127a21adf666`
          }
        },
        approverDecision: `${decision}`
      }
    ],
    createdDate: {
      iso8601: '2019-08-27T15:05:19-0400',
      jira: '2019-08-27T15:05:19.380-0400',
      friendly: 'Today 3:05 PM',
      epochMillis: 1566932719380
    },
    completedDate: {
      iso8601: '2019-08-27T15:23:45-0400',
      jira: '2019-08-27T15:23:45.131-0400',
      friendly: 'Today 3:23 PM',
      epochMillis: 1566933825131
    },
    _links: {
      self: `${baseURL}ex/jira/cac1784e-ee56-43f1-a806-593cb9c22d00/rest/servicedeskapi/request/${approvalIssueId}/approval/${approvalId}`
    }
  }
}

module.exports = result
