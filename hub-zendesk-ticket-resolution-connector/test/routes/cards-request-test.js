/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'


const { describe } = require('mocha')
const { expect } = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const R = require('ramda')
const service = require('../../services/zendesk-ticket-service')
const { cardsController } = require('../../routes/cards-request')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('cards-controller', () => {
  it('it should generate cards', async () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
      }
    }
    let respBody = ''
    let statusCode = ''
    const mockResp = {
      locals: {
        mfRoutingPrefix: 'https://mf-server/conn123/card/',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      },
      json: (jsonIn) => {
        respBody = jsonIn
      },
      status: (sc) => {
        statusCode = sc
        return mockResp
      }
    }
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.resolve(userInfo))
    const getUserTicketsStub = sinon.stub(service, 'getUserTickets').returns(Promise.resolve(userTickets))
    const getTicketsCommentsStub = sinon.stub(service, 'getTicketComments').returns(Promise.resolve(ticketComments))
    const getUsersStub = sinon.stub(service, 'getUsers').returns(Promise.resolve(usersResp))
    const getGroupsStud = sinon.stub(service, 'getGroups').returns(Promise.resolve(groupsResp))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    expect(respBody.objects[0].backend_id).to.equal('c3JpbmkuZ2FyZ2V5YUB2bXdhcmUuY29tLTYtMjAyMC0wNS0yOFQxMjoxNjo0OVo=')
    getUserInfoStub.restore()
    getUserTicketsStub.restore()
    getTicketsCommentsStub.restore()
    getUsersStub.restore()
    getGroupsStud.restore()
  })

  it('it should empty cards', async () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
      }
    }
    let respBody = ''
    let statusCode = ''
    const mockResp = {
      locals: {
        mfRoutingPrefix: 'https://mf-server/conn123/card/',
        mfJwt: { email: 'srini.gargeya@vmware.com' }
      },
      json: (jsonIn) => {
        respBody = jsonIn
      },
      status: (sc) => {
        statusCode = sc
        return mockResp
      }
    }
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.resolve([]))
    const getUserTicketsStub = sinon.stub(service, 'getUserTickets').returns(Promise.resolve([]))
    const getTicketsCommentsStub = sinon.stub(service, 'getTicketComments').returns(Promise.resolve([]))
    const getUsersStub = sinon.stub(service, 'getUsers').returns(Promise.resolve([]))
    const getGroupsStud = sinon.stub(service, 'getGroups').returns(Promise.resolve([]))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    getUserInfoStub.restore()
    getUserTicketsStub.restore()
    getTicketsCommentsStub.restore()
    getUsersStub.restore()
    getGroupsStud.restore()
  })

  it('cards should throw 401 if userInfo api throws 401 error', async () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
      }
    }
    let respBody = ''
    let statusCode = ''
    let backendStatus = ''
    const mockResp = {
      locals: {
        mfRoutingPrefix: 'https://mf-server/conn123/card/'
      },
      json: (jsonIn) => {
        respBody = jsonIn
      },
      status: (sc) => {
        statusCode = sc
        return mockResp
      },
      header: (hdrKey, bckndSts) => {
        backendStatus = bckndSts
        return mockResp
      }
    }
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 401 }))))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(backendStatus).to.equal(401)
    expect(statusCode).to.equal(400)
    getUserInfoStub.restore()
  })

  it('cards should throw 500 if userInfo api throws 5xx error', async () => {
    const mockReq = {
      headers: {
        'x-forwarded-proto': 'https',
        'x-forwarded-host': 'my-host',
        'x-forwarded-port': 3030,
        'x-forwarded-prefix': '/my-path-prefix'
      }
    }
    let respBody = ''
    let statusCode = ''
    let backendStatus = ''
    const mockResp = {
      locals: {
        mfRoutingPrefix: 'https://mf-server/conn123/card/'
      },
      json: (jsonIn) => {
        respBody = jsonIn
      },
      status: (sc) => {
        statusCode = sc
        return mockResp
      },
      header: (hdrKey, bckndSts) => {
        backendStatus = bckndSts
        return mockResp
      }
    }
    const getUserInfoStub = sinon.stub(service, 'getUserInfo').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 501 }))))
    await cardsController(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(backendStatus).to.equal(501)
    expect(statusCode).to.equal(500)
    getUserInfoStub.restore()
  })
})

