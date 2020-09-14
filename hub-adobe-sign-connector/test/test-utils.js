/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const addXForwardedHeaders = (fetchOptions) => {
    if (!fetchOptions.headers) {
        fetchOptions.headers = {}
    }
    fetchOptions.headers['X-Forwarded-Proto'] = 'https'
    fetchOptions.headers['X-Forwarded-Prefix'] = '/abc'
    fetchOptions.headers['X-Forwarded-Host'] = 'my-host'
    fetchOptions.headers['X-Forwarded-Port'] = '3030'
}

const replaceCardsUuid = (cardsResponse) => {
    cardsResponse.objects.forEach(card => {
        card.id = '00000000-0000-0000-0000-000000000000'
        card.actions.forEach(action => {
            action.id = '00000000-0000-0000-0000-000000000000'
        })
    })
}

module.exports = {
    addXForwardedHeaders,
    replaceCardsUuid
}
