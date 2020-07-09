;;Copyright © 2020 VMware, Inc. All Rights Reserved.
;;SPDX-License-Identifier: BSD-2-Clause

(require (quote [com.xenovus.xplib :as fl]))

(def input identity)

(defn get_owner_name [rec] (get (get rec "Owner") "Name"))

(defn get_lead_link [rec]
  (get rec "link"))

(def output {
             :lead-id          (fn* [p1__1042#] (get p1__1042# "Id")),
             :lead-name        (fn* [p1__1041#] (get p1__1041# "Name")),
             :lead-description (fn* [p1__1047#] (get p1__1047# "Description")),
             :owner-name       get_owner_name,
             :dismiss-url      (fn* [p1__1050#] (format "/leads/%s/dismiss" (get p1__1050# "Id"))),
             :log-call-url     (fn* [p1__1049#] (format "/leads/%s/logACall" (get p1__1049# "Id"))),
             :add-task-url     (fn* [p1__1048#] (format "/leads/%s/addTask" (get p1__1048# "Id"))),
             :id               (fn* [p1__1040#] (get p1__1040# "Id")),
             :lead-phone       (fn* [p1__1045#] (get p1__1045# "Phone")),
             :lead-mail        (fn* [p1__1046#] (get p1__1046# "Email")),
             :lead-status      (fn* [p1__1044#] (get p1__1044# "Status")),
             :lead-company     (fn* [p1__1043#] (get p1__1043# "Company")),
             :lead-link        get_lead_link
             })

(def tenant-admin-filter (fn [x] true))

(def card
  {
   :name        "Salesforce New Lead Assist",
   :header      {:title    "Salesforce - New lead assigned",
                 :subtitle ["New lead" "from" :lead-company],
                 :links    {:title    "",
                            :subtitle [:lead-link "" ""]
                            }
                 },
   :backend_id  :lead-id,
   :image_url   "https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-salesforce.png",
   :body        [[:lead-name "Lead Name"]
                 [:lead-mail "Email"]
                 [:lead-phone "Phone"]
                 [:lead-company "Company"]
                 [:lead-status "Status"]
                 [:owner-name "Owner"]
                 [:lead-description "Comments"]
                 [:lead-link "Lead URL"]],
   :actions     [
                 {
                  :allow-repeated            true,
                  :remove-card-on-completion true,
                  :completed-label           "Call Logged",
                  :http-type                 :post,
                  :primary                   true,
                  :mutually-exclusive-id     "LOG_A_CALL",
                  :label                     "Log call",
                  :url                       :log-call-url,
                  :user-input-field          {
                                              :water-mark "Please Enter Call Details",
                                              :min-length 1
                                              },
                  :action-key                "USER_INPUT"
                  }
                 {
                  :allow-repeated            true,
                  :remove-card-on-completion true,
                  :completed-label           "Task added",
                  :http-type                 :post,
                  :primary                   true,
                  :mutually-exclusive-id     "ADD_TASK",
                  :label                     "Add task",
                  :url                       :add-task-url,
                  :user-input-field          {
                                              :water-mark "Please Enter Task Subject",
                                              :min-length 1
                                              },
                  :action-key                "USER_INPUT"
                  }
                 ],
   :all-actions [
                 {
                  :allow-repeated            true,
                  :remove-card-on-completion true,
                  :completed-label           "Task added",
                  :http-type                 :post,
                  :primary                   false,
                  :mutually-exclusive-id     "ADD_TASK",
                  :label                     "Add Task",
                  :url                       :add-task-url,
                  :user-input-field          {
                                              :water-mark "Please Enter Task Subject",
                                              :min-length 1
                                              },
                  :action-key                "USER_INPUT",
                  :request-params            {
                                              "lead" :entire
                                              }
                  }
                 {
                  :allow-repeated            true,
                  :remove-card-on-completion true,
                  :completed-label           "Call Logged",
                  :http-type                 :post,
                  :primary                   false,
                  :mutually-exclusive-id     "LOG_A_CALL",
                  :label                     "Log A Call",
                  :url                       :log-call-url,
                  :user-input-field          {
                                              :water-mark "Please Enter Call Details",
                                              :min-length 1
                                              },
                  :action-key                "USER_INPUT",
                  :request-params            {
                                              "lead" :entire
                                              }
                  }
                 {
                  :allow-repeated            true,
                  :remove-card-on-completion true,
                  :completed-label           "Dismissed",
                  :http-type                 :post,
                  :primary                   false,
                  :mutually-exclusive-id     "DISMISS_LEAD",
                  :label                     "Dismiss",
                  :url                       :dismiss-url,
                  :action-key                "DIRECT",
                  :request-params            {
                                              "lead" :entire
                                              }
                  }
                 ]
   })