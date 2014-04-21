(ns pot.core.act
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.reader :refer [read-string]]
            [pot.core.hand :refer [move-handler undo-handler state-watcher history-watcher]]))

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
  [state history storage]
  (add-watch state :state-watcher (state-watcher state history))
  (add-watch history :history-watcher (history-watcher storage :history)))

(defn restore-state
  [state history storage]
  (when-let [stored-history (.get storage :history)]
    (when-let [restored-history (read-string stored-history)]
      (reset! state (get-in restored-history [:snapshots (:cursor restored-history)]))
      (reset! history restored-history))))
