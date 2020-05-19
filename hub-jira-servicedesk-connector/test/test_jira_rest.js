const rp = require('request-promise-native')
const { expect } = require('chai')
const jwt = require('jsonwebtoken')

const mfPrivKey = '-----BEGIN RSA PRIVATE KEY-----\nMIIEpQIBAAKCAQEA0wqsQiPzw5iot83JJWcWtxE8j02lOYSXBpDeBwQL2KQAvD2a\n5TC7iHU3+xFV4bLoZcMVPL6xnYAQnMzQlym/cQScpZdG+Chz4u0KbjAN3tkG0k6z\nDSwSN0RjVvS3O9CByPhYfdYSYDEjwFtlmbZAO5yOOvu5jwUP/X6KfR+v5BrytZgn\nBH+JOoU6gIPRiEzgV19LyNbqOG7k35kikRw7ocbMTRu5LfPxMI2AHz9ju7YyTyYG\n20zxKfVsrpNo5VspZcISZ/maFHK8thDWskkCQAoLxgGEC7+VnhmqIyLy5F/ge434\n37VmN7LU9LxbXqrB4nMrZlOOcWqmr1bYM7bLoQIDAQABAoIBAQCQNBHCW+ibtTtL\n5LxV51v5GTkFPmvwom3D2ccsihJCJMYv2fR2ONdbhaUL1CuXvfTYW/Wt/StGUJSJ\nX9YEBE3AvwL+jyC6PoH5BDmFUyaXKDpmB8qG7J9BzmQGrc5qe63DEhb9XQJPYiRo\nssr4vjSjxvTUzt5bIH1tnEKq/rTkKluLkvaRpnulMe1XB1bUkazd9MlJBfZlFz7b\nzHjDeK/czQ3LfHOrvqP8MEln7K5aPbQkpoABGKAKOlvXKbI6NJdaHdYyG2FSz8tx\n9upIX4i7twUzTHOu7e0aCk1kLNdqC+1uVZmb5UEQLaO9g1loS2ZzD6ADPNeEzxIQ\nBEnTJryFAoGBAPaVG/Qu8W99P6M14QUBywAOtyxSi0CtyxuoNfr4FWRbmsJWk8YY\n+SREp61IMD+P9e8vxVKiz9tT0OtO1Q0Go0ydNUm4RCw4xVFZSWAnIad5VOtKldtA\nwV4rdH9G0YFmqg3KP7ObmBSeV37D76SV/iap6QY4sm2tPLGNthuke2ArAoGBANsa\nEuVdEcQgswXdSd1oSqfp12MksCdn8+3JMKxECh7SWKMlF52DWyygOtVZN3oQnoOm\nmN3unRqTlKZRb/OzXpNe/tp3NQj63fs7A6K/zkDa70Wjjz73/0kSmK90/L29vbzp\nEnOEY2ZvGb5diEv/bhQTP8U5Wu8UJDkETJTvOVFjAoGBAPEsB8o9e7DCvNJB6VL/\nXPAydF+qYD6jfOsRC7Lqn+mnWudGzIPNeyhI6gMmfuI8SJtnisR3L3tiMA1l7iUu\nX9uYSz1ON4dVA1C8VnLv8w+dMTxsl8N5Q2d6cxflSRYaNqsELGfb/9PyxrraovHE\nLm7ccmi+XW2+KYWzh/DjYDQ/AoGBAMOW2xd1pc53gljR2oaT+1E6JtSSg84ptk+n\nMpQViRNKo2XATvyFrnZ/8wVRx3xoKZlMt1onEIgRBroSKOZcUSktvEQ59lY13MPR\nQsWeg/jReJeqEs4bhQEuYK8AuD6Jiz+AsL/+ht2CgHC3/lwZgaLaLCtbsBmM2Wks\ntVCe3YQRAoGALPKhVfd0E0jYpHYhV4cEFmceIIyebsrj4nUh794e/3s/N2vR4ySw\n6Y9XWT/330bh1C52O9W+UHXefkoPak89YhvVm0T+stPYOyIZF8xQagx8eFU/KPUB\n/0BsmYJy3InLDMS9MRCR7NiApZabOsWRRdYR9rXsm3dpUWffIyHYhW8=\n-----END RSA PRIVATE KEY-----\n'
const createMockJiraServer = require('./support/mock_jira_server').createServer
const createMockHeroServer = require('./support/mock_hero_server').createServer

