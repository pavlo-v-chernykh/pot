(ns pot.core.act
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [pot.core.hand :refer [move-handler undo-handler game-over-watcher history-watcher]]))

(def ^:private msg-handler-map
  {:move      move-handler
   :undo      undo-handler})

(defn- process-msg
  [{action-key :msg :as msg} state history]
  (let [{action-handler action-key} msg-handler-map]
    (action-handler msg state history)))

(defn listen-channels
  [state history {:keys [actions]}]
  (go (while true
        (alt!
          actions ([msg] (process-msg msg state history))))))

(defn watch-changes
  [state history]
  (add-watch state :game-over-watcher (game-over-watcher state history))
  (add-watch state :history-watcher (history-watcher state history)))
