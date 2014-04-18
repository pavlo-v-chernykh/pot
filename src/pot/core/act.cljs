(ns pot.core.act
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [<! chan put!]]
            [pot.core.bl :refer [process-board mirrorv-board transpose-board add-random-cell]]))

(def ^:private direction-handler-map
  {:left  process-board
   :right (comp mirrorv-board process-board mirrorv-board)
   :up    (comp transpose-board process-board transpose-board)
   :down  (comp transpose-board mirrorv-board process-board mirrorv-board transpose-board)})

(defn- can-take-step?
  [board]
  (apply not= (cons board (for [f (vals direction-handler-map)] (f board)))))

(defn- game-over-watcher
  [_ _ {:keys [changes]}]
  (fn [_ _ _ new]
    (when (and (not (:game-over new)) (not (can-take-step? (:board new))))
      (put! changes {:msg :game-over}))))

(defn- history-watcher
  [_ _ {:keys [history]}]
  (fn [_ _ old new]
    (when (not= (:board old) (:board new))
      (put! history {:msg :history :action :push :snapshot old}))))

(defn watch-changes
  [state state-history channels]
  (add-watch state :game-over-watcher (game-over-watcher state state-history channels))
  (add-watch state :history-watcher (history-watcher state state-history channels)))

(defn- move-handler
  [{:keys [direction]} state _ _]
  (let [handler (direction direction-handler-map)]
    (when (not (:game-over @state))
      (swap! state update-in [:board] (fn [board]
                                        (let [moved (handler board)]
                                          (if (not= moved board)
                                            (add-random-cell moved)
                                            board)))))))

(defn- undo-handler
  [_ state state-history channels]
  (let [snapshot (peek @state-history)]
    (when snapshot
      (when (:game-over @state)
        (remove-watch state :game-over-watcher)
        (swap! state assoc :game-over false)
        (add-watch state :game-over-watcher (game-over-watcher state state-history channels)))
      (remove-watch state :history-watcher)
      (swap! state assoc :board (:board snapshot))
      (add-watch state :history-watcher (history-watcher state state-history channels))
      (swap! state-history pop))))

(defn- game-over-handler
  [_ state _ _]
  (swap! state assoc :game-over true))

(defn- history-push-handler
  [{:keys [snapshot]} _ state-history _]
  (let [top (peek @state-history)]
    (when (and snapshot (not= snapshot top))
      (swap! state-history conj snapshot))))

(def ^:private history-handler-map
  {:push history-push-handler})

(defn- history-handler
  [{action-key :action :as msg} & opts]
  (let [{action-handler action-key} history-handler-map]
    (apply action-handler msg opts)))

(def ^:private msg-handler-map
  {:move      move-handler
   :undo      undo-handler
   :game-over game-over-handler
   :history   history-handler})

(defn- process-msg
  [{action-key :msg :as msg} & opts]
  (let [{action-handler action-key} msg-handler-map]
    (apply action-handler msg opts)))

(defn listen-channels
  [state state-history {:keys [actions changes history] :as channels}]
  (go (while true
        (alt!
          actions ([msg] (process-msg msg state state-history channels))
          changes ([msg] (process-msg msg state state-history channels))
          history ([msg] (process-msg msg state state-history channels))))))
