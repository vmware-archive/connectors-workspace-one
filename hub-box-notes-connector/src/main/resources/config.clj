;; Copyright © 2020 VMware, Inc. All Rights Reserved.
;; SPDX-License-Identifier: BSD-2-Clause

(def box-notes identity)

(def input box-notes)

(defn get-collaborators-string [rec]
  (def colloborators (get rec "users" []))
  (clojure.string/join ", " colloborators))

(def tenant-admin-filter (fn [rec] true))


(def output {
             :updated-at       (fn* [p1__992#] (get p1__992# "lastUpdatedDate")),
             :comments         (fn* [p1__993#] (get p1__993# "comment")),
             :file             (fn* [p1__990#] (get p1__990# "title")),
             :note-link        (fn* [p1__994#] (format "https://app.box.com/notes/%s" (get p1__994# "noteId"))),
             :post-message-url (fn* [p1__996#] (format "/notes/%s/message" (get p1__996# "commentId"))),
             :updated-by       (fn* [p1__991#] (get p1__991# "lastEditor")),
             :collaborators    get-collaborators-string,
             :id               (fn* [p1__988#] (get p1__988# "commentId")),
             :noteId           (fn* [p1__989#] (get p1__989# "noteId")),
             :add-user-url     (fn* [p1__995#] (format "/notes/%s/addUser" (get p1__995# "commentId")))})


(def card {
             :name        "Box Notes Assist",

             :header      {:title    "Box Notes - New mention",
                           :subtitle ["You have been @mentioned in" :file],
                           :links    {:title "", :subtitle ["" :note-link]}},

             :backend_id   :id,

             :image_url  "https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-box-notes.png",

             :body        [[:comments "Comment"]
                           [:updated-by "From"]
                           [:updated-at "Date"]
                           [:file "File Name"]
                           [:noteId "Notes ID"]
                           [:collaborators "Collaborators"]],


             :actions     [{:allow-repeated            true,
                            :remove-card-on-completion true,
                            :completed-label           "Message Added",
                            :http-type                 :post,
                            :primary                   true,
                            :mutually-exclusive-id     "ACT_ON_COMMENT",
                            :label                     "Comment",
                            :url                       :post-message-url,
                            :user-input-field          {
                                                         :water-mark "Enter the Message",
                                                         :min-length 1

                                                         },
                            :action-key                "USER_INPUT",
                            :request-params            {"document" :entire, "actionType" "addComment"}}



                           {:allow-repeated            true,
                            :remove-card-on-completion true,
                            :completed-label           "User Added",
                            :http-type                 :post,
                            :primary                   false,
                            :mutually-exclusive-id     "ACT_ON_MESSAGE",
                            :label                     "Invite collaborator",
                            :url                       :add-user-url,
                            :user-input-field          {
                                                         :water-mark "Enter the mail id",
                                                         :min-length 1

                                                         },
                            :action-key                "USER_INPUT",
                            :request-params            {"document" :entire, "actionType" "addUser"}}],


             :all-actions [{:allow-repeated            true,
                            :remove-card-on-completion true,
                            :completed-label           "User Added",
                            :http-type                 :post,
                            :primary                   true,
                            :mutually-exclusive-id     "ACT_ON_MESSAGE",
                            :label                     "Add User",
                            :url                       :add-user-url,
                            :user-input-field          {:water-mark "Enter the mail id", :min-length 1
                                                        },
                            :action-key                "USER_INPUT",
                            :request-params            {"document" :entire, "actionType" "addUser"}}



                           {:allow-repeated            true,
                            :remove-card-on-completion true,
                            :completed-label           "Message Added",
                            :http-type                 :post,
                            :primary                   true,
                            :mutually-exclusive-id     "ACT_ON_COMMENT",
                            :label                     "Post a message",
                            :url                       :post-message-url,
                            :user-input-field          {:water-mark "Enter the Message",
                                                        :min-length 1
                                                        },
                            :action-key                "USER_INPUT",
                            :request-params            {"document" :entire, "actionType" "addComment"}}



                 ]})
