(ns math.generic
  (:refer-clojure :rename {type core-type})
  (:gen-class))

(def empty-dtree {:steps {} :stop nil})

(def the-operator-table (atom {}))

(defn dtree-insert [{:keys [steps stop] :as dtree} op [predicate & predicates]]
  (if predicate
    ;; if there is another predicate, the next dtree is either the one
    ;; that governs this predicate at this stage, or a new empty one.
    (let [next-dtree (or (steps predicate) empty-dtree)]
      ;; augment the binding at this level
      (assoc dtree :steps
             (assoc steps predicate (dtree-insert next-dtree op predicates))))
    ;; no more predicates? store the current stop function.
    (do
      (if stop (prn "overwriting a binding!!"))
      (assoc dtree :stop op))))

(defn dtree-lookup [{:keys [steps stop]} [argument & arguments]]
  (if argument
    ;; take a step, if we can...that means finding a predicate
    ;; that matches at this step and seeing if the subordinate dtree
    ;; also matches. The first binding that matches this pair of
    ;; conditions is chosen.
    (some identity (map (fn [[step dtree]]
                          (and (step argument)
                               (dtree-lookup dtree arguments))) steps))
    ;; otherwise we stop here.
    stop))

(defn defhandler [operator predicates f]
  ;; what to do here? install ourselves in the global dtree.
  (swap! the-operator-table (fn [operator-table]
                              (let [dtree (get operator-table operator empty-dtree)]
                                (assoc operator-table operator
                                       (dtree-insert dtree f predicates))))))

(defn findhandler [operator arguments]
  (if-let [dtree (@the-operator-table operator)]
    (dtree-lookup dtree arguments)))

(defn make-operation [operator]
  (fn [& args]
    (if-let [h (findhandler operator args)]
      (apply h args)
      (throw (IllegalArgumentException.
              "no version of that operator works for those args.")))))

;; belongs to generic
;; XXX: have type return a predicate discriminating the type.
;; XXX: eventually: define a total order on types for canonicalization
(defn type [a]
  (or (:type (meta a))
      (core-type a)))

