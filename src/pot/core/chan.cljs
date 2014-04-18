(ns pot.core.chan
  (:require [cljs.core.async :refer [chan]]))

(defn create-channels
  []
  {:actions (chan)
   :changes (chan)
   :history (chan)})

