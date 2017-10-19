(ns engn-web.core
  "
  This module is a bit of a mess, which we will use to motivate some of the
  things that we will learn in the next few weeks.
  "
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljs-time.core :as time]
              [cljs-time.format :as time-format]
              [cljs-time.coerce :as time-coerce]
              [clojure.string :as string]
              [reagent-material-ui.core :as ui]
              [ajax.core :refer [GET POST]]))


;; ==========================================================================
;; Utility functions
;; ==========================================================================

(enable-console-print!)


;; ==========================================================================
;; App State
;;
;; This is messy right now, which will motivate something we learn
;; later in class
;; ==========================================================================

(defonce state (atom {}))

(defn handle-greeting! [result]
  (let [hello (:hello result)
        greet (:greeting result)]
    (swap! state assoc :hello hello)
    (swap! state assoc :greeting greet)))

(defn get-greeting! [name]
  ;; Send a GET request to /greet/<some name> on the server 
  ;;
  ;; Full docs on this library: https://github.com/JulianBirch/cljs-ajax
  (GET (str "/greet/" name)
       {:response-format :json
        :keywords? true
        :error-handler (fn [error] (println error) (js/alert "Error! Check the console!"))
        :handler handle-greeting!}))

(defn set-greeting! [greeting]
  ;; Send a GET request to /greet on the server with the
  ;; query param "greeting"
  ;;
  ;; Full docs on this library: https://github.com/JulianBirch/cljs-ajax
  (GET "/greet"
       {:params {:greeting greeting}
        :response-format :json
        :keywords? true
        :error-handler (fn [error] (println error) (js/alert "Error! Check the console!"))
        :handler #(js/alert "Greeting set")}))

;; ==========================================================================
;; Functions to send / receive messages and list channels
;;
;; This is messy right now, which will motivate something we learn
;; later in class
;; ==========================================================================


;; ==========================================================================
;; View components
;; ==========================================================================

(defn set-greeting-control []
  [ui/Card
    [ui/CardText
      [ui/TextField
           {:floatingLabelText "What is the greeting?"
            :onChange #(swap! state assoc :greeting-for-server %2)}]
      [ui/RaisedButton {:label "Set Server Greeting"
                        :on-click #(set-greeting! (:greeting-for-server @state))}]]])

(defn greet-control []
  [ui/Card
    [ui/CardText
      [ui/TextField
           {:floatingLabelText "Who are we greeting?"
            :onChange #(swap! state assoc :name %2)}]
      [ui/RaisedButton {:label "Greet"
                        :on-click #(get-greeting! (:name @state))}]]])

(defn main-page []
  (let [greeting (:greeting @state)
        hello    (:hello @state)]
    [ui/MuiThemeProvider ;theme-defaults
      [:div
        [set-greeting-control]
        [greet-control]
        [ui/Card
          [ui/CardText
            [:h1 (str greeting " " hello)]]]]]))



;; -------------------------
;; Routes
;; -------------------------

(def page (atom #'main-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'main-page))


;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
