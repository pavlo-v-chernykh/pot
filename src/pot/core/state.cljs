(ns pot.core.state)

(defn create-state
  [{:keys [width height]}]
  (atom {:board     (vec (repeat height (vec (repeat width nil))))
         :game-over false}))
