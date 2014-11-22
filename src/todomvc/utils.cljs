(ns todomvc.utils
  (:require [cljs.reader :as reader]
            [schema.core :as s :include-macros true])
  (:import [goog.ui IdGenerator]))

(def Todo
  {:id s/Str
   :title s/Str
   :completed s/Bool})

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))

(defn now []
  (js/Date.))

(defn pluralize [n word]
  (if (== n 1)
    word
    (str word "s")))

(defn store
  ([ns] (store ns nil))
  ([ns edn]
    (if-not (nil? edn)
      (.setItem js/localStorage ns (str edn))
      (let [s (.getItem js/localStorage ns)]
        (if-not (nil? s)
          (reader/read-string s)
          [])))))

(defn hidden [is-hidden]
  (if is-hidden
    #js {:display "none"}
    #js {}))
