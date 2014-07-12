(ns pot.core.sys
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.reader :refer [read-string]]
            [cljs.core.async :refer [chan]]
            [goog.storage.mechanism.mechanismfactory :as storage-factory]
            [pot.core.bl :refer [make-board add-random-cells]]
            [pot.core.hand :refer [process-msg]]))

(defn- create-config
  {:pre [(every? number? [height width init win-value])
         (<= init (* height width))
         (<= 32 win-value)]}
  [{:keys [height width init win-value history-store-key]}]
  (atom {:height            height
         :width             width
         :init              init
         :win-value         win-value
         :history-store-key history-store-key}))

(defn- create-state
  [{:keys [height width init]}]
  (atom {:board     (add-random-cells init (make-board height width))
         :score     0
         :direction nil}))

(defn- create-channels
  []
  {:actions (chan)})

(defn- create-history
  []
  (atom {:snapshots []
         :cursor    0}))

(defn- create-storage
  []
  (storage-factory/create))

(defn- create-runtime
  []
  (atom {:running false}))

(defn- set-running
  [{:keys [runtime] :as system} running]
  (swap! runtime assoc :running running)
  system)

(defn- store-system
  [system]
  (let [storage (:storage system)
        history (-> system :history deref)
        hkey (-> system :config deref :history-store-key)]
    (.set storage hkey history)
    system))

(defn- restore-system
  [system]
  (let [storage (:storage system)
        hkey (-> system :config deref :history-store-key)]
    (when-let [stored-history (.get storage hkey)]
      (when-let [restored-history (read-string stored-history)]
        (when-let [restored-state (get-in restored-history [:snapshots (:cursor restored-history)])]
          (-> system :state (reset! restored-state)))
        (-> system :history (reset! restored-history))))
    system))

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

(defn- state-watcher
  [{:keys [history]}]
  (fn [_ _ old new]
    (when (not= old new)
      (swap! history update-history old new))))

(defn- add-watchers
  [system]
  (let [{:keys [state]} system]
    (add-watch state :state-watcher (state-watcher system)))
  system)

(defn- remove-watchers
  [system]
  (let [{:keys [state]} system]
    (remove-watch state :state-watcher))
  system)

(defn- listen-channels
  [{{:keys [actions]} :channels :as system}]
  (go (while (-> system :runtime deref :running)
        (alt!
          actions ([msg] (process-msg system msg)))))
  system)

(defn create-system
  [options]
  (let [config (create-config options)]
    {:config   config
     :state    (create-state @config)
     :channels (create-channels)
     :history  (create-history)
     :storage  (create-storage)
     :runtime  (create-runtime)}))

(defn start-system
  [system]
  (-> system restore-system add-watchers (set-running true) listen-channels))

(defn stop-system
  [system]
  (-> system (set-running false) remove-watchers store-system))
