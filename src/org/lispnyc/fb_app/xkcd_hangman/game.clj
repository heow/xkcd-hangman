(ns org.lispnyc.fb-app.xkcd-hangman.game
  (:use clojure.set))

(defn word-complete? [target-word guesses]
  "Expects a string and an array of chars"
  (= 0
     (count (difference (set (.toUpperCase target-word))
                        (set (.toUpperCase (str guesses)))))))
                          
(defn visualize [target-word guesses]
  "For GUI display of missing letters."
  (apply str (map #(if (contains? (set (.toUpperCase guesses)) %1) %1 "_")
                  (.toUpperCase target-word))))

(defn count-incorrect [target-word guesses]
  "Count the incorrect guesses, spaces don't count."
  (count (difference (set (.toUpperCase
                           (apply str (filter #(not (= \space %1)) guesses))))
                     (set (.toUpperCase target-word)) )))

(def *random-words*
     [
      { :category "Irreverent"  :word "Hitler"       :pic "hitler.jpg" :hint "he was rejected from art school" }
      { :category "Surreal"     :word "nightmare"    :pic "nightmares.png" :hint "dream" }
      { :category "Tech"        :word "tech support" :pic "tech_support_cheat_sheet.png" :hint "" }
      { :category "Tech"        :word "lisp"         :pic "lisp_cycles.png" :hint "it's over 50 years old" }
      { :category "Tech"        :word "keyboard"     :pic "keyboards_are_disgusting.png" :hint "computer part" }
      { :category "Pet Peeve"   :word "reading"      :pic "pet_peeve_114.png" :hint "what you do on Saturday night" }
      { :category "Grammar"     :word "literally"    :pic "literally.png" :hint "or do you mean figuratively" }
      { :category "Grammar"     :word "affect"       :pic "effect_an_effect.png" :hint "not effect" }
      { :category "Math"        :word "np complete"  :pic "np_complete.png" :hint "complexity theory" }
      { :category "Surreal"     :word "elevator"     :pic "elevator.jpg" :hint "going up?" }
      { :category "Irreverent"  :word "thats what she said" :pic "thats_what_she_said.png" :hint "" }
      { :category "Hollidays"   :word "halloween"    :pic "october_30th.png" :hint "" }
      { :category "Programming" :word "work"         :pic "not_enough_work.png" :hint "9 to 5" }
      { :category "Geek"        :word "laptop"       :pic "morning_routine.png" :hint "" }
      { :category "Love"        :word "useless" :pic "useless.jpg" :hint "My normal approach is ... here" }
      { :category "Programming" :word "comment"      :pic "commented.png" :hint "remark" }
      { :category "Programming" :word "regular expression" :pic "regular_expressions.png" :hint "^[a-z]$" }
      { :category "Geek"        :word "su doku"      :pic "su_doku.jpg" :hint "game" }
      { :category "Geek"        :word "campfire"     :pic "campfire.png" :hint "outdoors" }
      { :category "Math"        :word "universe factory" :pic "pi.jpg" :hint "Help, I'm trapped in a..." }
      { :category "Geek"        :word "firefox" :pic "perspective.png" :hint "browser" }
])

(def *category-any* "any category")

(defn list-categories []
  (conj (sort (set (map #(:category %1) *random-words*))) *category-any*))

(defn select-word-by-category [category]
  "any or no category is random"
  (if (or (= category *category-any*)
          (= category nil))
    (rand-nth *random-words*)
    (rand-nth (filter #(= category (:category %1)) *random-words*))))

;; borrowed from rosettacode.org
(let [A (into #{} "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
      A-map (zipmap A (take 52 (drop 26 (cycle A))))]     
 
  (defn rot13[in-str]
    (reduce str (map #(if (A %1) (A-map %1) %1)  in-str))))
