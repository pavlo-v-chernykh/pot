(ns pot.core.hand
  (:require [pot.core.bl :refer [move-left move-right move-up move-down
                                 game-over? add-random-cell]]))

(def ^:private direction-handler-map
  {:left  move-left
   :right move-right
   :up    move-up
   :down  move-down})

(defn state-watcher
  [_ history]
  (fn [_ _ old new]
    (let [history-value @history
          cursor (:cursor history-value)
          prev (get-in history-value [:snapshots (dec cursor)])]
      (when-not (= old new)
        (swap! history assoc-in [:snapshots cursor] old)
        (when-not (= new prev)
          (swap! history assoc-in [:snapshots (inc cursor)] new))
        (swap! history update-in [:cursor] (if (= new prev) dec inc))))))

(defn history-watcher
  [storage key]
  (fn [_ _ old new]
    (when-not (= old new)
      (.set storage key (pr-str new)))))

(defn move-handler
  [{:keys [direction]} state history]
  (let [handler (direction direction-handler-map)
        history-value @history
        state-value @state
        cursor (:cursor history-value)
        next-snapshot (get-in history-value [:snapshots (inc cursor)])]
    (when (not (game-over? (:board state-value)))
      (if (and next-snapshot (= direction (:direction next-snapshot)))
        (reset! state next-snapshot)
        (let [take-up-to-cursor (comp vec (partial take (inc cursor)))
              new-board (-> state-value :board handler add-random-cell)]
          (swap! history update-in [:snapshots] take-up-to-cursor)
          (reset! state {:board     new-board
                         :direction direction}))))))

(defn undo-handler
  [_ state history]
  (let [history-value @history
        last-snapshot (get-in history-value [:snapshots (dec (:cursor history-value))])]
    (when last-snapshot
      (reset! state last-snapshot))))
