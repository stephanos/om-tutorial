(ns todomvc.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! <! chan]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
						[secretary.core :as secretary :include-macros true :refer [defroute]]
            [todomvc.utils :refer [pluralize now guid store hidden]]
            [clojure.string :as string]
            [todomvc.item :as item])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

(def ENTER_KEY 13)

(def app-state (atom {:showing :all :todos []}))

;; =============================================================================
;; Routing

(defroute "/" [] (swap! app-state assoc :showing :all))

(defroute "/:filter" [filter] (swap! app-state assoc :showing (keyword filter)))

(def history (History.))

(events/listen history EventType.NAVIGATE
  (fn [e] (secretary/dispatch! (.-token e))))

(.setEnabled history true)

;; =============================================================================
;; Main and Footer Components

(declare toggle-all)

(defn visible? [todo filter]
  (case filter
    :all true
    :active (not (:completed todo))
    :completed (:completed todo)))

(defn main [{:keys [todos showing editing] :as state} comm]
  (dom/section #js {:id "main" :style (hidden (empty? todos))}
    (dom/input
      #js {:id "toggle-all" :type "checkbox"
           :onChange #(toggle-all % state)
           :checked (every? :completed todos)})
    (apply dom/ul #js {:id "todo-list"}
      (om/build-all item/todo-item todos
        {:init-state {:comm comm}
         :key :id
         :fn (fn [todo]
               (cond-> todo
                 (= (:id todo) editing) (assoc :editing true)
                 (not (visible? todo showing)) (assoc :hidden true)))}))))

(defn make-clear-button [completed comm]
  (when (pos? completed)
    (dom/button
      #js {:id "clear-completed"
           :onClick #(put! comm [:clear (now)])}
      (str "Clear completed (" completed ")"))))

(defn footer [state count completed comm]
  (let [clear-button (make-clear-button completed comm)
        sel (-> (zipmap [:all :active :completed] (repeat ""))
                (assoc (:showing state) "selected"))]
    (dom/footer #js {:id "footer" :style (hidden (empty? (:todos state)))}
      (dom/span #js {:id "todo-count"}
        (dom/strong nil count)
        (str " " (pluralize count "item") " left"))
      (dom/ul #js {:id "filters"}
        (dom/li nil (dom/a #js {:href "#/" :className (sel :all)} "All"))
        (dom/li nil (dom/a #js {:href "#/active" :className (sel :active)} "Active"))
        (dom/li nil (dom/a #js {:href "#/completed" :className (sel :completed)} "Completed")))
      clear-button)))

;; =============================================================================
;; Todos

(defn toggle-all [e state]
  (let [checked (.. e -target -checked)]
    (om/transact! state :todos
      (fn [todos] (vec (map #(assoc % :completed checked) todos))))))

(defn handle-new-todo-keydown [e state owner]
  (when (== (.-which e) ENTER_KEY)
    (let [new-field (om/get-node owner "newField")]
      (when-not (string/blank? (.. new-field -value trim))
        (let [new-todo {:id (guid)
                        :title (.-value new-field)
                        :completed false}]
          (om/transact! state :todos
            #(conj % new-todo)
            [:create new-todo]))
        (set! (.-value new-field) "")))
    false))

(defn destroy-todo [state {:keys [id]}]
  (om/transact! state :todos
    (fn [todos] (into [] (remove #(= (:id %) id) todos)))
    [:delete id]))

(defn edit-todo [state {:keys [id]}] (om/update! state :editing id))

(defn save-todos [state] (om/update! state :editing nil))

(defn cancel-action [state] (om/update! state :editing nil))

(defn clear-completed [state]
  (om/transact! state :todos
    (fn [todos] (into [] (remove :completed todos)))))

(defn handle-event [type state val]
  (case type
    :destroy (destroy-todo state val)
    :edit    (edit-todo state val)
    :save    (save-todos state)
    :clear   (clear-completed state)
    :cancel  (cancel-action state)
    nil))

(defn todo-app [{:keys [todos] :as state} owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [comm (chan)]
        (om/set-state! owner :comm comm)
        (go (while true
              (let [[type value] (<! comm)]
                (handle-event type state value))))))

		om/IDidUpdate
    (did-update [_ _ _]
      (store "todos" todos))

		om/IRenderState
    (render-state [_ {:keys [comm]}]
      (let [active    (count (remove :completed todos))
            completed (- (count todos) active)]
        (dom/div nil
					(dom/header #js {:id "header"}
						(dom/h1 nil "todos")
					)
					(dom/input
						#js {:ref "newField" :id "new-todo"
								 :placeholder "What needs to be done?"
								 :onKeyDown #(handle-new-todo-keydown % state owner)})
					(main state comm)
					(footer state active completed comm))))))

(om/root todo-app app-state
  {:target (.getElementById js/document "todoapp")})

(dom/render
  (dom/div nil
    (dom/p nil "Double-click to edit a todo")
    (dom/p nil
      (dom/a #js {:href "http://github.com/swannodette"}))
    (dom/p nil
      #js ["Part of"
           (dom/a #js {:href "http://todomvc.com"} "TodoMVC")]))
  (.getElementById js/document "info"))