const baseUrl = 'http://localhost:9191'

let mockJira, mockHero

const mfToken = jwt.sign(
  {
    prn: 'jdoe@tenantId',
    eml: 'jdoe@vmware.com',
    tenant: 'tenantId',
    domain: 'vmware.com'
  },
  mfPrivKey,
  {
    algorithm: 'RS256',
    expiresIn: '300 seconds',
    subject: 'todo-user-id',
    audience: 'todo-this-connector',
    issuer: 'todo-mf-server'
  }
)

const callHealth = () => {
  const options = {
    method: 'GET',
    uri: `${baseUrl}/health`,
    resolveWithFullResponse: true,
    json: true
  }
  return rp(options)
}

const callDiscovery = () => {
  const options = {
    method: 'GET',
    uri: `${baseUrl}/`,
    resolveWithFullResponse: true,
    json: true,
    qs: {},
    headers: {
      'X-Forwarded-Proto': 'https',
      'X-Forwarded-Prefix': '/dev',
      'X-Forwarded-Host': 'test-host',
      'X-Forwarded-Port': '3001'
    }
  }
  return rp(options)
}

const callImage = () => {
  const options = {
    method: 'GET',
    uri: `${baseUrl}/images/connector.png`,
    resolveWithFullResponse: true,
    json: false,
    qs: {},
    headers: {}
  }
  return rp(options)
}

const callListServiceDesk = () => {
  const options = {
    method: 'POST',
    uri: `${baseUrl}/listServiceDesks`,
    resolveWithFullResponse: true,
    json: true,
    qs: {},
    headers: {
      Authorization: `Bearer ${mfToken}`,
      'X-Connector-Base-Url': '',
      'X-Forwarded-Proto': 'https',
      'X-Forwarded-Prefix': '/dev',
      'X-Forwarded-Host': 'test-host',
      'X-Forwarded-Port': '3001'
    }
  }
  return rp(options)
}

const callListRequestType = () => {
  const options = {
    method: 'POST',
    uri: `${baseUrl}/listRequestTypes`,
    resolveWithFullResponse: true,
    json: true,
    qs: {},
    headers: {
      Authorization: `Bearer ${mfToken}`,
      'X-Connector-Base-Url': '',
      'X-Forwarded-Proto': 'https',
      'X-Forwarded-Prefix': '/dev',
      'X-Forwarded-Host': 'test-host',
      'X-Forwarded-Port': '3001'
    }
  }
  return rp(options)
}

const handleApprovalAction = (issueKey, decision, comment) => {
  const options = {
    method: 'POST',
    uri: `${baseUrl}/approvalAction`,
    resolveWithFullResponse: true,
    json: true,
    qs: {},
    form: {
      issueKey: issueKey,
      decision: decision,
      comment: comment
    },
    headers: {
      Authorization: `Bearer ${mfToken}`,
      'X-Connector-Base-Url': '',
      'X-Forwarded-Proto': 'https',
      'X-Forwarded-Prefix': '/dev',
      'X-Forwarded-Host': 'test-host',
      'X-Forwarded-Port': '3001'
    }
  }
  return rp(options)
}

const callCards = () => {
  const options = {
    method: 'POST',
    uri: `${baseUrl}/cards`,
    resolveWithFullResponse: true,
    json: true,
    qs: {},
    body: {},
    headers: {
      Authorization: `Bearer ${mfToken}`,
      'X-Connector-Base-Url': '',
      'X-Forwarded-Proto': 'https',
      'X-Forwarded-Prefix': '/dev',
      'X-Forwarded-Host': 'test-host',
      'X-Forwarded-Port': '3001',
      'X-Routing-Prefix': `${baseUrl}/connectors/jiraSD/card/`
    }
  }
  return rp(options)
}

