(ns cheap-spot.main
  (:gen-class)
  (:require [aleph.http :as http]
            [reitit.ring :as ring]
            [clj-commons.byte-streams :as bs]
            [jsonista.core :as j]
            [hiccup.core :as h]
            [ring.middleware.defaults :as defaults])
  (:import  [java.time.format DateTimeFormatter]
            [java.time OffsetDateTime Duration]
            [java.util Locale]))

(def api-url "https://api.spot-hinta.fi/CheapestPeriod/2")
(def fin-formatter (DateTimeFormatter/ofPattern "E d.M.yyy 'kello' H.mm" (Locale. "fi")))

(defn- api-get []
  (-> @(http/get api-url)
      :body
      bs/to-string
      (j/read-value j/keyword-keys-object-mapper)))

(def data {:AveragePriceWithTax 0.0478,
           :AveragePriceNoTax 0.0434,
           :DateTimeStart "2023-01-09T04:00:00+02:00",
           :DateTimeEnd "2023-01-09T05:59:59+02:00"})

(defn- format-data [{:keys [DateTimeStart AveragePriceWithTax]}]
  (let [start-date-time (OffsetDateTime/parse DateTimeStart DateTimeFormatter/ISO_OFFSET_DATE_TIME)
        price-in-c (clojure.string/replace (format "%.2f" (* AveragePriceWithTax 100)) "." ",")
        minutes-until (-> (Duration/between (OffsetDateTime/now) start-date-time)
                          (.toMinutes))
        hours-until (Math/round (/ minutes-until 60.0))]
    {:start-date-time start-date-time
     :start-date-time-formatted (.format fin-formatter start-date-time)
     :price-with-tax price-in-c
     :minutes-until minutes-until
     :hours-until hours-until}))

;; Handlers

(def style
  "body {
     font-family: Helvetica, sans-serif;
     background: #222;
     color: #ccc;
     font-size: 200%;
     display: flex;
     justify-content: center;
     align-items: center;
     text-align: center;
   }
   h1 {
     color: #cfc;
   }")

(defn home-handler [_]
  (let [{:keys [start-date-time-formatted price-with-tax hours-until]}
        (format-data (api-get))]
    {:status 200
     :headers {"Content-type" "text/html"}
     :body (h/html
            [:html
             [:head
              [:style style]]
             [:body
              [:div
               [:h2 "Halvin pörssisähkön hinta 2 tunnin ajanjaksolle alkaa"]
               [:h1 "noin " hours-until " tunnin päästä"]
               [:h2 "eli " start-date-time-formatted "."]
               [:h2 "Sähkön hinta on tuolloin " price-with-tax " c/kWh (sis alv)."]]]])}))

;; Routes and middleware

(def routes
  [["/" {:get {:handler home-handler}}]])

(def ring-opts
  {:data
   {:middleware [[defaults/wrap-defaults defaults/api-defaults]]}})

(def app
  (ring/ring-handler
   (ring/router routes ring-opts)))

;; Web server

(defonce server (atom nil))

(def port
  (-> (System/getenv "PORT")
      (or "8080")
      (Integer/parseInt)))

(defn start-server []
  (reset! server (http/start-server #'app {:port port})))

(defn stop-server []
  (when @server
    (.close ^java.io.Closeable @server)))

(defn restart-server []
  (stop-server)
  (start-server))

;; Application entrypoint

(defn -main [& args]
  (println (format "Starting webserver on port: %s." port))
  (start-server))
