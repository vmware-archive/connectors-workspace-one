'use strict'

const log = (fmt, ...args) => {
  if (!process.env.SQUELCH_LOGS) {
    console.log('[HZRC] ' + fmt, ...args)
  }
}

const logReq = (res, fmt, ...args) => {
  const xReqId = res.locals.xReqId
  const xReqIdFmt = xReqId ? `[req: ${xReqId}] ` : ''
  const xBaseUrl = res.locals.baseUrl
  const xBaseUrlFmt = xBaseUrl ? `[base: ${xBaseUrl}] ` : ''
  const tenant = res.locals.mfTenant
  const tenantFmt = tenant ? `[t: ${tenant}] ` : ''
  const username = res.locals.username
  const usernameFmt = username ? `[u: ${username}] ` : ''
  const domain = res.locals.domain
  const domainFmt = domain ? `[d: ${domain}] ` : ''
  log(tenantFmt + usernameFmt + domainFmt + xReqIdFmt + xBaseUrlFmt + fmt, ...args)
}

module.exports = {
  log,
  logReq
}
