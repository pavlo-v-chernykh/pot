(ns pot.core.state
  (:require [pot.core.bl :refer [add-random-cell]]))

(defn create-state
  {:pre [(<= init (* width height))]}
  [{:keys [width height init]}]
  (let [board (vec (repeat height (vec (repeat width nil))))]
    (atom {:board     (->> board (iterate add-random-cell) rest (take init) last)
           :game-over false})))

(defn create-history
  []
  (atom []))
