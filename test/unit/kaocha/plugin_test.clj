(ns kaocha.plugin-test
  (:require [kaocha.plugin :as plugin]
            [clojure.test :refer :all]
            [kaocha.test-util :as util]
            [kaocha.output :as output])
  (:import (clojure.lang ExceptionInfo)))

(deftest missing-plugin-test
  (let [expected-message "Couldn't load plugin :kaocha.missing.plugin/gone. Failed to load namespaces kaocha.missing.plugin.gone and kaocha.missing.plugin. This could be caused by a misspelling or a missing dependency."]
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
  (let [expected-message "Couldn't load plugin :kaocha.plugin/gone. Failed to load namespace kaocha.plugin.gone. This could be caused by a misspelling or a missing dependency."]
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
  (let [expected-message "Couldn't load plugin :kaocha/plugin. The plugin was not defined after loading namespace kaocha.plugin. Is the file missing a defplugin?"]
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

(deftest normalize-name-test
  (are [input expected] (= expected (plugin/normalize-name input))
    :abc               :kaocha.plugin/abc
    :kaocha.plugin/abc :kaocha.plugin/abc
    :custom-ns/abc     :custom-ns/abc
    :custom.ns.abc     :custom.ns.abc))
