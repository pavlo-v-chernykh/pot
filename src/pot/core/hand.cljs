(ns pot.core.hand
  (:require [pot.core.bl :refer [move-left move-right move-up move-down
                                 can-take-step? can-take-some-step? add-random-cell]]))

(def ^:private direction-handler-map
  {:left  move-left
   :right move-right
   :up    move-up
   :down  move-down})

(defn history-watcher
  [_ history]
  (fn [_ _ old new]
    (let [old-board (:board old)
          new-board (:board new)
          hval @history
          cursor (:cursor hval)
          last-board (:board (get-in hval [:snapshots (dec cursor)]))]
      (when-not (or (= old-board new-board) (= old-board last-board))
        (swap! history assoc-in [:snapshots cursor] old)
        (swap! history update-in [:cursor] inc)))))

(defn storage-watcher
  [storage key]
  (fn [_ _ _ new]
    (.set storage key (pr-str new))))

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
        (do
          (when-not (can-take-some-step? next-board (vals direction-handler-map))
            (swap! state assoc :game-over true))
          (swap! state assoc :board next-board))
        (let [take-up-to-cursor (comp vec (partial take (inc cursor)))
              new-board (-> board handler add-random-cell)]
          (swap! history update-in [:directions] take-up-to-cursor)
          (swap! history update-in [:snapshots] take-up-to-cursor)
          (swap! state assoc :board new-board)
          (when-not (can-take-some-step? new-board (vals direction-handler-map))
            (swap! state assoc :game-over true))))
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
        (swap! state assoc :game-over false))
      (swap! state assoc :board last-board)
      (add-watch state :history-watcher (history-watcher state history))
      (swap! history assoc-in [:snapshots cursor] sval)
      (swap! history update-in [:cursor] dec))))
