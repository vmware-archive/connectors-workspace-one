const result = (baseurl) => {
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
    size: 1,
    start: 0,
    limit: 50,
    isLastPage: true,
    _links: {
      self: `${baseurl}/request?approvalStatus=MY_PENDING_APPROVAL&expand=requestType&requestOwnership=APPROVER&requestStatus=OPEN_REQUESTS`,
      base: 'https://mobileflows.atlassian.net',
      context: ''
    },
    values: [
      {
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
        issueId: '10069',
        issueKey: 'FSDP-68',
        requestTypeId: '14',
        requestType: {
          _expands: [
            'field'
          ],
          id: '14',
          _links: {
            self: `${baseurl}/servicedesk/1/requesttype/14`
          },
          name: 'New mobile device',
          description: 'Need a mobile phone or time for replacement? Let us know.',
          helpText: '',
          issueTypeId: '10102',
          serviceDeskId: '1',
          groupIds: [
            '2'
          ],
          icon: {
            id: '10471',
            _links: {
              iconUrls: {
                '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10471&size=large',
                '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10471&size=small',
                '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10471&size=xsmall',
                '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10471&size=medium'
              }
            }
          }
        },
        serviceDeskId: '1',
        createdDate: {
          iso8601: '2019-08-27T15:05:19-0400',
          jira: '2019-08-27T15:05:19.131-0400',
          friendly: 'Today 3:05 PM',
          epochMillis: 1566932719131
        },
        reporter: {
          accountId: 'qm:52e1efdc-0869-430c-adcf-4b538a3f98e5:d0b0ecbf-964d-4394-a6da-6aaf4f0d2784',
          name: 'qm:52e1efdc-0869-430c-adcf-4b538a3f98e5:d0b0ecbf-964d-4394-a6da-6aaf4f0d2784',
          key: 'qm:52e1efdc-0869-430c-adcf-4b538a3f98e5:d0b0ecbf-964d-4394-a6da-6aaf4f0d2784',
          emailAddress: 'davidpshaw@gmail.com',
          displayName: 'David Customer',
          active: true,
          timeZone: 'America/New_York',
          _links: {
            jiraRest: `${baseurl}/ex/jira/cac1784e-ee56-43f1-a806-593cb9c22d00/rest/api/2/user?accountId=qm%3A52e1efdc-0869-430c-adcf-4b538a3f98e5%3Ad0b0ecbf-964d-4394-a6da-6aaf4f0d2784`,
            avatarUrls: {
              '48x48': 'https://avatar-cdn.atlassian.com/default?size=48&s=48',
              '24x24': 'https://avatar-cdn.atlassian.com/default?size=24&s=24',
              '16x16': 'https://avatar-cdn.atlassian.com/default?size=16&s=16',
              '32x32': 'https://avatar-cdn.atlassian.com/default?size=32&s=32'
            },
            self: `${baseurl}/ex/jira/cac1784e-ee56-43f1-a806-593cb9c22d00/rest/api/2/user?accountId=qm%3A52e1efdc-0869-430c-adcf-4b538a3f98e5%3Ad0b0ecbf-964d-4394-a6da-6aaf4f0d2784`
          }
        },
        requestFieldValues: [
          {
            fieldId: 'summary',
            label: 'Summary',
            value: 'REQUEST_SUMMARY_HERE'
          },
          {
            fieldId: 'description',
            label: 'Phone details and justification',
            value: 'DETAILS_AND_JUSTIFICATION_HERE',
            renderedValue: {
              html: '<p>DETAILS_AND_JUSTIFICATION_HERE</p>'
            }
          },
          {
            fieldId: 'attachment',
            label: 'Attachment',
            value: []
          }
        ],
        currentStatus: {
          status: 'Waiting for approval',
          statusCategory: 'NEW',
          statusDate: {
            iso8601: '2019-08-27T15:05:19-0400',
            jira: '2019-08-27T15:05:19.131-0400',
            friendly: 'Today 3:05 PM',
            epochMillis: 1566932719131
          }
        },
        _links: {
          jiraRest: 'https://mobileflows.atlassian.net/rest/api/2/issue/10069',
          web: 'https://mobileflows.atlassian.net/servicedesk/customer/portal/1/FSDP-68',
          self: `${baseurl}/request/10069`
        }
      }
    ]
  }
}

module.exports = result
