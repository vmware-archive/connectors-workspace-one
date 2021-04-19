const express = require('express')
const bodyParser = require('body-parser')
const fs = require('fs')
const { v4: uuidv4 } = require('uuid')
const jwt = require('jsonwebtoken')
const mfPubKey = '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0wqsQiPzw5iot83JJWcW\ntxE8j02lOYSXBpDeBwQL2KQAvD2a5TC7iHU3+xFV4bLoZcMVPL6xnYAQnMzQlym/\ncQScpZdG+Chz4u0KbjAN3tkG0k6zDSwSN0RjVvS3O9CByPhYfdYSYDEjwFtlmbZA\nO5yOOvu5jwUP/X6KfR+v5BrytZgnBH+JOoU6gIPRiEzgV19LyNbqOG7k35kikRw7\nocbMTRu5LfPxMI2AHz9ju7YyTyYG20zxKfVsrpNo5VspZcISZ/maFHK8thDWskkC\nQAoLxgGEC7+VnhmqIyLy5F/ge43437VmN7LU9LxbXqrB4nMrZlOOcWqmr1bYM7bL\noQIDAQAB\n-----END PUBLIC KEY-----\n'

/**
 * Create a Mock Hero Server with the appropriate APIs to respond to the connector
 */
const createServer = () => {
  const port = process.env.MOCK_HERO_SERVER_PORT || 10002
  const app = express()
  app.use(bodyParser.json())

  const publicKeyPath = () => {
    return './test/support/files/keys/public.pem'
  }

  app.get('/security/public-key', function (req, res) {
    fs.readFile(publicKeyPath(), 'utf8', function (err, data) {
      if (err) {
        res.status(400).json({ error: err })
      } else {
        res.status(200).send(data)
      }
    })
  })

  app.get('/public-key', (req, res) => {
    res.status(200).send(mfPubKey)
  })

  app.get('/auth/oauthtoken', function (req, res) {
    res.status(200).send(userAuthToken(req))
  })

  /**
 * Retrieve a JWT for use with this mock
 *
 * @param  {} '/SAAS/auth/oauthtoken'
 * @param  {} function(req,res)
 */
  const userAuthToken = (req) => {
    const user = req.body.user || 'genericuser'
    const tenant = req.body.tenant || 'vmware'
    const domain = req.body.domain || 'VMWARE'
    const protocol = req.body.protocol || req.protocol
    const hostname = req.headers.host
    const host = `${protocol}://${hostname}`
    const issuer = `${host}/SAAS/auth`
    const email = req.body.email || `${user}@${domain}`
    const audience = `${host}/auth/oauthtoken`
    const expires = req.body.expires || '7d'

    const payload = {
      jti: uuidv4(),
      prn: `${user}@${tenant}`,
      domain: domain,
      eml: email,
      iss: issuer
    }
    const jwtOptions = {
      algorithm: 'RS256',
      expiresIn: expires,
      audience: audience,
      subject: user
    }

    return jwt.sign(payload, readPrivateKey(), jwtOptions)
  }

  /**
 * TODO:  this should not require access to test data, maybe
 */
  const readPrivateKey = () => {
    return fs.readFileSync('../test/support/files/keys/private.pem')
  }

  const server = app.listen(port)
  console.log(`*** Mock hero is listening (${port}), call close() when finished`)

  app.close = (fn) => {
    console.log('*** Mock hero is shutting down')
    server.close(() => {
      if (fn) {
        fn()
      }
    }
    )
  }

  return app
}

module.exports = {
  createServer
}
