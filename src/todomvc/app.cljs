(ns hello.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [datascript :as d])
  (:import [goog.ui IdGenerator]))

(enable-console-print!)

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))

;; =============================================================================
;; State

; from https://gist.github.com/allgress/11348685
(defn bind
  ([conn q]
    (bind conn q (atom nil)))
  ([conn q state]
    (let [k (guid)]
      (reset! state (d/q q @conn))
      (d/listen! conn k (fn [tx-report]
                          (let [novelty (d/q q (:tx-data tx-report))]
                            (when (not-empty novelty) ;; Only update if query results actually changed
                              (reset! state (d/q q (:db-after tx-report)))))))
      (set! (.-__key state) k)
      state)))

(defn unbind
  [conn state]
  (d/unlisten! conn (.-__key state)))


; create database
(def schema {})
(def conn (d/create-conn schema))

; listen to database changes
(d/listen! conn (fn [tx-report] (println tx-report)))

; init database
(d/transact! conn
  [{:db/id -1
    :count 0}])

; query to get current count
(def q-count
  '[:find ?count
    :where [?e :count ?count]])

; update count
(defn increment [_ count]
  (d/transact! conn
    [[:db/add 1 :count (inc count)]]))

; run a query
(defn query [q]
  (bind conn q)) ; TODO: how to transform to ICursor?


;; =============================================================================
;; View

(defn app-counter [state]
  (reify
    om/IRender
    (render [_]
      (let [count (ffirst @state)]
        (print count)
        (dom/div nil
          (dom/span nil count)
          (dom/button
            #js {:style #js {:marginLeft "5px"}
                 :onClick #(increment % count)} "+"))))))

(defn app-view []
  (reify
    om/IRender
    (render [_]
      (let [count (query q-count)]
        (dom/div nil
          (dom/h2 nil "Hello World")
          (om/build #(app-counter count) {}))))))


;; =============================================================================
;; Main

(def app-state (atom []))

(om/root app-view app-state
  {:target (. js/document (getElementById "app"))})
