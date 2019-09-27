# ServiceNow Connector (NodeJS)

## Overview

TODO

## Running

### Docker

First, add the MF_PUB_URL param to a .env file.

ex.

```
# .env
MF_PUB_KEY_URL=https://dev.hero.vmwservices.com/security/public-key
```

Then you can use docker-compose to start the connector:

``` shell
docker-compose up --build
```

### NPM

You can just run as a nodejs process instead of docker with npm:

```
export MF_PUB_KEY_URL=https://dev.hero.vmwservices.com/security/public-key
npm start
```

## Further reading

For information on card connectors, see the specification [wiki](https://github.com/vmwaresamples/card-connectors-guide/wiki).
