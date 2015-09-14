(ns onyx.log.full-test
  (:require [clojure.core.async :refer [chan >!! <!! close!]]
            [com.stuartsierra.component :as component]
            [onyx.log.entry :refer [create-log-entry]]
            [onyx.extensions :as extensions]
            [onyx.api :as api]
            [onyx.test-helper :refer [with-env with-peer-group with-peers load-config]]
            [clojure.test :refer [deftest is testing]]
            [onyx.log.curator :as zk]))

(facts "peers all join and watch each other"
       (let [config (load-config)
             onyx-id (java.util.UUID/randomUUID)
             env-config (assoc (:env-config config) :onyx/id onyx-id)
             peer-config (assoc (:peer-config config) :onyx/id onyx-id)
             n-peers 20]
         (with-env [env env-config]
           (with-peer-group [peer-group peer-config]
             (with-peers [v-peers n-peers peer-group]
               (let [ch (chan n-peers)
                     replica (loop [replica (extensions/subscribe-to-log (:log env) ch)]
                               (let [entry (<!! ch)
                                     new-replica (extensions/apply-log-entry entry replica)]
                                 (if (< (count (:pairs new-replica)) n-peers)
                                   (recur new-replica)
                                   new-replica)))]
                 (is (:prepared replica) => {})
                 (is (:accepted replica) => {})
                 (is (set (keys (:pairs replica)))
                       => (set (vals (:pairs replica))))
                 (is (count (:peers replica)) => n-peers)))))))
