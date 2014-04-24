(ns pot.core.ui
  (:require [clojure.browser.repl :as repl]
            [om.core :as om]
            [goog.events :as events]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put!]]
            [pot.core.bl :refer [game-over? win?]]
            [pot.core.sys :refer [create-system start-system stop-system]])
  (:import [goog.events KeyHandler KeyCodes EventType]))

(enable-console-print!)
(repl/connect "http://localhost:8000/repl")

(def ^:private key-direction-map
  {KeyCodes.LEFT  :left
   KeyCodes.RIGHT :right
   KeyCodes.UP    :up
   KeyCodes.DOWN  :down})

(def ^:private key-action-map
  {KeyCodes.LEFT  :move
   KeyCodes.RIGHT :move
   KeyCodes.UP    :move
   KeyCodes.DOWN  :move
   KeyCodes.ESC   :undo})

(defn- translate-key-to-msg
  [key]
  (let [action (get key-action-map key)]
    (case action
      :move {:msg :move :direction (get key-direction-map key)}
      :undo {:msg :undo}
      nil)))

(defn- root-key-handler
  [_ actions e]
  (let [msg (translate-key-to-msg (.-keyCode e))]
    (when msg
      (put! actions msg))))

(def ^:private system
  (create-system
    {:width             4
     :height            4
     :init              2
     :win-value         256
     :history-store-key :history}))

(om/root
  (fn [cursor owner]
    (reify
      om/IDisplayName
      (display-name [_] "board")
      om/IWillMount
      (will-mount [_]
        (let [actions (-> (om/get-state owner) :channels :actions)]
          (events/listen
            (KeyHandler. js/document)
            KeyHandler.EventType.KEY
            #(root-key-handler @cursor actions %))))
      om/IRenderState
      (render-state [_ {:keys [config]}]
        (let [board (:board cursor)
              win-value (-> config deref :win-value)]
          (html
            [:table
             (cond
               (game-over? board) [:tbody [:tr [:td "GAME OVER"]]]
               (win? board win-value) [:tbody [:tr [:td "YOU WIN"]]]
               :else [:tbody
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
  (:state system)
  {:target     (.getElementById js/document "app")
   :init-state system})

(om/root
  (fn [_ _]
    (reify
      om/IDisplayName
      (display-name [_] "controls")
      om/IRenderState
      (render-state [_ {{:keys [actions]} :channels}]
        (html
          [:div
           [:button
            {:on-click (fn [_] (put! actions {:msg :new-game}))}
            "New Game"]]))))
  (:state system)
  {:target     (.getElementById js/document "controls")
   :init-state system})

(om/root
  (fn [cursor _]
    (reify
      om/IDisplayName
      (display-name [_] "info")
      om/IRender
      (render [_]
        (html
          [:div
           [:span (:score cursor)]]))))
  (:state system)
  {:target (.getElementById js/document "info")})

(start-system system)
(events/listen js/window EventType.UNLOAD #(stop-system system))
