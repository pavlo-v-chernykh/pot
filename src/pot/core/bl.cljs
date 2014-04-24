(ns pot.core.bl
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require [cljs.core.match]))

(defn make-board
  [height width]
  (vec (repeat height (vec (repeat width nil)))))

(defn find-empty
  [board]
  (apply
    concat
    (map-indexed
      (fn [y row]
        (keep-indexed
          (fn [x cell]
            (when-not cell [y x]))
          row))
      board)))

(defn add-cell
  [board location]
  (assoc-in board location (if (< (rand) 0.9) 2 4)))

(defn add-random-cell
  [board]
  (let [empty-cells (find-empty board)]
    (if (seq empty-cells)
      (add-cell board (rand-nth empty-cells))
      board)))

(defn add-random-cells
  [n board]
  (->> board (iterate add-random-cell) rest (take n) last))

(defn process-pair
  [f s]
  (match [f s]
         [nil nil]             (with-meta [nil]         {:score 0})
         [f nil]               (with-meta [f]           {:score 0})
         [nil s]               (with-meta [s]           {:score 0})
         [f s :guard #(= f s)] (with-meta [(+ f s) nil] {:score (+ f s)})
         :else                 (with-meta [f s]         {:score 0})))

(defn compress-row
  [row]
  (reduce
    (fn [a v]
      (let [pair (process-pair (peek a) v)]
        (with-meta
          (apply conj (if (seq a) (pop a) a) pair)
          {:score (+ (-> a meta :score)
                     (-> pair meta :score))})))
    (with-meta [] {:score 0})
    row))

(defn process-row
  [row]
  (let [compressed-row (compress-row row)]
    (with-meta
      (vec (concat compressed-row (repeat (- (count row) (count compressed-row)) nil)))
      {:score (-> compressed-row meta :score)})))

(defn process-board
  [board]
  (let [processed-board (mapv process-row board)]
    (with-meta
      processed-board
      {:score (apply + (map (comp :score meta) processed-board))})))

(defn mirrorv-board
  [board]
  (with-meta
    (mapv (comp vec reverse) board)
    {:score (-> board meta :score)}))

(defn transpose-board
  [board]
  (with-meta
    (apply mapv vector board)
    {:score (-> board meta :score)}))

(def move-left process-board)
(def move-right (comp mirrorv-board process-board mirrorv-board))
(def move-up (comp transpose-board process-board transpose-board))
(def move-down (comp transpose-board mirrorv-board process-board mirrorv-board transpose-board))

(defn can-take-step?
  [board stepper]
  (not= board (stepper board)))

(defn can-take-some-step?
  [board steppers]
  (some (partial can-take-step? board) steppers))

(defn game-over?
  [board]
  (not (can-take-some-step? board [move-left move-right move-up move-down])))
