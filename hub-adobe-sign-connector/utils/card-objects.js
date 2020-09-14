/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const agreementModule = require('../services/agreement')
const { v4: uuidv4 } = require('uuid')
const sha1 = require('sha1')

async function agreementToCards(res, agreementArray, routingPrefix) {
    const agreementData = await Promise.all(agreementArray.map(agreement => fetchAgreementCardData(res, agreement, routingPrefix)))
    return agreementData.map(agreementDataToCard)
}

async function fetchAgreementCardData(res, agreement, routingPrefix) {
    const [agreementDetail, signingUrl, username] = await Promise.all([
        agreementModule.getAgreementDetail(res, agreement.id),
        agreementModule.getSigningUrl(res, agreement.id),
        agreementModule.getMember(res, agreement.id)
        ])
    return {agreementDetail, signingUrl, username, routingPrefix}
}

function agreementDataToCard ({agreementDetail, signingUrl, username, routingPrefix}) {
    const hash = sha1(`${agreementDetail.name}:${agreementDetail.message}:${agreementDetail.senderEmail}:${username}:${agreementDetail.expirationTime}`)
    let card
    card =  {
        id : uuidv4(),
        creation_date : agreementDetail.createdDate,
        hash: hash,
        header : {
            title : 'Adobe Sign - Signature requested',
        },
        body : {
            fields :[
                {
                    type : 'GENERAL',
                    title: 'Document Name',
                    description : agreementDetail.name
                },
                {
                    type : 'GENERAL',
                    title: 'Message',
                    description : agreementDetail.message
                },
                {
                    type : 'GENERAL',
                    title: 'Requester Name',
                    description : username.senderInfo.name
                },
                {
                    type: 'GENERAL',
                    title: 'Requester Email',
                    description: agreementDetail.senderEmail
                },
                {
                    type : 'SECTION',
                    title : 'ATTACHMENTS',
                    "items": [
                        {
                            type: "ATTACHMENT_URL",
                            title: "CLICK HERE TO DOWNLOAD THE ATTACHMENT",
                            attachment_name: `${agreementDetail.name}`,
                            attachment_content_type: "application/pdf",
                            attachment_url: `${routingPrefix}api/getDocument/${agreementDetail.id}?filename=${encodeURIComponent(agreementDetail.name)}`,
                            attachment_method: "GET"
                        }
                    ]
                }


            ]
        },
        actions : [
            {
                id: uuidv4(),
                action_key: 'OPEN_IN',
                primary: true,
                label: 'Sign Document',
                url: {
                    href: signingUrl.signingUrlSetInfos[0].signingUrls[0].esignUrl
                },
                type: 'GET',
                completed_label: 'Action Taken',
                allow_repeated : true
            }
        ],
        image : {
            href : 'https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-adobe-sign.png'
        },
        backend_id: agreementDetail.id
    }

    if(agreementDetail.hasOwnProperty('expirationTime')){
        card.body.fields.push(
            {
                type : 'GENERAL',
                title: 'Due Date',
                description : agreementDetail.expirationTime
            }
        )
    }
    return card
}

module.exports = {
    agreementToCards
}

