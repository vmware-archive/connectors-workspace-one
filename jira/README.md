**Jira Connector**

Note: Before running the Jira connector, make sure the discovery server and the OAuth server are running.

**Get details of a specific JIRA issue:**
```
curl -i -X GET \
     -H 'X-Jira-Authorization:Bearer 00D41000000FLSG!AREAQH7ZNr_kQ7XY5UwQhUw11Ml7CxF0a726MsqfiKH4FZOqIpXC2o7YI3wt5_FT_n89nGLSKiFGruzbnmMqfYA61KhSY.Oc' \
     -H "Authorization:Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE0NzUyMTYyMzYsInVzZXJfbmFtZSI6Impkb2UiLCJqdGkiOiI3YmQ0MjUyNi1kNWQ5LTQ0N2EtOTZiMC0zNjBiZDJiZGUyNDMiLCJjbGllbnRfaWQiOiJyb3N3ZWxsIiwic2NvcGUiOlsicmVhZCIsIndyaXRlIl19.bR3u5CcJRi0JtuiNKfCOInzZGIM5mg-w1xMQFBQh3ajxVxbwBqhWXb5WzxX1YzbNGUoBIK5Xy0U5-2MjyB-yy1jRrhSFVX7xpPmWC0eqVGTagp_k7LLSS03Q03AvfvbqVn1tClvk-OH4NNkvcuMQWpUT85j2mnMvLPeUikqekDD6l0HfD9cfmaZ7sMYlXM3F7FQvPwQ1a53yUXEZJX6q3OS2N0m5rHnldtFjj7spbrAEGMy92VsWKIqK3s6KE-Obkuk_zw-08ABnOqkSjHQJXLVMopX2YU6H75jpLUzX_llUPYd8tXIZG7ttgbsehvLRu-x3aTbLI8uPSVI0gYF0SQ" \
     -H "Content-Type:application/json" \
     'http://localhost:8061/api/v1/issues/APF-5'
```     
**Add Comment to JIRA issue:**

```
  curl -i -X POST \
     -H 'X-Jira-Authorization:Bearer 00D41000000FLSG!AREAQH7ZNr_kQ7XY5UwQhUw11Ml7CxF0a726MsqfiKH4FZOqIpXC2o7YI3wt5_FT_n89nGLSKiFGruzbnmMqfYA61KhSY.Oc' \
     -H "Authorization:Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE0NzUyMTYyMzYsInVzZXJfbmFtZSI6Impkb2UiLCJqdGkiOiI3YmQ0MjUyNi1kNWQ5LTQ0N2EtOTZiMC0zNjBiZDJiZGUyNDMiLCJjbGllbnRfaWQiOiJyb3N3ZWxsIiwic2NvcGUiOlsicmVhZCIsIndyaXRlIl19.bR3u5CcJRi0JtuiNKfCOInzZGIM5mg-w1xMQFBQh3ajxVxbwBqhWXb5WzxX1YzbNGUoBIK5Xy0U5-2MjyB-yy1jRrhSFVX7xpPmWC0eqVGTagp_k7LLSS03Q03AvfvbqVn1tClvk-OH4NNkvcuMQWpUT85j2mnMvLPeUikqekDD6l0HfD9cfmaZ7sMYlXM3F7FQvPwQ1a53yUXEZJX6q3OS2N0m5rHnldtFjj7spbrAEGMy92VsWKIqK3s6KE-Obkuk_zw-08ABnOqkSjHQJXLVMopX2YU6H75jpLUzX_llUPYd8tXIZG7ttgbsehvLRu-x3aTbLI8uPSVI0gYF0SQ" \
     -H "Content-Type:application/json" \
     -d \
  '{"body": "This is a new comment"}' \
   'http://localhost:8061/api/v1/issues/123df56/comment'
```