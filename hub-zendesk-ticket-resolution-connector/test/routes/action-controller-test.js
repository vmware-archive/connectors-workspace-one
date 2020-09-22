/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { describe } = require('mocha')
const { expect } = require('chai')
const sinon = require('sinon')
const chai = require('chai')
const { addTicketComment, updateTicketStatus } = require('../../routes/actions-controller')
const service = require('../../services/zendesk-ticket-service')
const sinonChai = require('sinon-chai')
chai.use(sinonChai)

describe('addTicketComment', () => {
  it('comment addition should be successful', async () => {
    const mockReq = {
      body: {
        comment: 'Adding new comment',
        ticketId: 6
      }
    }
    let respBody = ''
    let statusCode = ''
    const mockResp = {
      locals: {
        mfJwt: { email: 'text@email.com' }
      },
      json: (jsonIn) => {
        respBody = jsonIn
      },
      status: (sc) => {
        statusCode = sc
        return mockResp
      }
    }
    const addTicketCommentStub = sinon.stub(service, 'addTicketComment').returns(Promise.resolve(addCommentAPIResp))
    await addTicketComment(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    expect(respBody.commentId).to.equal(1071199566271)
    addTicketCommentStub.restore()
  })

  it('comment addition should throw 401 error', async () => {
    const mockReq = {
      body: {
        comment: 'Adding new comment',
        ticketId: 6
      }
    }
    let respBody = ''
    let statusCode = ''
    let backendStatus = ''
    const mockResp = {
      locals: {
        mfJwt: { email: 'text@email.com' }
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
    const addTicketCommentStub = sinon.stub(service, 'addTicketComment').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 401 }))))
    await addTicketComment(mockReq, mockResp)
    expect(statusCode).to.equal(400)
    expect(backendStatus).to.equal(401)
    expect(respBody.method).to.equal('addTicketComment')
    addTicketCommentStub.restore()
  })

  it('comment addition should throw 500 error on non 401 error', async () => {
    const mockReq = {
      body: {
        comment: 'Adding new comment',
        ticketId: 6
      }
    }
    let respBody = ''
    let statusCode = ''
    let backendStatus = ''
    const mockResp = {
      locals: {
        mfJwt: { email: 'text@email.com' }
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
    const addTicketCommentStub = sinon.stub(service, 'addTicketComment').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 422 }))))
    await addTicketComment(mockReq, mockResp)
    expect(statusCode).to.equal(500)
    expect(backendStatus).to.equal(422)
    expect(respBody.method).to.equal('addTicketComment')
    addTicketCommentStub.restore()
  })
})

describe('updateTicketStatus', () => {
  it('ticket status update should be successful', async () => {
    const mockReq = {
      body: {
        actionType: 'pending',
        comment: 'Adding new comment',
        ticketId: 9
      }
    }
    let respBody = ''
    let statusCode = ''
    const mockResp = {
      locals: {
        mfJwt: { email: 'text@email.com' }
      },
      json: (jsonIn) => {
        respBody = jsonIn
      },
      status: (sc) => {
        statusCode = sc
        return mockResp
      }
    }
    const updateTicketStatusStub = sinon.stub(service, 'updateTicketStatus').returns(Promise.resolve(ticketStatusChangeResp))
    await updateTicketStatus(mockReq, mockResp)
    expect(typeof respBody).to.equal('object')
    expect(statusCode).to.equal(200)
    expect(respBody.tiketId).to.equal(9)
    updateTicketStatusStub.restore()
  })

  it('ticket status update should throw 401 error', async () => {
    const mockReq = {
      body: {
        actionType: 'closed',
        comment: 'Adding new comment',
        ticketId: 6
      }
    }
    let respBody = ''
    let statusCode = ''
    let backendStatus = ''
    const mockResp = {
      locals: {
        authContext: { eml: 'text@email.com' }
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
    const updateTicketStatusStub = sinon.stub(service, 'updateTicketStatus').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 401 }))))
    await updateTicketStatus(mockReq, mockResp)
    expect(statusCode).to.equal(400)
    expect(backendStatus).to.equal(401)
    expect(respBody.method).to.equal('updateTicketStatus')
    updateTicketStatusStub.restore()
  })

  it('ticket status update should throw 500 error on non 401 error', async () => {
    const mockReq = {
      body: {
        actionType: 'closed',
        comment: 'Adding new comment',
        ticketId: 6
      },
      locals: {
        authContext: { eml: 'text@email.com' }
      }
    }
    let respBody = ''
    let statusCode = ''
    let backendStatus = ''
    const mockResp = {
      locals: {
        authContext: { eml: 'text@email.com' }
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
    const updateTicketStatusStub = sinon.stub(service, 'updateTicketStatus').returns(Promise.reject(new Error(JSON.stringify({ statusCode: 422 }))))
    await updateTicketStatus(mockReq, mockResp)
    expect(statusCode).to.equal(500)
    expect(backendStatus).to.equal(422)
    expect(respBody.method).to.equal('updateTicketStatus')
    updateTicketStatusStub.restore()
  })
})

const addCommentAPIResp = {
  ticket: {
    url: 'https://backflipt.zendesk.com/api/v2/tickets/6.json',
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
    updated_at: '2020-06-02T07:03:34Z',
    type: null,
    subject: 'Printer not working',
    raw_subject: 'Printer not working',
    description: 'Printer not working for last one week',
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
    allow_attachments: true
  },
  audit: {
    id: 1071199566151,
    ticket_id: 6,
    created_at: '2020-06-02T07:03:34Z',
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
        id: 1071199566271,
        type: 'Comment',
        author_id: 397911617552,
        body: 'We are working on this as a prioirty',
        html_body: '<div class="zd-comment" dir="auto"><p dir="auto">We are working on this as a prioirty</p></div>',
        plain_body: 'We are working on this as a prioirty.',
        public: true,
        attachments: [],
        audit_id: 1071199566151
      },
      {
        id: 1071199566431,
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
          397911617552
        ]
      }
    ],
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
    }
  }
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
