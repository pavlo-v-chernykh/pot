(ns pot.core.hand
  (:require [pot.core.bl :refer [move-left move-right move-up move-down
                                 add-random-cell add-random-cells make-board win?]]))

(def ^:private direction-handler-map
  {:left  move-left
   :right move-right
   :up    move-up
   :down  move-down})

(defn- move-handler
  [{:keys [config state history]} {:keys [direction]}]
  (let [state-value @state
        board (:board state-value)
        win-value (-> config deref :win-value)]
    (when (not (win? board win-value))
      (let [handler (direction direction-handler-map)
            processed-board (handler board)]
        (when (not= board processed-board)
          (let [history-value @history
                next-snapshot (get-in history-value [:snapshots (inc (:cursor history-value))])]
            (if (and next-snapshot (= direction (:direction next-snapshot)))
              (reset! state next-snapshot)
              (let [total-score (:score state-value)
                    step-score (-> processed-board meta :score)]
                (reset! state {:board     (add-random-cell processed-board)
                               :direction direction
                               :score     (+ total-score step-score)})))))))))

(defn- undo-handler
  [{:keys [state history]} _]
  (let [history-value @history
        prev-snapshot (get-in history-value [:snapshots (dec (:cursor history-value))])]
    (when prev-snapshot
      (reset! state prev-snapshot))))

(defn- new-game-handler
  [{:keys [config state history]} _]
  (let [{:keys [width height init]} @config]
    (reset! state {:board     (add-random-cells init (make-board height width))
                   :score     0
                   :direction nil}))
  (reset! history {:snapshots []
                   :cursor    0}))

(def ^:private msg-handler-map
  {:move     move-handler
   :undo     undo-handler
   :new-game new-game-handler})

(defn process-msg
  [system {action-key :msg :as msg}]
  (let [{action-handler action-key} msg-handler-map]
    (when action-handler
      (action-handler system msg))))
