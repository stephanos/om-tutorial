(ns todomvc.item
  (:require [cljs.core.async :refer [put!]]
            [todomvc.utils :refer [Todo now hidden]]
            [clojure.string :as string]
            [schema.core :as s :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :as html :refer-macros [html]]
            [om.core :as om :include-macros true]))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)

;; =============================================================================
;; Todo Item

;; -----------------------------------------------------------------------------
;; Event Handlers

(defn submit [e todo owner comm]
  (when-let [edit-text (om/get-state owner :edit-text)]
    (if-not (string/blank? (.trim edit-text))
      (do
        (om/update! todo :title edit-text) ; TODO: datascript
        (put! comm [:save @todo]))
      (put! comm [:destroy @todo])))
  false)

(defn complete [todo] ; TODO: datascript
  (om/transact! todo :completed #(not %)))

(defn destroy [todo comm]
  (put! comm [:destroy @todo]))

(defn edit [e todo owner comm]
  (let [todo @todo
        node (om/get-node owner "editField")]
    (put! comm [:edit todo])
    (doto owner
      (om/set-state! :needs-focus true)
      (om/set-state! :edit-text (:title todo)))))

(defn key-down [e todo owner comm]
  (condp == (.-keyCode e)
    ESCAPE_KEY (let [todo @todo]
                 (om/set-state! owner :edit-text (:title todo))
                 (put! comm [:cancel todo]))
    ENTER_KEY (submit e todo owner comm)
    nil))

(defn change [e todo owner]
  (om/set-state! owner :edit-text (-> e .-target .-value)))

;; -----------------------------------------------------------------------------
;; Component

(defcomponent todo-item [todo :- Todo owner]
  (init-state [_]
    (s/validate Todo todo)
    {:edit-text (:title todo)})

  (did-update [_ _ _]
    (when (and (:editing todo)
            (om/get-state owner :needs-focus))
      (let [node (om/get-node owner "editField")
            len (-> node .-value .-length)]
        (.focus node)
        (.setSelectionRange node len len))
      (om/set-state! owner :needs-focus nil)))

  (render-state [_ {:keys [comm] :as state}]
    (let [class (cond-> ""
                  (:completed todo) (str "completed")
                  (:editing todo) (str "editing"))]
      (html
        [:li {:class class :style (hidden (:hidden todo))}
         [:div {:class "view"}]
         [:input {:class "toggle" :type "checkbox"
                  :checked (and (:completed todo) "checked")
                  :on-change #(complete todo)}]
         [:label {:on-double-click #(edit % todo owner comm)}
          (:title todo)]
         [:button {:class "destroy"
                   :on-click #(destroy todo comm)}]
         [:input {:ref "editField" :class "edit"
                  :value (om/get-state owner :edit-text)
                  :on-blur #(submit % todo owner comm)
                  :on-change #(change % todo owner)
                  :on-key-down #(key-down % todo owner comm)}]]))))
