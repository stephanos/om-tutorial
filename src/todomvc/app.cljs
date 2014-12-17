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

; create database
(def schema {})
(def conn (d/create-conn schema))

; print database changes
(d/listen! conn (fn [tx-report] (println tx-report)))

; init database
(d/transact! conn
  [{:db/id -1 :count 42}])

; query to get count
(def q-count
  '[:find ?count
    :where [?e :count ?count]])

; update count
(defn increment [_ count]
  (d/transact! conn
    [[:db/add 1 :count (inc count)]]))


;; =============================================================================
;; State + View integration

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

(deftype QueryCursor [conn q state]
  IDeref
  (-deref [_]
    state)
  om/ICursor
  (-path [_] [])
  (-state [_] state)
  om/ITransact
  (-transact! [_ _ _ _]
    (throw (js/Error. "not supported")))
  IEquiv
  (-equiv [_ other]
    (if (om/cursor? other)
      (= state (-value other))
      (= state other)))
  ISeqable
  (-seq [this]
    (when (pos? (count @state))
      (map (fn [v _] (om/-derive this v state [])) @state (range))))
  IPrintWithWriter
  (-pr-writer [_ writer opts]
    (-pr-writer q writer opts)))

; run a query
(defn query [q]
  (QueryCursor. conn q (bind conn q)))
  

;; =============================================================================
;; View

(defn app-counter [state]
  (reify
    om/IRender
    (render [_]
      (print state)
      (let [count (ffirst state)]
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
