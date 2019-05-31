(ns kaocha.type.clojure.spec.test.fdef
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.string :as str]
            [clojure.test :as test]
            [expound.alpha :as expound]
            [kaocha.result :as result]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [kaocha.specs]))

(alias 'stc 'clojure.spec.test.check)

(defn load-testable [{::stc/keys [opts] :as test-plan} sym]
  (let [nsname    (namespace sym)
        var       (resolve sym)]
    {:kaocha.testable/type  :kaocha.type/clojure.spec.test.fdef
     :kaocha.testable/id    (keyword sym)
     :kaocha.testable/meta  (meta var)
     :kaocha.testable/desc  (str sym)
     :kaocha.spec.fdef/sym  sym
     :kaocha.spec.fdef/var  var
     ::stc/opts             opts}))

(defn load-testables [test-plan syms]
  (->> syms
       (sort-by name)
       (map #(load-testable test-plan %))))

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

(defmethod testable/-run :kaocha.type/clojure.spec.test.fdef
  [{the-var  :kaocha.spec.fdef/var
    sym      :kaocha.spec.fdef/sym
    the-meta :kaocha.testable/meta
    opts     ::stc/opts
    :as      testable} test-plan]
  (type/with-report-counters
    (let [check-results  (stest/check sym opts)
          result-count   (count check-results)
          checks-passed? (->> check-results (map :failure) (every? nil?))]
      (if checks-passed?
        (report-success check-results)
        (report-failure check-results))
      (merge testable {:kaocha.result/count result-count} (type/report-count)))))

(s/def :kaocha.spec.fdef/var var?)
(s/def :kaocha.spec.fdef/sym qualified-symbol?)

(s/def :kaocha.type/clojure.spec.test.fdef
  (s/keys :req [:kaocha.testable/type
                :kaocha.testable/id
                :kaocha.spec.fdef/name
                :kaocha.spec.fdef/var]))
