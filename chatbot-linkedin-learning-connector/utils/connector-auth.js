'use strict'

const rp = require('request-promise')
const jwt = require('jsonwebtoken')
const { log, logReq } = require('./log')
const pubKeyUrl = process.env.MF_JWT_PUB_KEY_URI

let pubKeyCache

function testConfig () {
  if (!pubKeyUrl) {
    throw Error('Please provide MobileFlows public key URL at MF_JWT_PUB_KEY_URI')
  }
}

const validate = async (req, res, next) => {
  try {
    const { mfTenant, username, email } = await verifyMfJwt(req)

    res.locals.mfTenant = mfTenant
    res.locals.username = username
    res.locals.email = email
  } catch (error) {
    logReq(res, 'validate error' + error)
    return res.status(401).json({ message: error.message })
  }

  next()
}

const verifyMfJwt = async (req) => {
  const mfPublicKey = await getPublicKey()

  return new Promise((resolve, reject) => {
    const jwtOptions = {
      algorithms: ['RS256'],
      clockTolerance: 60,
      clockTimestamp: Date.now() / 1000
    }
    const auth = (req.headers.authorization || '').replace('Bearer ', '').trim()

    jwt.verify(auth, mfPublicKey, jwtOptions, (err, decoded) => {
      if (err) {
        reject(err)
      } else {
        const index = decoded.prn.lastIndexOf('@')
        const username = decoded.prn.substring(0, index)
        resolve({
          mfTenant: decoded.tenant,
          username: username,
          email: decoded.eml,
          idmDomain: decoded.domain
        })
      }
    })
  })
}

const getPublicKey = async () => {
  if (pubKeyCache && pubKeyCache.expiresAtTime > Date.now()) {
    return pubKeyCache.value
  }

  const key = await rp(pubKeyUrl)
  const expiresAtTime = Date.now() + 3600000

  log(
    'Updating pub key cache for url: %s, set to expire around: %s',
    pubKeyUrl,
    new Date(expiresAtTime)
  )

  pubKeyCache = {
    expiresAtTime: expiresAtTime,
    value: key
  }

  return key
}

module.exports = {
  testConfig,
  validate
}