const userInfo =
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

const userTickets = [
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
    requester_id: 398627745992,
    submitter_id: 398627745992,
    assignee_id: 398627745992,
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
    tags: ['need_approval'],
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
    requester_id: 398627745992,
    submitter_id: 398627745992,
    assignee_id: 398627745992,
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
    requester_id: 123,
    submitter_id: 123,
    assignee_id: 398627745992,
    organization_id: 361067840292,
    group_id: 123,
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
  }
]

const ticketComments = [
  {
    id: 1087627740071,
    ticket_id: 6,
    type: 'Comment',
    author_id: 398627745992,
    body: 'Ticket Resolution 220 Description',
    html_body: '<div class="zd-comment" dir="auto">Ticket Resolution 220 Description<br></div>',
    plain_body: 'Ticket Resolution 220 Description',
    public: true,
    attachments: [
      {
        url: 'https://backflipt.zendesk.com/api/v2/attachments/376927556852.json',
        id: 376927556852,
        file_name: 'Avengers_1234567.docx',
        content_url: 'https://backflipt.zendesk.com/attachments/token/DB8J8fbp3QlRdD1sFfbtohbPB/?name=Avengers_1234567.docx',
        mapped_content_url: 'https://backflipt.zendesk.com/attachments/token/DB8J8fbp3QlRdD1sFfbtohbPB/?name=Avengers_1234567.docx',
        content_type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        size: 93356,
        width: null,
        height: null,
        inline: false,
        deleted: false,
        thumbnails: []
      }
    ],
    audit_id: 1087627739991,
    via: {
      channel: 'web',
      source: {
        from: {},
        to: {
          name: 'sravani',
          address: 'sravani@backflipt.com'
        },
        rel: null
      }
    },
    created_at: '2020-06-15T08:59:27Z',
    metadata: {
      system: {
        client: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36',
        ip_address: '103.232.128.15',
        location: 'Hyderabad, TG, India',
        latitude: 17.3841,
        longitude: 78.4564
      },
      custom: {}
    }
  },
  {
    id: 1087627740071,
    type: 'Comment',
    author_id: 398627745992,
    body: 'Ticket Resolution 220 Description',
    html_body: '<div class="zd-comment" dir="auto">Ticket Resolution 220 Description<br></div>',
    plain_body: 'Ticket Resolution 220 Description',
    public: true,
    attachments: [
      {
        url: 'https://backflipt.zendesk.com/api/v2/attachments/376927556852.json',
        id: 376927556852,
        file_name: 'Avengers_1234567.docx',
        content_url: 'https://backflipt.zendesk.com/attachments/token/DB8J8fbp3QlRdD1sFfbtohbPB/?name=Avengers_1234567.docx',
        mapped_content_url: 'https://backflipt.zendesk.com/attachments/token/DB8J8fbp3QlRdD1sFfbtohbPB/?name=Avengers_1234567.docx',
        content_type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        size: 93356,
        width: null,
        height: null,
        inline: false,
        deleted: false,
        thumbnails: []
      }
    ],
    audit_id: 1087627739991,
    via: {
      channel: 'web',
      source: {
        from: {},
        to: {
          name: 'sravani',
          address: 'sravani@backflipt.com'
        },
        rel: null
      }
    },
    created_at: '2020-06-15T08:59:27Z',
    metadata: {
      system: {
        client: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36',
        ip_address: '103.232.128.15',
        location: 'Hyderabad, TG, India',
        latitude: 17.3841,
        longitude: 78.4564
      },
      custom: {}
    }
  }
]

const usersResp = [
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
  },
  {
    id: 123,
    url: 'https://backflipt.zendesk.com/api/v2/users/398698541951.json',
    name: 'sravani',
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
]

const groupsResp = [
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
    url: 'https://backflipt.zendesk.com/api/v2/groups/360009305072.json',
    id: 123,
    description: '',
    default: true,
    deleted: false,
    created_at: '2020-05-15T06:24:01Z',
    updated_at: '2020-06-08T11:32:29Z'
  }
]
