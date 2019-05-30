(ns kaocha.type.clojure.spec.test.fdef
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.string :as str]
            [clojure.test :as test]
            [expound.alpha :as expound]
            [kaocha.result :as result]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [kaocha.spec-test-check :as k-stc]))

(alias 'stc 'clojure.spec.test.check)

(defn load-testable [sym test-plan]
  (let [nsname    (namespace sym)
        test-name (str sym)
        var       (resolve sym)]
    {:kaocha.testable/type   :kaocha.type/clojure.spec.alpha.fdef
     :kaocha.testable/id     (keyword test-name)
     :kaocha.testable/meta   (meta var)
     :kaocha.testable/desc   (str sym)
     :kaocha.fdef/sym        sym
     :kaocha.fdef/name       test-name
     :kaocha.fdef/var        var
     :kaocha.fdef/check-opts (k-stc/opts test-plan)}))

(defn load-testables [syms]
  (->> syms
       (sort-by name)
       (map load-testable)))

(defn report-success [check-results]
  (test/do-report
   {:type    :pass
    :message (str "Generative tests pass for "
                  (str/join ", " (map :sym check-results)))}))

(defn report-failure [check-results]
  (doseq [failed-check (filter :failure check-results)]
    (let [r       (stest/abbrev-result failed-check)
          failure (:failure r)]
      (test/do-report
       {:type     :fail
        :message  (expound/explain-results-str check-results)
        :expected (->> r :spec rest (apply hash-map) :ret)
        :actual   (if (instance? Throwable failure)
                    failure
                    (::stest/val failure))}))))

(defmethod testable/-run :kaocha.type/clojure.spec.alpha.fdef
  [{the-var  :kaocha.fdef/var
    sym      :kaocha.fdef/sym
    the-meta :kaocha.testable/meta
    opts     :kaocha.fdef/check-opts
    :as      testable} test-plan]
  (type/with-report-counters
    (let [check-results  (stest/check sym opts)
          result-count   (count check-results)
          checks-passed? (->> check-results (map :failure) (every? nil?))]
      (if checks-passed?
        (report-success check-results)
        (report-failure check-results))
      (merge testable {:kaocha.result/count result-count} (type/report-count)))))

(s/def :kaocha.fdef/name :kaocha.var/name)
(s/def :kaocha.fdef/var :kaocha.var/var)
(s/def :kaocha.fdef/sym symbol?)

(s/def :kaocha.type/clojure.spec.alpha.fdef
  (s/keys :req [:kaocha.testable/type
                :kaocha.testable/id
                :kaocha.fdef/name
                :kaocha.fdef/var]))
