;; Copyright © 2020 VMware, Inc. All Rights Reserved.
;; SPDX-License-Identifier: BSD-2-Clause
(def input identity)

(defn get-collaborators-string [rec]
  (def collaborators (get rec "users" []))
  (clojure.string/join ", " collaborators))

(defn get_author_name [rec]
  (def author (get rec "author")) (get author "displayName"))

(def tenant-admin-filter (fn [x] true))

(def output {
             :updated-at       (fn* [p1__1009#] (get p1__1009# "modifiedDate")),
             :doc-title        (fn* [p1__1008#] (get p1__1008# "fileTitle")),
             :doc-link         (fn* [p1__1011#] (get p1__1011# "link")),
             :updated-by       get_author_name,
             :collaborators    get-collaborators-string,
             :reply-url        (fn* [p1__1012#] (format "/comment/%s/reply" (get p1__1012# "commentId"))),
             :post-comment-url (fn* [p1__1014#] (format "/doc/%s/appendText" (get p1__1014# "docId"))),
             :id               (fn* [p1__1005#] (get p1__1005# "uniqueId")),
             :comment          (fn* [p1__1010#] (get p1__1010# "content")),
             :add-user-url     (fn* [p1__1013#] (format "/doc/%s/addUser" (get p1__1013# "docId"))),
             :doc-id           (fn* [p1__1007#] (get p1__1007# "docId")),
             :comment-id       (fn* [p1__1006#] (get p1__1006# "commentId"))
             })

(def card {
           :name        "Google Docs Assist",


           :header      {
                         :title    "Google Doc - New mention",
                         :subtitle ["You have been mentioned in" :doc-title],
                         :links    {:title    "",
                                    :subtitle ["" :doc-link]
                                    }
                         },

           :backend_id  :id,

           :image_url  "https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-google-docs.png",


           :body        [
                         [:comment "Comment"]
                         [:updated-by "From"]
                         [:updated-at "Date"]
                         [:doc-title "File Name"]
                         [:doc-id "Document ID"]
                         [:collaborators "Collaborators"]
                         ],


           :actions     [{:allow-repeated            true,
                          :remove-card-on-completion true,
                          :completed-label           "Replied",
                          :http-type                 :post,
                          :primary                   true,
                          :mutually-exclusive-id     "ACT_ON_REPLY",
                          :label                     "Reply",
                          :url                       :reply-url,
                          :user-input-field          {:water-mark "Enter the Reply Text",
                                                      :min-length 1
                                                      },
                          :action-key                "USER_INPUT",
                          :request-params            {"document" :entire}}



                         {:allow-repeated            true,
                          :remove-card-on-completion true,
                          :completed-label           "User Added",
                          :http-type                 :post,
                          :primary                   false,
                          :mutually-exclusive-id     "ACT_ON_USER",
                          :label                     "Invite collaborator",
                          :url                       :add-user-url,
                          :user-input-field          {:water-mark "Enter the Invitee mail id",
                                                      :min-length 1
                                                      },
                          :action-key                "USER_INPUT",
                          :request-params            {"document" :entire}}],


           :all-actions [{:allow-repeated            true,
                          :remove-card-on-completion true,
                          :completed-label           "Replied",
                          :http-type                 :post,
                          :primary                   true,
                          :mutually-exclusive-id     "ACT_ON_REPLY",
                          :label                     "Reply",
                          :url                       :reply-url,
                          :user-input-field          {:water-mark "Enter the Reply Text",
                                                      :min-length 1
                                                      },
                          :action-key                "USER_INPUT",
                          :request-params            {"document" :entire}}


                         {:allow-repeated            false,
                          :remove-card-on-completion true,
                          :completed-label           "User Added",
                          :http-type                 :post,
                          :primary                   true,
                          :mutually-exclusive-id     "ACT_ON_USER",
                          :label                     "Add User",
                          :url                       :add-user-url,
                          :user-input-field          {:water-mark "Enter the Invitee mail id",
                                                      :min-length 1
                                                      },
                          :action-key                "USER_INPUT",
                          :request-params            {"document" :entire}
                          }
                         ]
           }
  )
