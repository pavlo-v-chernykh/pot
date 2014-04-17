(ns pot.core.ui
  (:require [om.core :as om]
            [goog.events :as events]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put!]]
            [pot.core.bl :refer [add-random-cell]]
            [pot.core.act :refer [listen-channels watch-changes]]
            [pot.core.state :refer [create-state]]
            [pot.core.chan :refer [create-channels]])
  (:import [goog.events KeyHandler KeyCodes]))

(enable-console-print!)

(def app-state (create-state {:width  4
                              :height 4}))

(def chans (create-channels))

(def key-direction-map
  {KeyCodes.LEFT  :left
   KeyCodes.RIGHT :right
   KeyCodes.UP    :up
   KeyCodes.DOWN  :down})

(defn translate-key-to-msg
  [key]
  (let [direction (get key-direction-map key)]
    (cond
      direction {:msg :move :direction direction})))

(defn root-key-handler
  [{:keys [game-over]} actions e]
  (let [msg (translate-key-to-msg (.-keyCode e))]
    (when (and msg (not game-over))
      (put! actions msg))))

(om/root
  (fn [cursor owner]
    (reify
      om/IDisplayName
      (display-name [_] "board")
      om/IWillMount
      (will-mount [_]
        (let [actions (om/get-state owner [:chans :actions])]
          (events/listen
            (KeyHandler. js/document)
            KeyHandler.EventType.KEY
            #(root-key-handler @cursor actions %))))
      om/IRender
      (render [_]
        (let [board (:board cursor)
              yc (count board)
              xc (count (first board))]
          (html
            (into
              [:table]
              (for [y (range yc)]
                (into
                  [:tr {:key (str "tr" y)}]
                  (for [x (range xc)]
                    [:td {:style {:width      50
                                  :height     50
                                  :border     [["1px solid black"]]
                                  :text-align :center} ; todo move to css
                          :key   (str "td" (+ (* yc y) x))}
                     (get-in board [y x])])))))))))
  app-state
  {:target (. js/document (getElementById "app"))
   :init-state {:chans chans}})

(listen-channels app-state chans)
(watch-changes app-state chans)

(swap! app-state update-in [:board] add-random-cell) ; todo mote it out of here
(swap! app-state update-in [:board] add-random-cell)
