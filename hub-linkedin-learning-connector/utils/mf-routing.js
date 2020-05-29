'use strict'

exports.addContextPath = function (req, res, next) {
  const xRoutingPrefix = req.headers['x-routing-prefix']
  const xRoutingTemplate = req.headers['x-routing-template']

  const xForwardedPrefix = req.headers['x-forwarded-prefix']

  let contextPath
  if (xForwardedPrefix) {
    contextPath = xForwardedPrefix.substring(1) + '/'
  } else {
    contextPath = ''
  }

  if (xRoutingPrefix) {
    // Eg: Results either https://mf-server/conn123/card/ or https://mf-server/conn123/card/bot-connectors/
    res.locals.mfRoutingPrefix = xRoutingPrefix + contextPath
  }

  if (xRoutingTemplate) {
    res.locals.mfRoutingTemplate = xRoutingTemplate + contextPath
  }
  next()
}
