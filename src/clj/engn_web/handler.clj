(ns engn-web.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [config.core :refer [env]]
            [engn-web.middleware :refer [wrap-middleware]]
            [hiccup.page :refer [include-js include-css html5]]
            [ring.middleware.json :as json]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]))

;; ==========================================================================
;; Utility functions for serving up a JSON REST API
;; ==========================================================================

(def json-header
  "Utility function to set the appropriate headers in an
   HTTP response to return JSON to the client"
  {"Content-Type" "application/json"})

(defn json
  "Utility function to return JSON to the client"
  [data]
  {:status 200 :headers json-header :body data})

;; ==========================================================================
;; Functions to render the HTML for the single-page application
;; ==========================================================================

(def mount-target
  "This is the page that is displayed before figwheel
   compiles your application"
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler (this page may self-destruct)"]])

(defn head []
  "Function to generate the <head> tag in the main HTML page that is
   returned to the browser"
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "https://fonts.googleapis.com/icon?family=Material+Icons")
   (include-css "https://fonts.googleapis.com/css?family=Roboto:300,400,500")
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn main-page
   "Generates the main HTML page for the single-page application"
   []
   (html5
     (head)
     [:body
       mount-target
       (include-js "/js/app.js")]))

;; ==========================================================================
;; Functions to setup the list of URI routes for the application
;; and setup the appropriate middleware wrappers
;; ==========================================================================

(defonce state (atom {:greeting "No greeting set yet..."}))

(defn echo-handler [ping pong]
  (json {:ping ping :pong pong}))

(defn greet-handler [name]
  (json {:hello name :greeting (:greeting @state)}))

(defn set-greeting-handler [greeting]
  (swap! state assoc :greeting greeting)
  (json @state))


;; ==========================================================================
;; Functions to setup the list of URI routes for the application
;; and setup the appropriate middleware wrappers
;; ==========================================================================

;; This section of the code defines how requests from your browser are routed
;; to function calls.
;;
;; Complete documentation on everything you can do with the routing is available
;; here: https://github.com/weavejester/compojure/wiki/Routes-In-Detail
;;
(defroutes routes
  (GET "/" request (main-page))
  (GET "/greet/:name" [name] (greet-handler name))
  (GET "/greet" [greeting] (set-greeting-handler greeting))
  ;; Parameters can be extracted by a route in two ways;
  ;;
  ;; 1. Parameters can be in the path and denoted by ":" before them, such
  ;;    such as ":ping" in the route below. Any request to /echo/<some string>,
  ;;    such as http://localhost:3450/echo/bob or http://localhost:3450/echo/jim
  ;;    will be routed here and the "ping" will be extracted from the url
  ;;
  ;; 2. Parameters can be passed in the query string, like "pong" in the
  ;;    echo route. Query parameters can be sent from your browser by
  ;;    adding "?parameter=value" at the end of the URL, like this:
  ;;    http://localhost:3450/echo/paul?pong=bonjour
  ;;
  (GET "/echo/:ping" [ping pong] (echo-handler ping pong))
  ;; Routes down here handle static files and not found
  (resources "/")
  (not-found "Not Found"))


(def app (->  (wrap-middleware #'routes)
              (json/wrap-json-response)
              (json/wrap-json-params)
              wrap-params
              wrap-cookies))
