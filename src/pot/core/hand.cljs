(ns pot.core.hand
  (:require [pot.core.bl :refer [move-left move-right move-up move-down add-random-cell]]
            [pot.core.comp :refer [init-state init-history]]))

(defn- update-history
  [history old new]
  (let [cursor (:cursor history)
        prev-snapshot (get-in history [:snapshots (dec cursor)])
        next-snapshot (get-in history [:snapshots (inc cursor)])]
    (letfn [(save-old
              [history]
              (assoc-in history [:snapshots cursor] old))
            (truncate
              [history]
              (if (and next-snapshot (not= new prev-snapshot) (not= new next-snapshot))
                (update-in history [:snapshots] (comp vec (partial take (inc cursor))))
                history))
            (save-new
              [history]
              (if (not= new prev-snapshot)
                (assoc-in history [:snapshots (inc cursor)] new)
                history))
            (update-cursor
              [history]
              (update-in history [:cursor] (if (= new prev-snapshot) dec inc)))]
      (-> history save-old truncate save-new update-cursor))))

(defn state-watcher
  [_ history]
  (fn [_ _ old new]
    (when (not= old new)
      (swap! history update-history old new))))

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
  (let [state-value @state
        board (:board state-value)
        processed-board ((direction direction-handler-map) board)
        history-value @history
        next-snapshot (get-in history-value [:snapshots (inc (:cursor history-value))])]
    (when (not= board processed-board)
      (if (and next-snapshot (= direction (:direction next-snapshot)))
        (reset! state next-snapshot)
        (reset! state {:board     (add-random-cell processed-board)
                       :direction direction
                       :score     (+ (:score state-value) (-> processed-board meta :score))})))))

(defn undo-handler
  [_ state history]
  (let [history-value @history
        prev-snapshot (get-in history-value [:snapshots (dec (:cursor history-value))])]
    (when prev-snapshot
      (reset! state prev-snapshot))))

(defn new-game-handler
  [{:keys [width height init]} state history]
  (reset! state (init-state {:width width :height height :init init}))
  (reset! history (init-history)))
