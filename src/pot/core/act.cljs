(ns pot.core.act
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.reader :refer [read-string]]
            [pot.core.hand :refer [move-handler undo-handler history-watcher storage-watcher]]))

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
  (add-watch state :history-watcher (history-watcher state history))
  (add-watch state :storage-watcher (storage-watcher storage :state))
  (add-watch history :storage-watcher (storage-watcher storage :history)))

(defn restore-state
  [state history storage]
  (when-let [stored-state (.get storage :state)]
    (when-let [restored-state (read-string stored-state)]
      (when-not (:game-over restored-state)
        (reset! state restored-state)
        (when-let [stored-history (.get storage :history)]
          (when-let [restored-history (read-string stored-history)]
            (reset! history restored-history)))))))
