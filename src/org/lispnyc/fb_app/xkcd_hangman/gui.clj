;; modeled after the com.twinql.clojure.facebook's test application

;; Developer's note: this is using Compojure 0.3 which is a bit outdated
;; some of the existing utilities exist because of that.  Note, once
;; brought into 0.4 it'll be about 50% smaller.

(ns org.lispnyc.fb-app.xkcd-hangman.gui
  (:use org.lispnyc.fb-app.xkcd-hangman.game
        clojure.set
        compojure)
  (:require [com.twinql.clojure.facebook :as fb])
  (:gen-class)) ; required for main

(def *users-filename*       "users.data")
(def *users-stats*          "users.log")
(def *fb-url*               "http://apps.facebook.com/xkcd-hangman/")
(def *canvas-root*          "/xhm/")
(def *play-game-with-guess* (str *canvas-root* "play-game-with-guess"))
(def *comic-url*            "http://imgs.xkcd.com/comics/")

;; Trivial storage of logged-in users.
(def *users* (ref {}))

;; Request debugging.
(def *print-fb-params?* true)

;; Bind the session information for this application.
;; Use this macro around any code you want to make API
;; calls.
(defmacro with-appname [& body]
  `(fb/with-fb-keys ["secret"
                     "secret"]
     ~@body))

;; Common processing of params.
;; Note: no error handling in param verification!
;; It might be useful to redirect people to the app's canvas page when
;; signature verification fails.
(defmacro with-fb-params [m & body]
  `(with-appname
     (let [~'params (fb/fb-params ~'params)
           
           ;; Bind a predetermined set of labels to functions of the processed
           ;; parameters.
           ~@(when m
               (mapcat (fn [[bind-to f]]
                         (list bind-to (list f 'params)))
                       m))]
       (when *print-fb-params?*
         (prn ~'params))
       (fb/with-session-key ~'params
         ~@body))))

(defn persist-users! [users]
  (with-open [f (java.io.FileWriter. *users-filename*)]
    (.write f (prn-str users))))

(defn persist-stats! [s]
  (dosync
   (with-open [f (java.io.FileWriter. *users-stats* true)]
     (.write f (prn-str s)))))

(defn contains-login-params? [params]
  (and (or (contains? params :fb_sig_authorize)
           (contains? params :fb_sig_added))
       (contains? params :fb_sig_expires)))
  
;; Take the parameters received by the post-auth handler,
;; mutating the users map.
(defn handle-user-login
  "Returns whether the user is authorized."
  [params]
  (when (contains-login-params? params)
    (println "# ... handling user login with params:")
    (prn (select-keys params [:fb_sig_user, :fb_sig_authorize, :fb_sig_expires]))
    
    (let [user        (:fb_sig_user params)
          authorized? (or (:fb_sig_authorize params)
                          (:fb_sig_added params))    ; Ah, Facebook, your APIs are so shoddy.
          until       (:fb_sig_expires params)]
      
      ;; No need to verify the parameters -- process-params already did.
      (when user
        (persist-users! 
          (dosync
            (if authorized?
              (alter *users* assoc user until)
              (alter *users* dissoc user))))
        authorized?))))

;; Checks the user from the parameters against the expiry in the users map.
(defn user-logged-in? [params]
  (let [expires (get @*users* (:fb_sig_user params) 0)]
    (> (* expires 1000)
       (System/currentTimeMillis))))

(defn user-just-logged-in? [params]
  (true? (:installed params)))

(defn http-redirect
  ([url] (http-redirect url 0))
  ([url refresh]
     (let [content (str refresh ";url=" url)]
       (html [:meta {:http-equiv "REFRESH" :content content}]))))

(defn page-start [params]
  (html
   [:p]
   [:form {:name "input" :action *play-game-with-guess* :method "POST"}
    (drop-down "category" (list-categories))
    [:input {:type "submit" :value "play again!"}]
    ]
   ))

(defn page-footer [params]
  [:p][:p][:p]
  [:center
   [:hr]
   [:h5 "Content courtesy of " [:a { :href "http://xkcd.com"} "XKCD"] [:br]
    [:a {:href "https://github.com/heow/clj-facebook/blob/master/src/com/agi/fb_app/xkcd_hangman/gui.clj" :target "_new"} "view Clojure source" ]]
   ])

(defn page-play-game-with-guess [params]
  (let [guesses           (str (:guess params) (:guesses params) " ") ; seed guesses with a space, it's free
        random-word       (select-word-by-category (:category params))
        word              (if (contains? params :word) (:word params)
                            (:word random-word))
        hint              (if (contains? params :hint) (:hint params)
                              (:hint random-word))
        pic               (if (contains? params :pic) (:pic params)
                              (:pic random-word))
        category          (if (and (contains? params :category)
                                   (not (= *category-any* (:category params))))
                            (:category params)
                            (:category random-word))
        incorrect-guesses (apply str (difference (set guesses)
                                                 (set (.toUpperCase word))))
        ]
    (html
     [:center
      [:h3 category]
      [:img {:src (str *canvas-root* "images/hm" (count-incorrect word guesses) ".gif")
             :size "234x171" }]
      [:h1 (str ""
                (apply str
                       (interpose " "
                                 (replace { \space "&nbsp;"}
                                           (visualize word guesses)))))]]
     (if (word-complete? word guesses)
       (html ; win
        (persist-stats! params)
        [:center
         [:h1 "Woohoo!"]
         [:img {:src (str *comic-url* pic) }]]
        [:center (page-start params)]
        (page-footer params))
       (if (< 5 (count-incorrect word guesses))
         (html ; lose
          (persist-stats! params)
          [:center
           [:h1 "Sorry :-("]
           [:h3 "The answer is \"" word "\""]
           [:img {:src (str *comic-url* pic) }]]
          [:center (page-start params)]
          (page-footer params))
         (html ; normal play
          (if (and  (> (count hint) 0) (> (count-incorrect word guesses) 1))
            [:center [:h3 (str "(hint: " hint ")")]])
          [:center [:h3 (str "" (apply str (interpose " " incorrect-guesses)))]
           [:form {:name "input" :action *play-game-with-guess* :method "POST"}
            ;; state is on the screen
            [:input {:type "hidden" :name "fb_sig_user" :value (:fb_sig_user params)}]
            [:input {:type "hidden" :name "word"     :value word}]
            [:input {:type "hidden" :name "hint"     :value hint}]
            [:input {:type "hidden" :name "pic"      :value pic}]
            [:input {:type "hidden" :name "guesses"  :value guesses}]
            [:input {:type "hidden" :name "category" :value category}]
            ;; buttons
            (map #(html [:input {:name "guess" :type "submit" :value %1}])
                 (difference (set "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                             (set guesses))) ]]
          (page-footer params)))))))
    
(defroutes game-app
  (POST *play-game-with-guess*
        (println "# play-game-with-guess: " params)
        (with-fb-params {}
          (page-play-game-with-guess params)))
  (GET (str *canvas-root* "canvas/")
       (println "# Accessed main canvas page.")
       (with-fb-params {}
         (handle-user-login params)
         (fb/with-new-fb-session []
           (if (user-just-logged-in? params)
             ;; TODO: validate login?
             ;; http://developers.facebook.com/docs/authentication/fb_sig
             (http-redirect *fb-url*)
             (html
              [:h1 "XKCD Hangman"]
              (if (user-logged-in? params)
                ;; (page-start params)
                (page-play-game-with-guess params)
                (html
                 [:p [:a {:target "_parent" ;; <http://www.maybefriday.com/blog/2009/07/facebook-apps-and-iframes/>
                          :href (fb/login-url)}
                      "First log in then we can get started."]])))))))

  ;; (route/files *canvas-root* {:root "html/"}) 
  ;; ...is replaced by the pain of Compojure 0.3:
  (GET "/xhm/images/hm0.gif" (serve-file "html/images" "hm0.gif"))
  (GET "/xhm/images/hm1.gif" (serve-file "html/images" "hm1.gif"))
  (GET "/xhm/images/hm2.gif" (serve-file "html/images" "hm2.gif"))
  (GET "/xhm/images/hm3.gif" (serve-file "html/images" "hm3.gif"))
  (GET "/xhm/images/hm4.gif" (serve-file "html/images" "hm4.gif"))
  (GET "/xhm/images/hm5.gif" (serve-file "html/images" "hm5.gif"))
  (GET "/xhm/images/hm6.gif" (serve-file "html/images" "hm6.gif"))
  
  (GET "/*"
       (with-params
         (println "# Accessed default page: " params)
         (html
          [:h1 "404 not found"]
          ))))

(defmacro ignore-errors [& body]
  `(try
     (do
       ~@body)
     (catch Throwable e#
       nil)))

(defn start-server [host port]
  (if (string? port) (start-server host (Integer/parseInt port)) ; rerun as int
      (compojure.server.jetty/run-server {:port port} ; 0.3-isim
                  "/*" (servlet game-app))))

(defn -main [& args] ; required for main
  (println
   "users: "
   (dosync
    (ref-set *users*
             (or
              (ignore-errors
               (read-string
                (slurp *users-filename*)))
              {}))))
  (if (nil? args)
    (start-server "localhost" "8888")
    (start-server (first args) (first (rest args))) ))

;; to run in swank's REPL:
(comment
  (do
    (in-ns 'org.lispnyc.fb-app.xkcd-hangman.gui)
    (defonce server (start-server "localhost" "8888")))
  (.stop server)
  (.start server)
  )
