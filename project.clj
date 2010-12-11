(defproject xkcd-hangman "1.0.0"
  :description "XKCD Inspired Hangman game as a Facebook application."
  :main org.lispnyc.fb-app.xkcd-hangman.gui
  :dependencies [
                [org.clojure/clojure "1.2.0"]
                [org.clojure/clojure-contrib "1.2.0"]
                [com.twinql.clojure/facebook "1.2.2"]                
                [compojure "0.3.2"]
                ]
  :dev-dependencies [
                    [swank-clojure "1.3.0-SNAPSHOT"]
                    [lein-run "1.0.1-SNAPSHOT"]
                    ])

