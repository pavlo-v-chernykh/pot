(ns pot.core.ui
  (:require [clojure.browser.repl :as repl]
            [om.core :as om]
            [goog.events :as events]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put!]]
            [pot.core.bl :refer [game-over? add-random-cell]]
            [pot.core.act :refer [listen-channels watch-changes restore-state]]
            [pot.core.comp :refer [create-state create-channels create-history create-storage]])
  (:import [goog.events KeyHandler KeyCodes]))

(enable-console-print!)
(repl/connect "http://localhost:8000/repl")

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

(def app-config {:width 4 :height 4 :init 2})
(def app-state (create-state app-config))
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
        (let [board (:board cursor)]
          (html
            [:table
             (if (game-over? board)
               [:tbody [:tr [:td "GAME OVER"]]]
               [:tbody
                (map-indexed
                  (fn [y row]
                    [:tr {:key (str "tr" y)}
                     (let [rc (count row)]
                       (map-indexed
                         (fn [x cell]
                           [:td {:style {:width      50
                                         :height     50
                                         :border     [["1px solid black"]]
                                         :text-align :center}
                                 :key   (str "td" (+ (* rc y) x))}
                            cell])
                         row))])
                  board)])])))))
  app-state
  {:target     (.getElementById js/document "app")
   :init-state {:channels channels}})

(om/root
  (fn [_ _]
    (reify
      om/IDisplayName
      (display-name [_] "controls")
      om/IRenderState
      (render-state [_ {{:keys [actions]}           :channels
                        {:keys [height width init]} :config}]
        (html
          [:div
           [:button
            {:on-click (fn [_] (put! actions {:msg :new-game :width width :height height :init init}))}
            "New Game"]]))))
  app-state
  {:target     (.getElementById js/document "controls")
   :init-state {:channels channels :config app-config}})

(listen-channels app-state app-history channels)
(watch-changes app-state app-history storage)
