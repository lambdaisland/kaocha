(ns kaocha.type.clojure.spec.alpha.fdef
  (:require [kaocha.testable]
            [kaocha.type.var]
            [clojure.spec.alpha :as s]))

(alias 'stc 'clojure.spec.test.check)

(defn load-testable [sym {::stc/keys [num-tests max-size]
                          :as        test-plan}]
  (let [nsname    (namespace sym)
        test-name (str sym)
        var       (resolve sym)]
    {:kaocha.testable/type :kaocha.type/clojure.spec.alpha.fdef
     :kaocha.testable/id   (keyword test-name)
     :kaocha.testable/meta (meta var)
     :kaocha.testable/desc (str sym)
     :kaocha.fdef/sym      sym
     :kaocha.fdef/name     test-name
     :kaocha.fdef/var      var
     :kaocha.fdef/check-opts (select-keys test-plan )
     ::stc/num-tests       num-tests
     ::stc/max-size        max-size}))

(defmethod testable/-run :kaocha.type/clojure.spec.alpha.fdef
  [{the-var  :kaocha.fdef/var
    sym      :kaocha.fdef/sym
    the-meta :kaocha.testable/meta
    opts     :kaocha.fdef/check-opts
    :as      testable} test-plan]
  (type/with-report-counters
    (let [results (stest/check sym ~opts)]
      (binding [t/*testing-vars* (conj t/*testing-vars* the-var)]
        (t/do-report {:type :begin-test-var, :var the-var})
        (try
          (test)
          (catch clojure.lang.ExceptionInfo e
            (when-not (:kaocha/fail-fast (ex-data e))
              (t/do-report {:type                    :error
                            :message                 "Uncaught exception, not in assertion."
                            :expected                nil
                            :actual                  e
                            :kaocha.result/exception e})))
          (catch Throwable e
            (t/do-report {:type                    :error
                          :message                 "Uncaught exception, not in assertion."
                          :expected                nil
                          :actual                  e
                          :kaocha.result/exception e}))))
      (let [{::result/keys [pass error fail pending] :as result} (type/report-count)]
        (when (= pass error fail pending 0)
          (binding [testable/*fail-fast?*    false
                    testable/*test-location* {:file (:file the-meta) :line (:line the-meta)}]
            (t/do-report {:type ::zero-assertions})))
        (t/do-report {:type :end-test-var, :var the-var})
        (merge testable {:kaocha.result/count 1} (type/report-count))))))

(s/def :kaocha.fdef/name :kaocha.var/name)
(s/def :kaocha.fdef/var :kaocha.var/var)
(s/def :kaocha.fdef/sym symbol?)

(s/def :kaocha.type/clojure.spec.alpha.fdef
  (s/keys :req [:kaocha.testable/type
                :kaocha.testable/id
                :kaocha.fdef/name
                :kaocha.fdef/var]))
