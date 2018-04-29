(ns kaocha.random)

(defn rng [seed]
  (let [rng (java.util.Random. seed)]
    (fn [& _] (.nextInt rng))))

(defn randomize-tests [seed ns->tests]
  (let [next-int (rng seed)
        ns->tests' (->> ns->tests
                        (map (fn [[k v]] [k (sort-by str v)]))
                        (sort-by first))]
    (->> ns->tests'
         (map (fn [[k v]] [k (sort-by next-int v)]))
         (sort-by next-int))))
