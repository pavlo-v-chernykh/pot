(ns pot.core.state
  (:require [pot.core.bl :refer [add-random-cell]])
  (:import [goog.storage Storage]
           [goog.storage.mechanism HTML5LocalStorage]))

(defn create-state
  {:pre [(<= init (* width height))]}
  [{:keys [width height init]}]
  (let [board (vec (repeat height (vec (repeat width nil))))]
    (atom {:board     (->> board (iterate add-random-cell) rest (take init) last)
           :game-over false
           :direction nil})))

(defn create-history
  []
  (atom {:snapshots  []
         :cursor     0}))

(defn create-storage
  []
  (Storage. (HTML5LocalStorage.)))
