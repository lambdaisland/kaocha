(ns kaocha.type.spec.test.fdef
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.test.alpha :as stest]
            [clojure.string :as str]
            [clojure.test :as t]
            [expound.alpha :as expound]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.report :as report]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [orchestra.spec.test :as orchestra]))

;; This namespace does not actually exist, but is created by
;; requiring clojure.spec.test.alpha
(alias 'stc 'clojure.spec.test.check)

(defn load-testable [stc-config sym]
  (let [var (resolve sym)]
    (merge {:kaocha.testable/type :kaocha.type/spec.test.fdef
            :kaocha.testable/id   (keyword sym)
            :kaocha.testable/meta (meta var)
            :kaocha.testable/desc (str sym)
            :kaocha.spec.fdef/sym sym
            :kaocha.spec.fdef/var var}
           stc-config)))

(defn load-testables [testable syms]
  (let [stc-config (select-keys testable [::stc/instrument?
                                          ::stc/check-asserts?
                                          ::stc/opts])]
    (->> syms
         (sort-by name)
         (map (partial load-testable stc-config)))))

(defn report-success [check-results]
  (t/do-report
   {:type    :pass
    :message (str "Generative tests pass for "
                  (str/join ", " (map :sym check-results)))}))

(defn report-failure [check-results]
  (doseq [failed-check (filter :failure check-results)]
    (let [r       (stest/abbrev-result failed-check)
          failure (:failure r)]
      (t/do-report
       {:type     :fail
        :message  (expound/explain-results-str check-results)
        :expected (->> r :spec rest (apply hash-map) :ret)
        :actual   (if (instance? Throwable failure)
                    failure
                    (::stest/val failure))}))))

(defmethod testable/-run :kaocha.type/spec.test.fdef
  [{the-var        :kaocha.spec.fdef/var
    sym            :kaocha.spec.fdef/sym
    wrap           :kaocha.testable/wrap
    instrument?    ::stc/instrument?
    check-asserts? ::stc/check-asserts?
    opts           ::stc/opts
    :as            testable}
   _test-plan]
  (type/with-report-counters
    (when instrument? (orchestra/instrument))
    (when check-asserts? (spec/check-asserts true))
    (t/do-report {:type :kaocha.stc/begin-fdef, :var the-var})
    (try
      (let [location       (select-keys (meta the-var) [:file :line])
            test           (reduce #(%2 %1) (partial stest/check sym {::stc/opts opts}) wrap)
            check-results  (test)
            checks-passed? (->> check-results (map :failure) (every? nil?))]
        (binding [testable/*test-location* location]
          (if checks-passed?
            (report-success check-results)
            (report-failure check-results))))
      (catch clojure.lang.ExceptionInfo e
        (when-not (:kaocha/fail-fast (ex-data e))
          (report/report-exception e)))
      (catch Throwable e
        (report/report-exception e)))
    (t/do-report {:type :kaocha.stc/end-fdef, :var the-var})
    (when instrument? (orchestra/unstrument))
    (when check-asserts? (spec/check-asserts false))
    (merge testable {:kaocha.result/count 1} (type/report-count))))

(spec/def :kaocha.spec.fdef/var var?)
(spec/def :kaocha.spec.fdef/sym qualified-symbol?)

(spec/def :kaocha.type/spec.test.fdef
  (spec/keys :req [:kaocha.testable/type
                :kaocha.testable/id
                :kaocha.spec.fdef/var]))

(hierarchy/derive! :kaocha.type/spec.test.fdef :kaocha.testable.type/leaf)
(hierarchy/derive! :kaocha.stc/begin-fdef :kaocha/begin-test)
(hierarchy/derive! :kaocha.stc/end-fdef :kaocha/end-test)
