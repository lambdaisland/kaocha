(ns kaocha.testable.var
  (:require [clojure.test :as t]
            [kaocha.testable :as testable]
            [kaocha.result :as result]))

(defmethod testable/run :kaocha.type/var [{:kaocha.var/keys [var test] :as testable}]
  (let [initial-report @t/*report-counters*]
    (binding [t/*testing-vars* (conj t/*testing-vars* var)]
      (t/do-report {:type :begin-test-var, :var var})
      (try
        (test)
        (catch clojure.lang.ExceptionInfo e
          (when-not (:kaocha/fail-fast (ex-data e))
            (t/do-report {:type :error, :message "Uncaught exception, not in assertion."
                          :expected nil, :actual e})))
        (catch Throwable e
          (t/do-report {:type :error, :message "Uncaught exception, not in assertion."
                        :expected nil, :actual e})))
      (t/do-report {:type :end-test-var, :var var}))
    (merge testable
           {:kaocha.result/count 1}
           (result/diff-test-result initial-report @t/*report-counters*))))
