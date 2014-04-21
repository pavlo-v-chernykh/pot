(ns pot.core.ui
  (:require [om.core :as om]
            [goog.events :as events]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put!]]
            [pot.core.bl :refer [add-random-cell]]
            [pot.core.act :refer [listen-channels watch-changes restore-state]]
            [pot.core.state :refer [create-state create-history create-storage]]
            [pot.core.chan :refer [create-channels]])
  (:import [goog.events KeyHandler KeyCodes]))

(enable-console-print!)

(def key-direction-map
  {KeyCodes.LEFT  :left
   KeyCodes.RIGHT :right
   KeyCodes.UP    :up
   KeyCodes.DOWN  :down})

(def key-action-map
  {KeyCodes.LEFT  :move
   KeyCodes.RIGHT :move
   KeyCodes.UP    :move
   KeyCodes.DOWN  :move
   KeyCodes.ESC   :undo})

(defn translate-key-to-msg
  [key]
  (let [action (get key-action-map key)]
    (case action
      :move {:msg :move :direction (get key-direction-map key)}
      :undo {:msg :undo}
      nil)))

(defn root-key-handler
  [_ actions e]
  (let [msg (translate-key-to-msg (.-keyCode e))]
    (when msg
      (put! actions msg))))

(def app-state (create-state {:width  4
                              :height 4
                              :init   2}))
(def app-history (create-history))
(def channels (create-channels))
(def storage (create-storage))

(restore-state app-state app-history storage)

(om/root
  (fn [cursor owner]
    (reify
      om/IDisplayName
      (display-name [_] "board")
      om/IWillMount
      (will-mount [_]
        (let [actions (om/get-state owner [:channels :actions])]
          (events/listen
            (KeyHandler. js/document)
            KeyHandler.EventType.KEY
            #(root-key-handler @cursor actions %))))
      om/IRender
      (render [_]
        (let [board (:board cursor)
              game-over (:game-over cursor)
              yc (count board)
              xc (count (first board))]
          (html
            [:table
             (if game-over
               [:tbody [:tr [:td "GAME OVER"]]]
               (into
                 [:tbody]
                 (for [y (range yc)]
                   (into
                     [:tr {:key (str "tr" y)}]
                     (for [x (range xc)]
                       [:td {:style {:width      50
                                     :height     50
                                     :border     [["1px solid black"]]
                                     :text-align :center}
                             :key   (str "td" (+ (* yc y) x))}
                        (get-in board [y x])])))))])))))
  app-state
  {:target     (.getElementById js/document "app")
   :init-state {:channels channels}})

(listen-channels app-state app-history channels)
(watch-changes app-state app-history storage)
