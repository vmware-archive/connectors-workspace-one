'use strict'

const jwt = require('jsonwebtoken')

const privateKey = '-----BEGIN RSA PRIVATE KEY-----\nMIIEpQIBAAKCAQEA0wqsQiPzw5iot83JJWcWtxE8j02lOYSXBpDeBwQL2KQAvD2a\n5TC7iHU3+xFV4bLoZcMVPL6xnYAQnMzQlym/cQScpZdG+Chz4u0KbjAN3tkG0k6z\nDSwSN0RjVvS3O9CByPhYfdYSYDEjwFtlmbZAO5yOOvu5jwUP/X6KfR+v5BrytZgn\nBH+JOoU6gIPRiEzgV19LyNbqOG7k35kikRw7ocbMTRu5LfPxMI2AHz9ju7YyTyYG\n20zxKfVsrpNo5VspZcISZ/maFHK8thDWskkCQAoLxgGEC7+VnhmqIyLy5F/ge434\n37VmN7LU9LxbXqrB4nMrZlOOcWqmr1bYM7bLoQIDAQABAoIBAQCQNBHCW+ibtTtL\n5LxV51v5GTkFPmvwom3D2ccsihJCJMYv2fR2ONdbhaUL1CuXvfTYW/Wt/StGUJSJ\nX9YEBE3AvwL+jyC6PoH5BDmFUyaXKDpmB8qG7J9BzmQGrc5qe63DEhb9XQJPYiRo\nssr4vjSjxvTUzt5bIH1tnEKq/rTkKluLkvaRpnulMe1XB1bUkazd9MlJBfZlFz7b\nzHjDeK/czQ3LfHOrvqP8MEln7K5aPbQkpoABGKAKOlvXKbI6NJdaHdYyG2FSz8tx\n9upIX4i7twUzTHOu7e0aCk1kLNdqC+1uVZmb5UEQLaO9g1loS2ZzD6ADPNeEzxIQ\nBEnTJryFAoGBAPaVG/Qu8W99P6M14QUBywAOtyxSi0CtyxuoNfr4FWRbmsJWk8YY\n+SREp61IMD+P9e8vxVKiz9tT0OtO1Q0Go0ydNUm4RCw4xVFZSWAnIad5VOtKldtA\nwV4rdH9G0YFmqg3KP7ObmBSeV37D76SV/iap6QY4sm2tPLGNthuke2ArAoGBANsa\nEuVdEcQgswXdSd1oSqfp12MksCdn8+3JMKxECh7SWKMlF52DWyygOtVZN3oQnoOm\nmN3unRqTlKZRb/OzXpNe/tp3NQj63fs7A6K/zkDa70Wjjz73/0kSmK90/L29vbzp\nEnOEY2ZvGb5diEv/bhQTP8U5Wu8UJDkETJTvOVFjAoGBAPEsB8o9e7DCvNJB6VL/\nXPAydF+qYD6jfOsRC7Lqn+mnWudGzIPNeyhI6gMmfuI8SJtnisR3L3tiMA1l7iUu\nX9uYSz1ON4dVA1C8VnLv8w+dMTxsl8N5Q2d6cxflSRYaNqsELGfb/9PyxrraovHE\nLm7ccmi+XW2+KYWzh/DjYDQ/AoGBAMOW2xd1pc53gljR2oaT+1E6JtSSg84ptk+n\nMpQViRNKo2XATvyFrnZ/8wVRx3xoKZlMt1onEIgRBroSKOZcUSktvEQ59lY13MPR\nQsWeg/jReJeqEs4bhQEuYK8AuD6Jiz+AsL/+ht2CgHC3/lwZgaLaLCtbsBmM2Wks\ntVCe3YQRAoGALPKhVfd0E0jYpHYhV4cEFmceIIyebsrj4nUh794e/3s/N2vR4ySw\n6Y9XWT/330bh1C52O9W+UHXefkoPak89YhvVm0T+stPYOyIZF8xQagx8eFU/KPUB\n/0BsmYJy3InLDMS9MRCR7NiApZabOsWRRdYR9rXsm3dpUWffIyHYhW8=\n-----END RSA PRIVATE KEY-----\n'
const publicKey = '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0wqsQiPzw5iot83JJWcW\ntxE8j02lOYSXBpDeBwQL2KQAvD2a5TC7iHU3+xFV4bLoZcMVPL6xnYAQnMzQlym/\ncQScpZdG+Chz4u0KbjAN3tkG0k6zDSwSN0RjVvS3O9CByPhYfdYSYDEjwFtlmbZA\nO5yOOvu5jwUP/X6KfR+v5BrytZgnBH+JOoU6gIPRiEzgV19LyNbqOG7k35kikRw7\nocbMTRu5LfPxMI2AHz9ju7YyTyYG20zxKfVsrpNo5VspZcISZ/maFHK8thDWskkC\nQAoLxgGEC7+VnhmqIyLy5F/ge43437VmN7LU9LxbXqrB4nMrZlOOcWqmr1bYM7bL\noQIDAQAB\n-----END PUBLIC KEY-----\n'

let fakeMf

const getMfTokenFor = (username, audUrl) => {
  return jwt.sign(
    {
      prn: `${username}@tenantId`,
      eml: `${username}@vmware.com`,
      tenant: 'tenantId',
      domain: 'vmware.com'
    },
    privateKey,
    {
      algorithm: 'RS256',
      expiresIn: '300 seconds',
      subject: 'todo-user-id',
      audience: audUrl,
      issuer: 'todo-mf-server'
    }
  )
}

const start = () => {
  const express = require('express')
  const app = express()
  app.get('/public-key', (req, res) => res.send(publicKey))
  fakeMf = app.listen(5000, () => console.log('Fake mobile flows server listening on 5000'))
}

const stop = (fn) => {
  fakeMf.close(() => {
    fn()
  })
}

module.exports = {
  start,
  stop,
  getMfTokenFor
}
