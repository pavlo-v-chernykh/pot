(ns pot.core.hand
  (:require [pot.core.bl :refer [move-left move-right move-up move-down
                                 can-take-step? add-random-cell]]))

(defn state-watcher
  [_ history]
  (fn [_ _ old new]
    (let [history-value @history
          cursor (:cursor history-value)
          prev-snapshot (get-in history-value [:snapshots (dec cursor)])
          next-snapshot (get-in history-value [:snapshots (inc cursor)])]
      (when (not= old new)
        (swap! history assoc-in [:snapshots cursor] old)
        (when (not= new prev-snapshot)
          (when (and next-snapshot (not= new next-snapshot))
            (swap! history update-in [:snapshots] (comp vec (partial take (inc cursor)))))
          (swap! history assoc-in [:snapshots (inc cursor)] new))
        (swap! history update-in [:cursor] (if (= new prev-snapshot) dec inc))))))

(defn history-watcher
  [storage key]
  (fn [_ _ old new]
    (when-not (= old new)
      (.set storage key (pr-str new)))))

(def ^:private direction-handler-map
  {:left  move-left
   :right move-right
   :up    move-up
   :down  move-down})

(defn move-handler
  [{:keys [direction]} state history]
  (let [board (:board @state)
        handler (direction direction-handler-map)
        history-value @history
        cursor (:cursor history-value)
        next-snapshot (get-in history-value [:snapshots (inc cursor)])]
    (when (can-take-step? board handler)
      (if (and next-snapshot (= direction (:direction next-snapshot)))
        (reset! state next-snapshot)
        (reset! state {:board     (-> board handler add-random-cell)
                       :direction direction})))))

(defn undo-handler
  [_ state history]
  (let [history-value @history
        prev-snapshot (get-in history-value [:snapshots (dec (:cursor history-value))])]
    (when prev-snapshot
      (reset! state prev-snapshot))))
