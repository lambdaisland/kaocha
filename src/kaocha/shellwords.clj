(ns kaocha.shellwords)

(def shellwords-pattern #"[^\s'\"]+|[']([^']*)[']|[\"]([^\"]*)[\"]")

;; ported from cucumber.runtime.ShellWords, which was ported from Ruby
(defn shellwords [cmdline]
  (let [matcher (re-matcher shellwords-pattern cmdline)]
    (loop [res []]
      (if (.find matcher)
        (recur
         (if-let [word (.group matcher 1)]
           (conj res word)
           (let [word (.group matcher)]
             (if (and (= \" (first word))
                      (= \" (last word)))
               (conj res (subs word 1 (dec (count word))))
               (conj res word)))))
        res))))
