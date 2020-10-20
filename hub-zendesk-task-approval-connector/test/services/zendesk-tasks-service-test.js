/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { describe } = require('mocha')
const { expect } = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const nock = require('nock')
const { getUserInfo, getUserTickets, getGroups, getTicketComments, updateTicketStatus, getUsers } = require('../../services/zendesk-tasks-service')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('Service API tests', () => {
  describe('getUserInfo', () => {
    it('Get userInfo by emailId', async () => {
      nock('https://vmware.zendesk.com')
        .get('/api/v2/search.json')
        .query(true)
        .reply(200, userInfoMockResp)
      const response = await getUserInfo({
        backendAuthorization: 'Bearer connectorAuthorizationValue',
        backendBaseUrl: 'https://vmware.zendesk.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      })
      expect(typeof response).to.equal('object')
      expect(response.name).to.equal('Srini Gargeya')
      expect(response.id).to.equal(397911617552)
    })

    it('getUserInfo in network error', async () => {
      nock('https://vmware.zendesk.com')
        .get('/api/v2/search.json')
        .query(true)
        .reply(400)
      try {
        await getUserInfo({
          backendAuthorization: 'Bearer connectorAuthorizationValue',
          backendBaseUrl: 'https://vmware.zendesk.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        })
        sinon.assert.fail('User info should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(400)
      }
    })
  })

  describe('getUserTickets', () => {
    it('Search ticket valid flow', async () => {
      nock('https://vmware.zendesk.com')
        .get('/api/v2/search.json')
        .query(true)
        .reply(200, userTickets)
      const response = await getUserTickets({
        backendAuthorization: 'Bearer connectorAuthorizationValue',
        backendBaseUrl: 'https://vmware.zendesk.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, 397911617552)
      expect(typeof response).to.equal('object')
      expect(response.length).to.equal(3)
      expect(response[0].id).to.equal(6)
    })

    it('getUserTickets in network error', async () => {
      nock('https://vmware.zendesk.com')
        .get('/api/v2/search.json')
        .query(true)
        .reply(401)
      try {
        await getUserTickets({
          backendAuthorization: 'Bearer connectorAuthorizationValue',
          backendBaseUrl: 'https://vmware.zendesk.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, 397911617552)
        sinon.assert.fail('getUserTickets should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })

  describe('getTicketComments', () => {
    it('Comments happy path', async () => {
      nock('https://vmware.zendesk.com')
        .get('/api/v2/tickets/2/comments.json')
        .reply(200, comments)
      const response = await getTicketComments({
        backendAuthorization: 'Bearer connectorAuthorizationValue',
        backendBaseUrl: 'https://vmware.zendesk.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, 2)
      expect(typeof response).to.equal('object')
      expect(response.length).to.equal(2)
      expect(response[0].id).to.equal(1062311652711)
      expect(response[1].attachments.length).to.equal(1)
      expect(response[1].attachments[0].id).to.equal(376333956311)
    })

    it('getTicketComments in network error', async () => {
      nock('https://vmware.zendesk.com')
        .get('/api/v2/tickets/2/comments.json')
        .reply(401)
      try {
        await getTicketComments({
          backendAuthorization: 'Bearer connectorAuthorizationValue',
          backendBaseUrl: 'https://vmware.zendesk.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, 2)
        sinon.assert.fail('getTicketComments should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })

  describe('getUsers', () => {
    it('get users', async () => {
      nock('https://vmware.zendesk.com')
        .get('/api/v2/users/show_many.json')
        .query(true)
        .reply(200, users)
      const response = await getUsers({
        backendAuthorization: 'Bearer connectorAuthorizationValue',
        backendBaseUrl: 'https://vmware.zendesk.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, 2)
      expect(typeof response).to.equal('object')
      expect(response.length).to.equal(2)
      expect(response[0].id).to.equal(398627745992)
      expect(response[1].id).to.equal(398698541951)
    })

    it('get users in network error', async () => {
      nock('https://vmware.zendesk.com')
        .get('/api/v2/users/show_many.json')
        .query(true)
        .reply(401)
      try {
        await getUsers({
          backendAuthorization: 'Bearer connectorAuthorizationValue',
          backendBaseUrl: 'https://vmware.zendesk.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, 2)
        sinon.assert.fail('getUsers should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })

  describe('getGroups', () => {
    it('get groups', async () => {
      nock('https://vmware.zendesk.com')
        .get('/api/v2/groups.json')
        .reply(200, groups)
      const response = await getGroups({
        backendAuthorization: 'Bearer connectorAuthorizationValue',
        backendBaseUrl: 'https://vmware.zendesk.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, 2)
      expect(typeof response).to.equal('object')
      expect(response[0].id).to.equal(360009305072)
    })

    it('get groups in network error', async () => {
      nock('https://vmware.zendesk.com')
        .get('/api/v2/groups.json')
        .reply(401)
      try {
        await getGroups({
          backendAuthorization: 'Bearer connectorAuthorizationValue',
          backendBaseUrl: 'https://vmware.zendesk.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, 2)
        sinon.assert.fail('getGroups should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })

  describe('updateTicketStatus', () => {
    it('updateTicketStatus happy path', async () => {
      nock('https://vmware.zendesk.com')
        .put('/api/v2/tickets/9.json',
          {
            ticket: {
              status: 'pending',
              comment: 'Changing status to pending as there is a delay in customer response'
            }
          }
        )
        .reply(200, ticketStatusChangeResp)
      const response = await updateTicketStatus({
        backendAuthorization: 'Bearer connectorAuthorizationValue',
        backendBaseUrl: 'https://vmware.zendesk.com',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      }, 9, 'pending', 'Changing status to pending as there is a delay in customer response')
      expect(typeof response).to.equal('object')
      expect(response.ticket.id).to.equal(9)
      expect(response.ticket.status).to.equal('pending')
      expect(response.ticket.description).to.equal('I need a replacement for my laptop charger')
      expect(response.audit.events[0].body).to.equal('Changing status to pending as there is a delay in customer response')
    })

    it('updateTicketStatus in network error', async () => {
      nock('https://vmware.zendesk.com')
        .put('/api/v2/tickets/9.json',
          {
            ticket: {
              status: 'closed',
              comment: 'Closing ticket as service is done'
            }
          }
        )
        .reply(401)
      try {
        await updateTicketStatus({
          backendAuthorization: 'Bearer connectorAuthorizationValue',
          backendBaseUrl: 'https://vmware.zendesk.com',
          mfJwt: { email: 'srini.gargeya@vmware.com' }
        }, 9, 'closed', 'Closing ticket as service is done')
        sinon.assert.fail('updateTicketStatus should throw exception. so this path should not be reachable')
      } catch (err) {
        const error = JSON.parse(err.message)
        expect(error.statusCode).to.equal(401)
      }
    })
  })
})

const userInfoMockResp = {
  results: [
    {
      id: 397911617552,
      url: 'https://vmware.zendesk.com/api/v2/users/397911617552.json',
      name: 'Srini Gargeya',
      email: 'srini.gargeya@vmware.com',
      created_at: '2020-05-15T06:24:01Z',
      updated_at: '2020-06-01T14:16:56Z',
      time_zone: 'Arizona',
      iana_time_zone: 'America/Phoenix',
      phone: null,
      shared_phone_number: null,
      photo: null,
      locale_id: 1,
      locale: 'en-US',
      organization_id: 361067840292,
      role: 'admin',
      verified: true,
      external_id: null,
      tags: [],
      alias: null,
      active: true,
      shared: false,
      shared_agent: false,
      last_login_at: '2020-06-01T14:16:56Z',
      two_factor_auth_enabled: null,
      signature: null,
      details: null,
      notes: null,
      role_type: null,
      custom_role_id: null,
      moderator: true,
      ticket_restriction: null,
      only_private_comments: false,
      restricted_agent: false,
      suspended: false,
      chat_only: false,
      default_group_id: 360009305072,
      report_csv: true,
      user_fields: {},
      result_type: 'user'
    }
  ],
  facets: null,
  next_page: null,
  previous_page: null,
  count: 1
}

const userTickets = {
  results: [
    {
      url: 'https://vmware.zendesk.com/api/v2/tickets/6.json',
      id: 6,
      external_id: null,
      via: {
        channel: 'web',
        source: {
          from: {},
          to: {},
          rel: null
        }
      },
      created_at: '2020-05-28T12:16:49Z',
      updated_at: '2020-05-28T12:16:49Z',
      type: null,
      subject: 'Phone is not working',
      raw_subject: 'Phone is not working',
      description: 'Phone is not working for last one week',
      priority: null,
      status: 'open',
      recipient: null,
      requester_id: 397911617552,
      submitter_id: 397911617552,
      assignee_id: 397911617552,
      organization_id: 361067840292,
      group_id: 360009305072,
      collaborator_ids: [],
      follower_ids: [],
      email_cc_ids: [],
      forum_topic_id: null,
      problem_id: null,
      has_incidents: false,
      is_public: true,
      due_at: null,
      tags: [],
      custom_fields: [],
      satisfaction_rating: null,
      sharing_agreement_ids: [],
      fields: [],
      followup_ids: [],
      brand_id: 360003980972,
      allow_channelback: false,
      allow_attachments: true,
      result_type: 'ticket'
    },
    {
      url: 'https://vmware.zendesk.com/api/v2/tickets/2.json',
      id: 2,
      external_id: null,
      via: {
        channel: 'api',
        source: {
          from: {},
          to: {},
          rel: null
        }
      },
      created_at: '2020-05-26T08:37:39Z',
      updated_at: '2020-05-28T09:26:52Z',
      type: null,
      subject: 'My printer is not working!',
      raw_subject: 'My printer is not working!',
      description: 'The printer head is making lot of noise.',
      priority: null,
      status: 'pending',
      recipient: null,
      requester_id: 397911617552,
      submitter_id: 397911617552,
      assignee_id: 397911617552,
      organization_id: 361067840292,
      group_id: 360009305072,
      collaborator_ids: [],
      follower_ids: [],
      email_cc_ids: [],
      forum_topic_id: null,
      problem_id: null,
      has_incidents: false,
      is_public: true,
      due_at: null,
      tags: [],
      custom_fields: [],
      satisfaction_rating: null,
      sharing_agreement_ids: [],
      fields: [],
      followup_ids: [],
      brand_id: 360003980972,
      allow_channelback: false,
      allow_attachments: true,
      result_type: 'ticket'
    },
    {
      url: 'https://vmware.zendesk.com/api/v2/tickets/1.json',
      id: 1,
      external_id: null,
      via: {
        channel: 'sample_ticket',
        source: {
          from: {},
          to: {},
          rel: null
        }
      },
      created_at: '2020-05-15T06:24:01Z',
      updated_at: '2020-05-28T07:28:01Z',
      type: 'incident',
      subject: 'Sample ticket: Meet the ticket',
      raw_subject: 'Sample ticket: Meet the ticket',
      description: 'Hi Srini,\n\nThis is your first ticket. Ta-da! Any customer request sent to your supported channels (email, chat, voicemail, web form, and tweet) will become a Support ticket, just like this one. Respond to this ticket by typing a message above and clicking Submit. You can also see how an email becomes a ticket by emailing your new account, support@vmware.zendesk.com. Your ticket will appear in ticket views.\n\nThat\'s the ticket on tickets. If you want to learn more, check out: \nhttps://support.zendesk.com/hc/en-us/articles/203691476\n',
      priority: 'normal',
      status: 'closed',
      recipient: null,
      requester_id: 397922704332,
      submitter_id: 397911617552,
      assignee_id: 397911617552,
      organization_id: null,
      group_id: 360009305072,
      collaborator_ids: [],
      follower_ids: [],
      email_cc_ids: [],
      forum_topic_id: null,
      problem_id: null,
      has_incidents: false,
      is_public: true,
      due_at: null,
      tags: [
        'sample',
        'support',
        'zendesk'
      ],
      custom_fields: [],
      satisfaction_rating: null,
      sharing_agreement_ids: [],
      fields: [],
      followup_ids: [],
      brand_id: 360003980972,
      allow_channelback: false,
      allow_attachments: true,
      result_type: 'ticket'
    }
  ],
  facets: null,
  next_page: null,
  previous_page: null,
  count: 3
}

const comments = {
  comments: [
    {
      id: 1062311652711,
      type: 'Comment',
      author_id: 397911617552,
      body: 'The printer head is making lot of noise.',
      html_body: '<div class="zd-comment" dir="auto"><p dir="auto">The printer head is making lot of noise.</p></div>',
      plain_body: 'The printer head is making lot of noise.',
      public: true,
      attachments: [],
      audit_id: 1062311652591,
      via: {
        channel: 'api',
        source: {
          from: {},
          to: {
            name: 'Srini Gargeya',
            address: 'srini.gargeya@xenovus.com'
          },
          rel: null
        }
      },
      created_at: '2020-05-26T08:37:39Z',
      metadata: {
        system: {
          client: 'insomnia/7.1.1',
          ip_address: '49.207.136.123',
          location: 'New Delhi, DL, India',
          latitude: 28.6014,
          longitude: 77.1989
        },
        custom: {}
      }
    },
    {
      id: 1063499358972,
      type: 'Comment',
      author_id: 397911617552,
      body: 'this is to check attachments',
      html_body: '<div class="zd-comment" dir="auto">this is to check attachments<br><br><br></div>',
      plain_body: 'this is to check attachments',
      public: false,
      attachments: [
        {
          url: 'https://backflipt.zendesk.com/api/v2/attachments/376333956311.json',
          id: 376333956311,
          file_name: 'attach.txt',
          content_url: 'https://backflipt.zendesk.com/attachments/token/bXPAYsV5ki3iU0aWEqL5Bzc70/?name=attach.txt',
          mapped_content_url: 'https://backflipt.zendesk.com/attachments/token/bXPAYsV5ki3iU0aWEqL5Bzc70/?name=attach.txt',
          content_type: 'text/plain',
          size: 42,
          width: null,
          height: null,
          inline: false,
          deleted: false,
          thumbnails: []
        }
      ],
      audit_id: 1063499358872,
      via: {
        channel: 'web',
        source: {
          from: {},
          to: {},
          rel: null
        }
      },
      created_at: '2020-05-26T09:23:00Z',
      metadata: {
        system: {
          client: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36',
          ip_address: '49.207.136.123',
          location: 'New Delhi, DL, India',
          latitude: 28.6014,
          longitude: 77.1989
        },
        custom: {}
      }
    }
  ],
  next_page: null,
  previous_page: null,
  count: 2
}

const ticketStatusChangeResp = {
  ticket: {
    url: 'https://backflipt.zendesk.com/api/v2/tickets/9.json',
    id: 9,
    external_id: null,
    via: {
      channel: 'web',
      source: {
        from: {},
        to: {},
        rel: null
      }
    },
    created_at: '2020-06-02T06:07:30Z',
    updated_at: '2020-06-02T07:22:23Z',
    type: null,
    subject: 'Need replacment for Laptop charger',
    raw_subject: 'Need replacment for Laptop charger',
    description: 'I need a replacement for my laptop charger',
    priority: null,
    status: 'pending',
    recipient: null,
    requester_id: 398698541951,
    submitter_id: 398627745992,
    assignee_id: 398627745992,
    organization_id: null,
    group_id: 360009305072,
    collaborator_ids: [],
    follower_ids: [],
    email_cc_ids: [],
    forum_topic_id: null,
    problem_id: null,
    has_incidents: false,
    is_public: true,
    due_at: null,
    tags: [],
    custom_fields: [],
    satisfaction_rating: null,
    sharing_agreement_ids: [],
    fields: [],
    followup_ids: [],
    brand_id: 360003980972,
    allow_channelback: false,
    allow_attachments: true
  },
  audit: {
    id: 1071213648931,
    ticket_id: 9,
    created_at: '2020-06-02T07:22:23Z',
    author_id: 397911617552,
    metadata: {
      system: {
        client: 'insomnia/7.1.1',
        ip_address: '146.196.38.93',
        location: 'New Delhi, DL, India',
        latitude: 28.6014,
        longitude: 77.1989
      },
      custom: {}
    },
    events: [
      {
        id: 1071213649011,
        type: 'Comment',
        author_id: 397911617552,
        body: 'Changing status to pending as there is a delay in customer response',
        html_body: '<div class="zd-comment" dir="auto"><p dir="auto">Changing status to pending as there is a delay in customer response</p></div>',
        plain_body: 'Changing status to pending as there is a delay in customer response',
        public: true,
        attachments: [],
        audit_id: 1071213648931
      },
      {
        id: 1071213649091,
        type: 'Notification',
        via: {
          channel: 'rule',
          source: {
            from: {
              deleted: false,
              title: 'Notify requester and CCs of comment update',
              id: 360149638672
            },
            rel: 'trigger'
          }
        },
        subject: '[{{ticket.account}}] Re: {{ticket.title}}',
        body: 'Your request ({{ticket.id}}) has been updated. To add additional comments, reply to this email.\n{{ticket.comments_formatted}}',
        recipients: [
          398698541951
        ]
      },
      {
        id: 1071213649131,
        type: 'Notification',
        via: {
          channel: 'rule',
          source: {
            from: {
              deleted: false,
              title: 'Notify assignee of comment update',
              id: 360149638692
            },
            rel: 'trigger'
          }
        },
        subject: '[{{ticket.account}}] Re: {{ticket.title}}',
        body: 'This ticket (#{{ticket.id}}) has been updated.\n\n{{ticket.comments_formatted}}',
        recipients: [
          398627745992
        ]
      }
    ],
    via: {
      channel: 'api',
      source: {
        from: {},
        to: {
          name: 'sravani',
          address: 'sravani@backflipt.com'
        },
        rel: null
      }
    }
  }
}

const users = {
  users: [
    {
      id: 398627745992,
      url: 'https://backflipt.zendesk.com/api/v2/users/398627745992.json',
      name: 'Victor',
      email: 'victor@backflipt.com',
      created_at: '2020-06-02T05:09:28Z',
      updated_at: '2020-06-16T13:55:53Z',
      time_zone: 'Arizona',
      iana_time_zone: 'America/Phoenix',
      phone: null,
      shared_phone_number: null,
      photo: null,
      locale_id: 1,
      locale: 'en-US',
      organization_id: null,
      role: 'agent',
      verified: true,
      external_id: null,
      tags: [],
      alias: '',
      active: true,
      shared: false,
      shared_agent: false,
      last_login_at: '2020-06-16T13:55:53Z',
      two_factor_auth_enabled: null,
      signature: '',
      details: '',
      notes: '',
      role_type: null,
      custom_role_id: null,
      moderator: false,
      ticket_restriction: null,
      only_private_comments: false,
      restricted_agent: false,
      suspended: false,
      chat_only: false,
      default_group_id: 360009305072,
      report_csv: false,
      user_fields: {}
    },
    {
      id: 398698541951,
      url: 'https://backflipt.zendesk.com/api/v2/users/398698541951.json',
      name: 'sravani',
      email: 'sravani@backflipt.com',
      created_at: '2020-06-02T05:11:23Z',
      updated_at: '2020-06-02T05:23:13Z',
      time_zone: 'Arizona',
      iana_time_zone: 'America/Phoenix',
      phone: null,
      shared_phone_number: null,
      photo: null,
      locale_id: 1,
      locale: 'en-US',
      organization_id: null,
      role: 'end-user',
      verified: true,
      external_id: null,
      tags: [],
      alias: '',
      active: true,
      shared: false,
      shared_agent: false,
      last_login_at: '2020-06-02T05:23:13Z',
      two_factor_auth_enabled: false,
      signature: null,
      details: '',
      notes: '',
      role_type: null,
      custom_role_id: null,
      moderator: false,
      ticket_restriction: 'requested',
      only_private_comments: false,
      restricted_agent: true,
      suspended: false,
      chat_only: false,
      default_group_id: null,
      report_csv: false,
      user_fields: {}
    }
  ],
  next_page: null,
  previous_page: null,
  count: 2
}

const groups = {
  groups: [
    {
      url: 'https://backflipt.zendesk.com/api/v2/groups/360009305072.json',
      id: 360009305072,
      name: 'Default',
      description: '',
      default: true,
      deleted: false,
      created_at: '2020-05-15T06:24:01Z',
      updated_at: '2020-06-08T11:32:29Z'
    },
    {
      url: 'https://backflipt.zendesk.com/api/v2/groups/360009772931.json',
      id: 360009772931,
      name: 'Finance',
      description: '',
      default: false,
      deleted: false,
      created_at: '2020-06-08T11:32:16Z',
      updated_at: '2020-06-08T11:32:16Z'
    },
    {
      url: 'https://backflipt.zendesk.com/api/v2/groups/360009765612.json',
      id: 360009765612,
      name: 'Sales',
      description: '',
      default: false,
      deleted: false,
      created_at: '2020-06-08T11:32:01Z',
      updated_at: '2020-06-08T11:32:01Z'
    }
  ],
  next_page: null,
  previous_page: null,
  count: 3
}
