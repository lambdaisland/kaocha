(ns kaocha.plugin-test
  (:require [kaocha.plugin :as plugin]
            [clojure.test :refer :all]
            [kaocha.test-util :as util]
            [kaocha.output :as output])
  (:import (clojure.lang ExceptionInfo)))

(deftest missing-plugin-test
  (let [expected-message "Couldn't load plugin :kaocha.missing.plugin/gone. Failed to load namespaces kaocha.missing.plugin.gone and kaocha.missing.plugin."]
  (is (thrown-with-msg? ExceptionInfo
                        (re-pattern expected-message)
                        (plugin/load-all [:kaocha.missing.plugin/gone])))
  (is (= {:err  (str "ERROR: " expected-message "\n") :out "" :result nil}
         (binding [output/*colored-output* false]
           (util/with-out-err
             (try
               (plugin/load-all [:kaocha.missing.plugin/gone])
               (catch ExceptionInfo e
                 nil))))))))

(deftest missing-unnamespaced-plugin-test
  (let [expected-message "Couldn't load plugin :kaocha.plugin/gone. Failed to load namespace kaocha.plugin.gone."]
    (is (thrown-with-msg? ExceptionInfo
                          (re-pattern expected-message)
                        (plugin/load-all [:gone])))
  (is (= {:err (str "ERROR: " expected-message "\n") :out "" :result nil}
         (binding [output/*colored-output* false]
           (util/with-out-err
             (try
               (plugin/load-all [:gone])
               (catch ExceptionInfo e
                 nil))))))))


(deftest missing-plugin-valid-ns-test
  (let [expected-message "Couldn't load plugin :kaocha/plugin, but loaded kaocha.plugin."]
    (is (thrown-with-msg? ExceptionInfo
                          (re-pattern expected-message)
                          (plugin/load-all [:kaocha/plugin])))
    (is (= {:err (str "ERROR: " expected-message "\n") :out "" :result nil}
           (binding [output/*colored-output* false]
             (util/with-out-err
               (try
                 (plugin/load-all [:kaocha/plugin])
                 (catch ExceptionInfo e
                   nil))))))))
