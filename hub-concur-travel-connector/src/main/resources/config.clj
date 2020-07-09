;; Copyright © 2020 VMware, Inc. All Rights Reserved.
;; SPDX-License-Identifier: BSD-2-Clause

(require (quote [com.xenovus.xplib :as fl]))
(require (quote [clojure.string :as cs]))

(def date-fmt-str "M/d/yyyy h:m a")

(def formatter (fl/get-formatter date-fmt-str))

(def travel-requests identity)

(def input travel-requests)

(defn get-entries [rec] (get rec "EntriesList"))

(defn get-segments [rec] (get rec "SegmentsList"))

(defn check-am-or-pm [s]
  (
    or (cs/includes? (cs/lower-case s) "am") (cs/includes? (cs/lower-case s) "pm"))

  )


(defn add-time-am-pm [s]
  (
    cond (cs/blank? s) ""
         (check-am-or-pm s) s
         :else (str (cs/trim s) " 12:00 AM"))

  )


(defn add-utc-to-am-pm-string [s]
  (if (cs/blank? (re-find #"(?i)[ap]m" s)) s (str s " UTC")))



(defn get-segment-info [rec]
  (let [dep (format "%s %s" (fl/nil->blank (get rec "DepartureDate" ""))
                    (fl/nil->blank (get rec "DepartureTime" "")))
        arr (format "%s %s" (fl/nil->blank (get rec "ArrivalDate" ""))
                    (fl/nil->blank (get rec "ArrivalTime" "")))
        d1 (add-time-am-pm dep)
        a1 (add-time-am-pm arr)
        dur (if (or (cs/blank? d1) (cs/blank? a1))
              ""
              (str (fl/duration d1 a1 date-fmt-str)))
        seg-type (get rec "SegmentType")]

    {:arrival-date               (if (= seg-type "Miscellaneous") (add-utc-to-am-pm-string dep) ""),
     :pick-up-city               (if (= seg-type "Car Rental") (get rec "FromLocationName") ""),
     :arrival-city               (if (= seg-type "Miscellaneous") (get rec "ToLocationName") ""),
     :drop-off-city              (if (= seg-type "Car Rental") (get rec "ToLocationName") ""),
     :arrival-location-details   (if (= seg-type "Miscellaneous") (get rec "ToLocationDetail") ""),
     :city                       (if (= seg-type "Hotel Reservation") (get rec "ToLocationName") ""),
     :arrive-at                  (if (contains? (set (quote ("Air Ticket" "Train Ticket"))) seg-type)
                                   (add-utc-to-am-pm-string arr)
                                   ""),
     :duration                   dur,
     :pick-up-date               (if (= seg-type "Car Rental") (add-utc-to-am-pm-string dep) ""),
     :departure-city             (if (= seg-type "Miscellaneous") (get rec "FromLocationName") ""),
     :title                      seg-type,
     :foreign-amt                (fl/format-amount (get rec "ForeignAmount") (get rec "ForeignCurrencyCode")),
     :segment-type               seg-type,
     :from                       (if (contains? (set (quote ("Air Ticket" "Train Ticket"))) seg-type)
                                   (get rec "FromLocationName")
                                   ""),
     :comment                    (-> (get rec "CommentsList") (nth 0) ((fn* [p1__1015#] (get p1__1015# "Comment" "")))),
     :location-details           (if (= seg-type "Hotel Reservation") (get rec "ToLocationDetail") ""),
     :check-in                   (if (= seg-type "Hotel Reservation") (add-utc-to-am-pm-string dep) ""),
     :departure-location-details (if (= seg-type "Miscellaneous") (get rec "FromLocationDetail") ""),
     :drop-off-date              (if (= seg-type "Car Rental") (add-utc-to-am-pm-string arr) ""),
     :pick-up-location-details   (if (= seg-type "Car Rental") (get rec "FromLocationDetail") ""),
     :drop-off-location-details  (if (= seg-type "Car Rental") (get rec "ToLocationDetail") ""),
     :departure-date             (if (= seg-type "Miscellaneous") (add-utc-to-am-pm-string arr) ""),
     :check-out                  (if (= seg-type "Hotel Reservation") (add-utc-to-am-pm-string arr) ""),
     :depart-at                  (if (contains? (set (quote ("Air Ticket" "Train Ticket"))) seg-type)
                                   (add-utc-to-am-pm-string dep)
                                   ""),
     :to                         (if (contains? (set (quote ("Air Ticket" "Train Ticket"))) seg-type)
                                   (get rec "ToLocationName")
                                   "")})
  )




(defn get-exception-list [rec] (get rec "ExceptionsList" []))

(defn get-exception-message [rec] (get rec "ExceptionMessage" ""))

(defn get-exception-message-list [rec]
  (remove cs/blank? (map get-exception-message (get-exception-list rec))))


(defn get-exception-string [rec]
  (cs/join ""
           (map-indexed (fn* [p1__1016# p2__1017#] (str (+ p1__1016# 1) ") " p2__1017#))
                        (get-exception-message-list rec)))
  )


(defn get-key-name [key] (name key))


(defn prefix-key [key prefix] (keyword (str prefix (get-key-name key))))


(defn transform-segment [seg index]
  (into {} (map (fn [[k v]] [(prefix-key k (format "seg-%d-" index)) v]) seg)))


(defn get-entry-info [entry]
  (let [segments (for [s (get-segments entry)] (get-segment-info s))
        is-mult-segment (> (count segments) 1)
        transform (fn [s idx] (transform-segment s idx))
        segs (into {} (map-indexed (fn [idx s] s) segments))
        entry-flds {:title           (str (get entry "ExpenseTypeName") " - Multi Segment"),
                    :segment-count   (get entry "SegmentCount"),
                    :foreign-amt     (fl/format-amount (get entry "ForeignAmount") (get entry "ForeignCurrencyCode")),
                    :expense-type    (get entry "ExpenseTypeName"),
                    :exceptions      (get-exception-string entry),
                    :is-mult-segment is-mult-segment}
        transform-multi-seg (fn [idx seg]
                              (dissoc
                                (assoc seg :title (str "Segment - " (+ idx 1)) :expense-type (entry-flds :expense-type))
                                :foreign-amt))
        multi-segs (map-indexed transform-multi-seg segments)]

    (if is-mult-segment
      (into [] (cons entry-flds multi-segs))
      [(merge entry-flds segs)]))
  )



(defn select-the-right-date [rec]
  (let [dep (add-time-am-pm (:depart-at rec))
        arr (add-time-am-pm (:arrive-at rec))]
    (cond (not (cs/blank? dep)) (fl/time-object dep date-fmt-str)
          (not (cs/blank? arr)) (fl/time-object arr date-fmt-str)
          :else (fl/epoch-date)))

  )

(def employee-name (fn* [p1__1018#] (get p1__1018# "EmployeeName")))


(def login-id (fn* [p1__1019#] (get p1__1019# "LoginID")))


(defn submit-date [rec]
  (let [dt (fl/time-object (rec "SubmitDate") "yyyy-MM-dd'T'HH:mm:ss")]
    (str (.format dt formatter) " UTC")))



(defn get-submitter-info [rec]
  [{:email         (login-id rec),
    :employee-name (employee-name rec),
    :submit-date   (submit-date rec)}])


(defn sort-by-dep-arr [recs] (into [] (sort-by select-the-right-date recs)))


(defn get-all-segment-info [rec]
  (sort-by-dep-arr
    (into []
          (for [e (get-entries rec)
                s (get-segments e)]
            (get-segment-info s))))

  )


(defn get-all-entries-info [rec]
  (into [] (flatten (for [e (get-entries rec)] (get-entry-info e))))

  )


(defn plural-form [count] (if (< count 2) "" "s"))


(defn duration [rec]
  (let [dep (fl/nil->blank (get rec "StartDate" ""))
        arr (fl/nil->blank (get rec "EndDate" ""))
        d1 (add-time-am-pm dep)
        a1 (add-time-am-pm arr)
        dur (if (or (cs/blank? d1) (cs/blank? a1))
              0
              (+ 1 (fl/duration d1 a1 date-fmt-str)))]
    (if (= 0 dur) "" (format "%d day%s" dur (plural-form dur))))
  )

(def filter-by-amount (fn [rec] (> (:trip-total rec) 5000)))

(defn filter-immediate-travel [rec]
  (let [s (:start-date rec)]
    (if (cs/blank? s) true (<= (fl/duration-till-date s date-fmt-str) 7))))

(def tenant-admin-filter (fn [rec] (and true true)))


(def output {
             :submitted-by        employee-name,
             :end-date            (fn* [p1__1025#] (get p1__1025# "EndDate")),
             :purpose             (fn* [p1__1026#] (get p1__1026# "Purpose")),
             :login-id            login-id,
             :duration            duration,
             :request-name        (fn* [p1__1023#] (get p1__1023# "RequestName")),
             :trip-total          (fn* [p1__1021#] (Double/parseDouble (get p1__1021# "RequestTotal" "0"))),
             :id                  (fn* [p1__1020#] (get p1__1020# "RequestID")),
             :employee-name       employee-name,
             :submit-date         submit-date,
             :exceptions          get-exception-string,
             :start-date          (fn* [p1__1024#] (get p1__1024# "StartDate")),
             :submitter           (fn* [p1__1027#] (get-submitter-info p1__1027#)),
             :total-amt           (fn* [p1__1022#]
                                    (fl/format-amount (get p1__1022# "RequestTotal") (get p1__1022# "CurrencyCode"))),
             :entries-section     get-all-entries-info,
             :workflow-action-url (fn* [p1__1028#] (get p1__1028# "WorkflowActionURL"))}
  )

(defn get-entry-schema [entry]
  (let [seg-count (:segment-count entry)
        seg-suffix-str "(Segment %d)"
        seg-suffix (fn [idx label] (str label (if (< seg-count 2) "" (format seg-suffix-str idx))))
        seg-schema [[:from "From"]
                    [:segment-type "Type"]
                    [:check-in "Check In"]
                    [:to "To"]
                    [:check-out "Check Out"]
                    [:arrive-at "Arrive at"]
                    [:depart-at "Depart at"]]
        keys (mapcat (fn [idx] (into [] (map (fn [[k v]] [k v]) seg-schema))) (range 0 seg-count))]
    (into []
          (concat
            [[:expense-type "Expense Type"]
             [:foreign-amt "Amount"]
             [:exceptions "Exceptions"]]
            keys))))

(def card {
           :name        "Concur Travel Approval",

           :header      {:title    "Concur - Travel approval request",
                         :subtitle [:request-name],
                         :links    {:title    "",
                                    :subtitle ["https://www.concursolutions.com"]}},
           :backend_id  :id,
           :image_url  "https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-concur.png",

           :body        [[:submitted-by "Submitted By"]
                         [:total-amt "Amount"]
                         [:purpose "Purpose"]
                         [:start-date "Start Date"]
                         [:end-date "End Date"]
                         [:request-name "Request Name"]
                         [:submit-date "Submitted On"]
                         [:exceptions "Exceptions"]
                         [:entries-section
                          :title
                          [[]
                           [:expense-type
                            {"Airfare"
                                             [[:from "From"]
                                              [:to "To"]
                                              [:depart-at "Depart at"]
                                              [:arrive-at "Arrive at"]
                                              [:expense-type "Expense Type"]
                                              [:foreign-amt "Amount"]
                                              [:exceptions "Exceptions"]],
                             "Train"         [[:from "From"]
                                              [:to "To"]
                                              [:arrive-at "Arrive at"]
                                              [:depart-at "Depart at"]
                                              [:expense-type "Expense Type"]
                                              [:foreign-amt "Amount"]
                                              [:exceptions "Exceptions"]],
                             "Hotel"         [[:location-details "Hotel Name"]
                                              [:city "City"]
                                              [:check-in "Check In"]
                                              [:check-out "Check Out"]
                                              [:expense-type "Expense Type"]
                                              [:foreign-amt "Amount"]
                                              [:exceptions "Exceptions"]],
                             "Car Rental"    [[:pick-up-city "Pick Up City"]
                                              [:pick-up-date "Pick Up Date"]
                                              [:drop-off-city "Drop Off City"]
                                              [:drop-off-date "Drop Off Date"]
                                              [:expense-type "Expense Type"]
                                              [:foreign-amt "Amount"]
                                              [:exceptions "Exceptions"]],
                             "Miscellaneous" [[:arrival-city "Arrival City"]
                                              [:arrival-date "Arrival Date"]
                                              [:departure-city "Departure City"]
                                              [:departure-date "Departure Date"]
                                              [:expense-type "Expense Type"]
                                              [:foreign-amt "Amount"]
                                              [:exceptions "Exceptions"]]}]]]],

           :actions     [{:allow-repeated            false,
                          :remove-card-on-completion true,
                          :completed-label           "Approved",
                          :http-type                 :post,
                          :primary                   true,
                          :mutually-exclusive-id     "ACT_ON_REQUEST",
                          :label                     "Approve",
                          :url                       "/travelrequest/workflowaction",
                          :user-input-field          {
                                                      :water-mark "Provide your comments here",
                                                      :min-length 1
                                                      },
                          :action-key                "USER_INPUT",
                          :request-params            {
                                                      "travelRequest" :entire,
                                                      "actionType" "APPROVE"
                                                      }}

                         {:allow-repeated            false,
                          :remove-card-on-completion true,
                          :completed-label           "Sent Back",
                          :http-type                 :post,
                          :primary                   false,
                          :mutually-exclusive-id     "ACT_ON_REQUEST",
                          :label                     "Send Back",
                          :url                       "/travelrequest/workflowaction",
                          :user-input-field          {
                                                      :water-mark "Provide your comments here",
                                                      :min-length 1
                                                      },
                          :action-key                "USER_INPUT",
                          :request-params            {"travelRequest" :entire, "actionType" "SEND_BACK"}}],

           :all-actions [{:allow-repeated            false,
                          :remove-card-on-completion true,
                          :completed-label           "Sent Back",
                          :http-type                 :post,
                          :primary                   true,
                          :mutually-exclusive-id     "ACT_ON_REQUEST",
                          :label                     "Send Back",
                          :url                       "/travelrequest/workflowaction",
                          :user-input-field          {
                                                      :water-mark "Provide your comments here",
                                                      :min-length 1
                                                      },
                          :action-key                "USER_INPUT",
                          :request-params            {"travelRequest" :entire, "actionType" "SEND_BACK"}}

                         {:allow-repeated            false,
                          :remove-card-on-completion true,
                          :completed-label           "Approved",
                          :http-type                 :post,
                          :primary                   true,
                          :mutually-exclusive-id     "ACT_ON_REQUEST",
                          :label                     "Approve",
                          :url                       "/travelrequest/workflowaction",
                          :user-input-field          {
                                                      :water-mark "Provide your comments here",
                                                      :min-length 1
                                                      },
                          :action-key                "USER_INPUT",
                          :request-params            {"travelRequest" :entire, "actionType" "APPROVE"}}


                         {:allow-repeated            false,
                          :remove-card-on-completion true,
                          :completed-label           "Dismissed",
                          :http-type                 :post,
                          :primary                   false,
                          :mutually-exclusive-id     "DISMISS_TASK",
                          :label                     "Dismiss",
                          :url                       "/card/dismiss",
                          :action-key                "DIRECT",
                          :request-params            {"actionType" "dismiss"}}]

           })
