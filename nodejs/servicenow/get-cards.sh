#!/bin/bash


source .env


curl -v \
     -X POST \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -H "Authorization: Bearer $USER_JWT_TOKEN" \
     -H "X-ServiceNow-Authorization: Bearer $servicenow_token" \
     -H "X-ServiceNow-Base-Url: https://dev15329.service-now.com" \
     -d "
{
            \"tokens\": {
                \"ticket_id\": [
                    \"REQ0010061\",
                    \"REQ0010062\",
                    \"REQ0010076\",
                    \"REQ0010075\",
                    \"REQ0010074\",
                    \"REQ0010073\"
                ],
                \"email\" : [
                    \"jjeff@vmware.com\"
                ]
            }
}
" \
     "http://localhost:4000/cards/requests"
     #"http://$HOST:$PORT/hero/cards/requests"
