const result = (baseurl, serviceDeskId) => {
  return {
    _expands: [
      'field'
    ],
    size: 17,
    start: 0,
    limit: 50,
    isLastPage: true,
    _links: {
      self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype`,
      base: 'https://mobileflows.atlassian.net',
      context: ''
    },
    values: [
      {
        _expands: [
          'field'
        ],
        id: '1',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/1`
        },
        name: 'Get IT help',
        description: 'Get assistance for general IT problems and questions.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '2',
          '1'
        ],
        icon: {
          id: '10491',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10491&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10491&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10491&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10491&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '2',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/2`
        },
        name: 'Fix an account problem',
        description: "Having trouble accessing certain websites or systems? We'll help you out.",
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '3'
        ],
        icon: {
          id: '10472',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10472&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10472&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10472&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10472&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '3',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/3`
        },
        name: 'Get a guest wifi account',
        description: 'Raise a request to ask for temp wifi access for guests.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '3'
        ],
        icon: {
          id: '10474',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10474&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10474&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10474&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10474&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '4',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/4`
        },
        name: 'Set up VPN to the office',
        description: 'Want to access work stuff from outside? Let us know.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '3',
          '1'
        ],
        icon: {
          id: '10506',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10506&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10506&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10506&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10506&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '5',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/5`
        },
        name: 'Request admin access',
        description: 'For example, if you need to administer Jira.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '3'
        ],
        icon: {
          id: '10504',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10504&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10504&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10504&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10504&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '6',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/6`
        },
        name: 'Request a new account',
        description: 'Request a new account for a system.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '3',
          '1'
        ],
        icon: {
          id: '10494',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10494&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10494&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10494&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10494&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '7',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/7`
        },
        name: 'Onboard new employees',
        description: 'Request access for new employees.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '3'
        ],
        icon: {
          id: '10499',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10499&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10499&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10499&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10499&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '8',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/8`
        },
        name: 'Desktop/Laptop support',
        description: 'If you are having computer problems, let us know here.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '2',
          '1'
        ],
        icon: {
          id: '10470',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10470&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10470&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10470&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10470&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '9',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/9`
        },
        name: 'Set up a phone line redirect',
        description: 'Request a redirect of our phone systems for a specific date and time.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '4'
        ],
        icon: {
          id: '10477',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10477&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10477&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10477&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10477&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '10',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/10`
        },
        name: 'Request new software',
        description: 'If you need a software license, raise a request here.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '2',
          '5'
        ],
        icon: {
          id: '10479',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10479&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10479&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10479&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10479&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '11',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/11`
        },
        name: 'Request new hardware',
        description: 'For example, a new mouse or monitor.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '2'
        ],
        icon: {
          id: '10475',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10475&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10475&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10475&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10475&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '12',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/12`
        },
        name: 'Request a desk phone',
        description: "If you'd like to request a desk phone, get one here.",
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [
          '1'
        ],
        icon: {
          id: '10466',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10466&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10466&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10466&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10466&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '13',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/13`
        },
        name: 'Emailed request',
        description: 'Request received from your email support channel.',
        helpText: '',
        issueTypeId: '10101',
        serviceDeskId: '1',
        groupIds: [],
        icon: {
          id: '10492',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10492&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10492&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10492&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10492&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '14',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/14`
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
      {
        _expands: [
          'field'
        ],
        id: '15',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/15`
        },
        name: 'Report a system problem',
        description: 'Having trouble with a system?',
        helpText: '',
        issueTypeId: '10100',
        serviceDeskId: '1',
        groupIds: [
          '5',
          '4',
          '1'
        ],
        icon: {
          id: '10508',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10508&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10508&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10508&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10508&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '16',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/16`
        },
        name: 'Upgrade or change a server',
        description: 'For example, upgrade the VPN server.',
        helpText: '',
        issueTypeId: '10103',
        serviceDeskId: '1',
        groupIds: [
          '4'
        ],
        icon: {
          id: '10510',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10510&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10510&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10510&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10510&size=medium'
            }
          }
        }
      },
      {
        _expands: [
          'field'
        ],
        id: '17',
        _links: {
          self: `${baseurl}/servicedesk/${serviceDeskId}/requesttype/17`
        },
        name: 'Upgrade or change a managed system',
        description: 'For example, upgrade Jira.',
        helpText: '',
        issueTypeId: '10103',
        serviceDeskId: '1',
        groupIds: [
          '5'
        ],
        icon: {
          id: '10503',
          _links: {
            iconUrls: {
              '48x48': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10503&size=large',
              '24x24': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10503&size=small',
              '16x16': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10503&size=xsmall',
              '32x32': 'https://mobileflows.atlassian.net/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=10503&size=medium'
            }
          }
        }
      }
    ]
  }
}

module.exports = result
