
'use strict'

const jwt = require('jsonwebtoken')
const rp = require('request-promise-native')
const { log, logReq } = require('./log')

const publicKeyUrl = process.env.MF_JWT_PUB_KEY_URI

const testConfig  = ()  => {
  if (!publicKeyUrl) {
    throw Error('Please provide Mobile Flows public key URL at MF_JWT_PUB_KEY_URI')
  }
}

let publicKeyCache

const validate = async (req, res, next) => {
  const authorization = res.locals.authorization

  if (!authorization || !authorization.replace(/^Bearer/, '').trim()) {
    const msg = 'Missing authorization header';
    logReq(res, msg)
    return res.status(401).send({ message: msg  })
  }

  try {
      const decoded = await verifyAuthAsync(authorization)
      const index = decoded.prn.lastIndexOf('@')
      const username = decoded.prn.substring(0, index)
      const { eml, tenant, domain } = decoded;

      res.locals.username = username
      res.locals.email = eml
      res.locals.mfTenant = tenant
      res.locals.domain = domain
      res.locals.jwt = decoded

      next()
    } catch (error) {
      logReq(res, error.message)
      res.status(401).json({
        message: `Identity verification failed! ${error}`
      })
    }

}

const verifyAuthAsync = async (authorization, options) => {
  try {
    const publicKeyContent = await getPublicKey()
    const jwtOptions = {
      algorithms: [
        'RS256',
        'RS384',
        'RS512',
        'ES256',
        'ES384',
        'ES512'
      ],
      clockTolerance: 60,
      clockTimestamp: Date.now() / 1000
    }
    const auth = authorization.replace('Bearer ', '').trim()
    return  jwt.verify(auth, publicKeyContent, jwtOptions)
  } catch (error) {
    throw new Error(error.message)
  }
}

const getPublicKey = async () => {
  if (publicKeyCache && publicKeyCache.expiresAtTime > Date.now()) {
    return publicKeyCache.value
  }

  try {
    const data = await rp(publicKeyUrl)
    const expiresAtTime = Date.now() + 3600000

    log(
      'Updating public key cache for url: %s, set to expire around: %s',
      publicKeyUrl,
      new Date(expiresAtTime)
    )

    publicKeyCache = {
      expiresAtTime: expiresAtTime,
      value: data
    }

    return data
  } catch (error) {
    return new Error(`Failed to retrieve public key: ${error.statusCode}`)
  }
}

module.exports = {
  testConfig,
  validate
}
