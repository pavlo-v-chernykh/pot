(ns pot.core.hand
  (:require [pot.core.bl :refer [move-left move-right move-up move-down
                                 can-take-step? can-take-some-step? add-random-cell]]))

(def ^:private direction-handler-map
  {:left  move-left
   :right move-right
   :up    move-up
   :down  move-down})

(defn game-over-watcher
  [state _]
  (fn [_ _ _ new]
    (when-not (or (:game-over new) (can-take-some-step? (:board new) (vals direction-handler-map)))
      (swap! state assoc :game-over true))))

(defn history-watcher
  [_ history]
  (fn [_ _ old new]
    (let [old-board (:board old)
          new-board (:board new)
          hval @history
          cursor (:cursor hval)
          last-board (:board (get-in hval [:snapshots (dec cursor)]))]
      (when-not (or (= old-board new-board) (= old-board last-board))
        (swap! history assoc-in [:snapshots cursor] {:board old-board})
        (swap! history update-in [:cursor] inc)))))

(defn move-handler
  [{:keys [direction]} state history]
  (let [handler (direction direction-handler-map)
        hval @history sval @state
        game-over (:game-over sval)
        board (:board sval)
        cursor (:cursor hval)
        next-board (get-in hval [:snapshots (inc cursor) :board])
        cur-dir (get-in hval [:directions cursor])]
    (when (and (not game-over) (can-take-step? board handler))
      (if (= cur-dir direction)
        (swap! state assoc :board next-board)
        (let [take-up-to-cursor (comp vec (partial take (inc cursor)))]
          (swap! history update-in [:directions] take-up-to-cursor)
          (swap! history update-in [:snapshots] take-up-to-cursor)
          (swap! state update-in [:board] (comp add-random-cell handler))))
      (swap! history assoc-in [:directions cursor] direction))))

(defn undo-handler
  [_ state history]
  (let [hval @history sval @state
        cursor (:cursor hval)
        last-board (get-in hval [:snapshots (dec cursor) :board])
        game-over (:game-over sval)
        board (:board sval)]
    (when last-board
      (remove-watch state :history-watcher)
      (when game-over
        (remove-watch state :game-over-watcher)
        (swap! state assoc :game-over false)
        (add-watch state :game-over-watcher (game-over-watcher state history)))
      (swap! state assoc :board last-board)
      (add-watch state :history-watcher (history-watcher state history))
      (swap! history assoc-in [:snapshots cursor] {:board board})
      (swap! history update-in [:cursor] dec))))
