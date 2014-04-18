(ns pot.core.hand
  (:require [pot.core.bl :refer [move-left move-right move-up move-down
                                 can-take-step? add-random-cell]]))

(defn game-over-watcher
  [state _]
  (fn [_ _ _ new]
    (when-not (or (:game-over new) (can-take-step? (:board new)))
      (swap! state assoc :game-over true))))

(defn history-watcher
  [_ history]
  (fn [_ _ old new]
    (let [old-board (:board old)
          new-board (:board new)
          last-board (:board (peek @history))]
      (when-not (or (= old-board new-board) (= old-board last-board))
        (swap! history conj {:board old-board})))))

(def ^:private direction-handler-map
  {:left  move-left
   :right move-right
   :up    move-up
   :down  move-down})

(defn move-handler
  [{:keys [direction]} state _]
  (let [handler (direction direction-handler-map)]
    (when (not (:game-over @state))
      (swap! state update-in [:board] (fn [board]
                                        (let [moved (handler board)]
                                          (if (not= moved board)
                                            (add-random-cell moved)
                                            board)))))))

(defn undo-handler
  [_ state history]
  (let [snapshot (peek @history)]
    (when snapshot
      (when (:game-over @state)
        (remove-watch state :game-over-watcher)
        (swap! state assoc :game-over false)
        (add-watch state :game-over-watcher (game-over-watcher state history)))
      (remove-watch state :history-watcher)
      (swap! state assoc :board (:board snapshot))
      (add-watch state :history-watcher (history-watcher state history))
      (swap! history pop))))
