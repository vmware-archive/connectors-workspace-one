
;; Copyright © 2020 VMware, Inc. All Rights Reserved.
;; SPDX-License-Identifier: BSD-2-Clause

(require (quote [com.xenovus.xplib :as fl]))

(def input identity)

(defn Contact [rec] (get rec "primaryContact"))

(defn get_contact_person [rec] (get (Contact rec) "yomifullname"))

(defn get_contact_details [rec] (get (Contact rec) "emailaddress1"))

(def output
  {
   :account-telephone   (fn* [p1__999#] (get p1__999# "telephone1")),
   :contact-details     get_contact_details,
   :account-link        (fn* [p1__1001#] (get p1__1001# "websiteurl")),
   :account-name        (fn* [p1__998#] (get p1__998# "name")),
   :task-url            (fn* [p1__1002#] (format "/accounts/%s/create_task" (get p1__1002# "accountid"))),
   :call-url            (fn* [p1__1004#] (format "/accounts/%s/schedule_phonecall" (get p1__1004# "accountid"))),
   :id                  (fn* [p1__997#] (get p1__997# "accountid")),
   :contact-person      get_contact_person,
   :account-description (fn* [p1__1000#] (get p1__1000# "description")),
   :appointment-url     (fn* [p1__1003#] (format "/accounts/%s/schedule_appointment" (get p1__1003# "accountid")))
   }
  )
(def tenant-admin-filter (fn [x] true))

(def card
  {
   :name        "Microsoft Dynamics New Account Assist",
   :header      {:title    "Microsoft Dynamics - New account assigned",
                 :subtitle ["New account" "for" :account-name],
                 :links    {
                            :title "",
                            :subtitle
                                   [:account-link "" ""]
                            }
                 },
   :backend_id   :id,

   :image_url  "https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-ms-dynamics.png",

   :body        [
                 [:account-name "Account"]
                 [:contact-person "Contact Person"]
                 [:contact-details "Contact Details"]
                 [:account-telephone "Phone"]
                 [:account-description "Comments"]
                 [:account-link "Account Url"]
                 ],

   :actions     [
                 {
                  :allow-repeated            true,
                  :remove-card-on-completion true,
                  :completed-label           "Phone Call Created",
                  :http-type                 :post,
                  :primary                   true,
                  :mutually-exclusive-id     "ADD_PHONE_CALL",
                  :label                     "Log call",
                  :url                       :call-url,
                  :user-input-field          {
                                              :water-mark "Subject",
                                              :min-length 1,
                                              :max-length 200
                                              },
                  :action-key                "USER_INPUT"
                  }
                 {
                  :allow-repeated            true,
                  :remove-card-on-completion true,
                  :completed-label           "Task Created",
                  :http-type                 :post,
                  :primary                   true,
                  :mutually-exclusive-id     "ADD_TASK",
                  :label                     "Add task",
                  :url                       :task-url,
                  :user-input-field          {
                                              :water-mark "Subject",
                                              :min-length 1,
                                              :max-length 200
                                              },
                  :action-key                "USER_INPUT"}
                 ],
   :all-actions [
                 {
                  :allow-repeated            true,
                  :remove-card-on-completion true,
                  :completed-label           "Task Created",
                  :http-type                 :post, :primary false,
                  :mutually-exclusive-id     "ADD_TASK",
                  :label                     "+ Task",
                  :url                       :task-url,
                  :user-input-field          {
                                              :water-mark "Subject",
                                              :min-length 1,
                                              :max-length 200

                                              },
                  :action-key                "USER_INPUT"
                  }
                 {
                  :allow-repeated            true,
                  :remove-card-on-completion true,
                  :completed-label           "Appointment Created",
                  :http-type                 :post,
                  :primary                   false,
                  :mutually-exclusive-id     "ADD_APPOINTMENT",
                  :label                     "+ Appointment",
                  :url                       :appointment-url,
                  :user-input-field          {
                                              :water-mark "Subject",
                                              :min-length 1,
                                              :max-length 200
                                              },
                  :action-key                "USER_INPUT"
                  }
                 {
                  :allow-repeated            true,
                  :remove-card-on-completion true,
                  :completed-label           "Phone Call Created",
                  :http-type                 :post,
                  :primary                   false,
                  :mutually-exclusive-id     "ADD_PHONE_CALL",
                  :label                     "+ Phone Call",
                  :url                       :call-url,
                  :user-input-field          {
                                               :water-mark "Subject",
                                               :min-length 1,
                                               :max-length 200

                                               },
                  :action-key                "USER_INPUT"
                  }
                 ]
   }
  )
