(ns pot.core.act
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [<! chan put!]]
            [pot.core.bl :refer [process-board mirrorv-board transpose-board add-random-cell]]))

(def direction-handler-map
  {:left  process-board
   :right (comp mirrorv-board process-board mirrorv-board)
   :up    (comp transpose-board process-board transpose-board)
   :down  (comp transpose-board mirrorv-board process-board mirrorv-board transpose-board)})

(defn move-handler
  [state {:keys [direction]}]
  (let [handler (direction direction-handler-map)]
    (swap! state update-in [:board] (comp add-random-cell handler))))

(defn game-over-handler
  [state {:keys [game-over]}]
  (swap! state assoc-in [:game-over] game-over)
  (swap! state assoc-in [:board] [[nil nil nil nil] ; todo: make it right :)
                                  ["G" "A" "M" "E"]
                                  ["O" "V" "E" "R"]
                                  [nil nil nil nil]]))

(def action-handler-map
  {:move      move-handler
   :game-over game-over-handler})

(defn run-action
  [state {action-key :msg :as msg}]
  (let [{action-handler action-key} action-handler-map]
    (action-handler state msg)))

(defn listen-channels
  [state {:keys [actions changes]}]
  (go (while true
        (alt!
          actions ([msg] (run-action state msg))
          changes ([msg] (run-action state msg))))))

(defn- can-take-step?
  [board]
  (apply not= (cons board (for [f (vals direction-handler-map)] (f board)))))

(defn- board-changes-watcher
  [{:keys [changes]}]
  (fn [_ _ _ new]
    (when-not (or (can-take-step? (:board new)) (:game-over new))
      (put! changes {:msg :game-over :game-over true}))))

(defn watch-changes
  [state channels]
  (add-watch state :board-changes (board-changes-watcher channels)))
