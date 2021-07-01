(ns live-opts.state
  (:require
   [reagent.core :as r]))

(def state
  (r/atom
   {:cur-results nil
    :cur-sort nil
    :cur-quotes nil
    :cur-catalysts nil
    :cur-visible-quotes #{}
    :ladders {} ;; Map[[Ticker, expiration, opttype], Map[Contract, data]]
    :spreads #{} ;; Set[start CS]
    :status :loading}))
(defn print-opts-ex [] (-> @state :cur-results first println))
(defn print-quotes-ex [] (-> @state :cur-quotes first println))
(defn print-catalysts-ex [] (-> @state :cur-catalysts first println))
(defn print-spreads [] (-> @state :spreads println))
(defn print-ladders [] (-> @state :ladders println))
