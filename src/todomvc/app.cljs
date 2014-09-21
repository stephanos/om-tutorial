(ns todomvc.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [<! chan]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
						[sablono.core :as html :refer-macros [html]]
						[secretary.core :as secretary :include-macros true :refer [defroute]]
            [todomvc.utils :refer [pluralize now guid store hidden]]
            [clojure.string :as string]
            [todomvc.item :as item])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

(def ENTER_KEY 13)

;; =============================================================================
;; State

(def app-state (atom {:showing :all :todos []}))

;; =============================================================================
;; Routing

(defroute "/" []
	(swap! app-state assoc :showing :all))

(defroute "/:filter" [filter]
	(swap! app-state assoc :showing (keyword filter)))

(def history (History.))

(events/listen history EventType.NAVIGATE
  (fn [e] (secretary/dispatch! (.-token e))))

(.setEnabled history true)

;; =============================================================================
;; Todo App

;; -----------------------------------------------------------------------------
;; Event Handlers

(defn toggle-all [e state]
	(let [checked (-> e .-target .-checked)]
		(om/transact! state :todos
			(fn [todos] (vec (map #(assoc % :completed checked) todos))))))

(defn enter-new-todo [e state owner]
	(when (== (.-which e) ENTER_KEY)
		(let [new-field 		 (om/get-node owner "newField")
					new-field-text (string/trim (.-value new-field))]
			(when-not (string/blank? new-field-text)
				(let [new-todo {:id (guid)
												:title new-field-text
												:completed false}]
					(om/transact! state :todos #(conj % new-todo)))
				(set! (.-value new-field) "")))
		false))

(defn destroy-todo [state {:keys [id]}]
	(om/transact! state :todos
		(fn [todos] (vec (remove #(= (:id %) id) todos)))))

(defn edit-todo [state {:keys [id]}]
	(om/update! state :editing id))

(defn save-todos [state]
	(om/update! state :editing nil))

(defn cancel-action [state]
	(om/update! state :editing nil))

(defn handle-event [type state val]
	(case type
		:destroy (destroy-todo state val)
		:edit    (edit-todo state val)
		:save    (save-todos state)
		:cancel  (cancel-action state)
		nil))

;; -----------------------------------------------------------------------------
;; Sub-Components

(defn visible? [todo filter]
  (case filter
    :all true
    :active (not (:completed todo))
    :completed (:completed todo)))

(defn header []
	(html
		[:header {:id "header"}
			[:h1 "todos"]]))

(defn list-items [todos showing editing comm]
	(om/build-all item/todo-item todos
		{:init-state {:comm comm}
		 :key :id
		 :fn (fn [todo]
					 (cond-> todo
						 (= (:id todo) editing) (assoc :editing true)
						 (not (visible? todo showing)) (assoc :hidden true)))}))

(defn listing [{:keys [todos showing editing] :as state} comm]
  (html
		[:section {:id "main" :style (hidden (empty? todos))}
			[:input
			 	{:id "toggle-all" :type "checkbox"
				 :on-change #(toggle-all % state)
				 :checked (every? :completed todos)}]
			[:ul {:id "todo-list"}
				(list-items todos showing editing comm)]]))

(defn footer [{:keys [todos] :as state}]
  (let [count (count (remove :completed todos))
				sel   (-> (zipmap [:all :active :completed] (repeat ""))
                  (assoc (:showing state) "selected"))]
		(html
			[:footer {:id "footer" :style (hidden (empty? todos))}
      	[:span {:id "todo-count"}
					[:strong count]
					(str " " (pluralize count "item") " left")]
				[:ul {:id "filters"}
					[:li [:a {:href "#/" :class (sel :all)} "All"]]
					[:li [:a {:href "#/active" :class (sel :active)} "Active"]]
					[:li [:a {:href "#/completed" :class (sel :completed)} "Completed"]]]])))

(defn render-disclaimer []
	(dom/render
		(html
			[:div
				[:p "Double-click to edit a todo"]
				[:p "Part of "
					[:a {:href "http://todomvc.com"} "TodoMVC"]]])
		(.getElementById js/document "info")))

;; -----------------------------------------------------------------------------
;; Todo App

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
			(html
				[:div
				 	(header)
				 	[:input
				 		{:id "new-todo"
						 :ref "newField"
						 :placeholder "What needs to be done?"
						 :on-key-down #(enter-new-todo % state owner)}]
					(listing state comm)
					(footer state)]))))

;; -----------------------------------------------------------------------------
;; Root

(om/root todo-app app-state
  {:target (.getElementById js/document "todoapp")})

(render-disclaimer)