const createCustomerRequest = (summary, description) => {
  const options = {
    method: 'POST',
    uri: `${baseUrl}/createCustomerRequest`,
    resolveWithFullResponse: true,
    json: true,
    qs: {},
    form: {
      summary: summary,
      description: description
    },
    headers: {
      Authorization: `Bearer ${mfToken}`,
      'X-Connector-Base-Url': '',
      'X-Forwarded-Proto': 'https',
      'X-Forwarded-Prefix': '/dev',
      'X-Forwarded-Host': 'test-host',
      'X-Forwarded-Port': '3001'
    }
  }
  return rp(options)
}

describe('jira_rest tests:', () => {
  let server
  let auth

  require('dotenv').config()

  before(() => {
    if (!server) {
      mockHero = createMockHeroServer()
      mockJira = createMockJiraServer()
      try {
        process.env.PORT = '9191'
        process.env.NODE_ENV = 'test'
        process.env.SERVICEDESK_REQUEST_API = 'http://localhost:10001'
        process.env.ATLASSIAN_API_SERVER = 'http://localhost:10001'
        process.env.MF_JWT_PUB_KEY_URI = 'http://localhost:10002/public-key'

        delete require.cache[require.resolve('../routes/auth')]
        auth = require('../routes/auth')

        delete require.cache[require.resolve('../index')]
        server = require('../index')

      } catch (error) {
        console.log('Error =>', error)
      }
    }
  })

  after(done => {
    mockJira.close(() => {
      mockHero.close(() => {
        if (server) {
          server.close(done)
        } else {
          done()
        }
      })
    })
  })

  describe('auth tests:', function () {
    it('publicKeyURL should return the URL from the env', function () {
      const url = auth.publicKeyURL()
      expect(url).to.eql(process.env.MF_JWT_PUB_KEY_URI)
    })

    it('getPublicKey should return the public key', async function () {
      const key = await auth.test.getPublicKey({ authPubKeyUrl: auth.publicKeyURL() })
      expect(key.startsWith('-----BEGIN PUBLIC KEY-----'), 'Expected a public key but did not get one')
    })
  })

  describe('Health', () => {
    it('should have a health endpoint', async () => {
      try {
        const resp = await callHealth()
        expect(resp.statusCode).to.eql(200)
        expect(resp.body.status).to.eql('UP')
      } catch (err) {
        expect.fail(err)
      }
    })
  })

  describe('Discovery', () => {
    it('should return correct meta data', async () => {
      try {
        const resp = await callDiscovery()
        expect(resp.statusCode).to.eql(200)
        expect(resp.body).to.eql(
          {
            image: {
              href: 'https://s3.amazonaws.com/vmw-mf-assets/connector-images/jira-service-desk.png'
            },
            object_types: {
              card: {
                pollable: true,
                doc: {
                  href: 'https://vmwaresamples.github.io/card-connectors-guide/#schema/herocard-response-schema.json'
                },
                endpoint: {
                  href: 'https://test-host:3001/dev/cards'
                }
              }
            }
          }
        )
      } catch (err) {
        expect.fail(err)
      }
    })
  })

  describe('Image', () => {
    it('should return success', async () => {
      const resp = await callImage()
      expect(resp.statusCode).to.eql(200)
    })
  })

  describe('Service Desk', () => {
    it('should return the correct json', async () => {
      try {
        const resp = await callListServiceDesk()
        expect(resp.statusCode).to.eql(200)
        expect(resp.body).to.eql(
          [
            {
              id: '1',
              projectId: '10000',
              projectName: 'First service desk project',
              projectKey: 'FSDP'
            }
          ]
        )
      } catch (err) {
        expect.fail(err)
      }
    })
  })

  describe('Request Type', () => {
    it('should return the list of all request type for service desk id 1', async () => {
      const resp = await callListRequestType()
      expect(resp.statusCode).to.eql(200)
      expect(resp.body).to.eql(
        [
          {
            id: '1',
            name: 'Get IT help',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '2',
            name: 'Fix an account problem',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '3',
            name: 'Get a guest wifi account',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '4',
            name: 'Set up VPN to the office',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '5',
            name: 'Request admin access',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '6',
            name: 'Request a new account',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '7',
            name: 'Onboard new employees',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '8',
            name: 'Desktop/Laptop support',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '9',
            name: 'Set up a phone line redirect',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '10',
            name: 'Request new software',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '11',
            name: 'Request new hardware',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '12',
            name: 'Request a desk phone',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '13',
            name: 'Emailed request',
            issueTypeId: '10101',
            serviceDeskId: '1'
          },
          {
            id: '14',
            name: 'New mobile device',
            issueTypeId: '10102',
            serviceDeskId: '1'
          },
          {
            id: '15',
            name: 'Report a system problem',
            issueTypeId: '10100',
            serviceDeskId: '1'
          },
          {
            id: '16',
            name: 'Upgrade or change a server',
            issueTypeId: '10103',
            serviceDeskId: '1'
          },
          {
            id: '17',
            name: 'Upgrade or change a managed system',
            issueTypeId: '10103',
            serviceDeskId: '1'
          }
        ]
      )
    })
  })

  describe('Customer Request', () => {
    it('Should create a customer request with issueID and issuekey', async () => {
      const resp = await createCustomerRequest('creating test summary', 'test description')
      expect(resp.statusCode).to.eql(200)
      expect(resp.body).to.eql({
        issueId: '10082',
        issueKey: 'FSDP-79'
      })
    })
  })

  describe('Approval', () => {
    it('check for approval with decision as true ', async () => {
      const resp = await handleApprovalAction(1, true)
      expect(resp.statusCode).to.eql(200)
      expect(resp.body).to.eql({
        status: 'true'
      })
    })

    it('check for approval with decision as false', async () => {
      const resp = await handleApprovalAction(1, false)
      expect(resp.statusCode).to.eql(200)
      expect(resp.body).to.eql({
        status: 'false'
      })
    })
  })

  describe('Card Request', () => {
    it('should return the expected card', async () => {
      const resp = await callCards()
      expect(resp.statusCode).to.eql(200)

      resp.body.objects[0].id = 'replaced'
      resp.body.objects[0].hash = 'replaced'
      resp.body.objects[1].id = 'replaced'
      resp.body.objects[1].hash = 'replaced'

      const card1Actions = resp.body.objects[0].actions
      delete resp.body.objects[0].actions
      const card2Actions = resp.body.objects[1].actions
      delete resp.body.objects[1].actions

      expect(card1Actions[0].label).to.eql('Create Request')
      expect(card2Actions[0].label).to.eql('Approve')
      expect(card2Actions[1].label).to.eql('Decline')

      expect(resp.body).to.eql(
        {
          objects: [
            {
              image: {
                href: 'https://s3.amazonaws.com/vmw-mf-assets/connector-images/jira-service-desk.png'
              },
              body: {
                fields: [

                ],
                description: 'Submit a Request'
              },
              id: 'replaced',
              backend_id: 'create_card',
              hash: 'replaced',
              header: {
                title: 'Create Customer Request'
              }
            },
            {
              image: {
                href: 'https://s3.amazonaws.com/vmw-mf-assets/connector-images/jira-service-desk.png'
              },
              body: {
                fields: [
                  {
                    type: 'GENERAL',
                    title: 'Description',
                    description: 'DETAILS_AND_JUSTIFICATION_HERE'
                  },
                  {
                    type: 'GENERAL',
                    title: 'Reporter',
                    description: 'David Customer'
                  },
                  {
                    type: 'GENERAL',
                    title: 'Request Type',
                    description: 'New mobile device'
                  },
                  {
                    type: 'GENERAL',
                    title: 'Date Created',
                    description: 'Today 3:05 PM'
                  },
                  {
                    type: 'GENERAL',
                    title: 'Status',
                    description: 'Waiting for approval'
                  }
                ]
              },
              id: 'replaced',
              backend_id: 'FSDP-68',
              hash: 'replaced',
              header: {
                title: 'REQUEST_SUMMARY_HERE',
                subtitle: [
                  'FSDP-68'
                ],
                subtitle_hl: [
                  {
                    name: 'FSDP-68',
                    href: 'https://mobileflows.atlassian.net/servicedesk/customer/portal/1/FSDP-68'
                  }
                ]
              }
            }
          ]
        }
      )
    })
  })
})
