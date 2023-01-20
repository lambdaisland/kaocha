(ns kaocha.repl-test
  (:require [clojure.test :refer :all]
            [kaocha.repl :as repl]
            [kaocha.config :as config]))

(deftest config-test
  (is (match?
       '{:kaocha/tests [{:kaocha.testable/id :foo
                         :kaocha/test-paths ["test/foo"]}]
         :kaocha/reporter [kaocha.report.progress/report]
         :kaocha/color? false
         :kaocha/fail-fast? true
         :kaocha/plugins [:kaocha.plugin/randomize
                          :kaocha.plugin/filter
                          :kaocha.plugin/capture-output
                          :kaocha.plugin.alpha/xfail]}

       (repl/config {:config-file "fixtures/custom_config.edn"}))))

(deftest extra-config-test
  (is (match?
        '{:kaocha/tests [{:kaocha.testable/id :foo
                         :kaocha/test-paths ["test/foo"]}]
         :kaocha/reporter [kaocha.report.progress/report]
         :kaocha/color? true
         :kaocha/fail-fast? true
         :kaocha/plugins [:kaocha.plugin/randomize
                          :kaocha.plugin/filter
                          :kaocha.plugin/capture-output
                          :kaocha.plugin.alpha/xfail]}
        (repl/config {:color? true :config-file "fixtures/custom_config.edn"}))))


(deftest config-with-profile-test
  (testing  "specifying a profile"
    (is (match?
          '{:kaocha/tests [{:kaocha.testable/id :unit
                            :kaocha/test-paths ["test"]}]
            :kaocha/reporter kaocha.report.progress/report }
          (repl/config {:profile :test :config-file "test/unit/kaocha/config/loaded-test-profile.edn"}))))
  (testing "not specifying a profile"
    (is (match?
          '{:kaocha/tests [{:kaocha.testable/id :unit
                            :kaocha/test-paths ["test"]}]
            :kaocha/reporter kaocha.report/documentation }
          (repl/config {:config-file "test/unit/kaocha/config/loaded-test-profile.edn"})))))


