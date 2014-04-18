(ns pot.core.bl
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require [cljs.core.match]))

(defn find-empty
  [board]
  (apply
    concat
    (for [y (range (count board))]
      (for [x (range (count (first board))) :when (nil? (get-in board [y x]))]
        [y x]))))

(defn add-cell
  [board location]
  (assoc-in board location (if (< (rand) 0.9) 2 4)))

(defn add-random-cell
  [board]
  (let [empty-cells (find-empty board)]
    (if (seq empty-cells)
      (add-cell board (rand-nth empty-cells))
      board)))

(defn process-pair
  [f s]
  (match [f s]
         [nil nil]             [nil]
         [f nil]               [f]
         [nil s]               [s]
         [f s :guard #(= f s)] [(+ f s) nil]
         :else                 [f s]))

(defn compress-row
  [row]
  (reduce
    (fn [a v]
      (apply conj
             (if (seq a) (pop a) a)
             (process-pair (peek a) v)))
    []
    row))

(defn process-row
  [row]
  (let [compressed (compress-row row)
        missing-count (- (count row) (count compressed))]
    (concat compressed (repeat missing-count nil))))

(defn process-board
  [board]
  (vec (map (comp vec process-row) board)))

(defn mirrorv-board
  [board]
  (vec (map (comp vec reverse) board)))

(defn transpose-board
  [board]
  (apply mapv vector board))
