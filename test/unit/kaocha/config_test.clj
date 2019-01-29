(ns kaocha.config-test
  (:require [kaocha.test :refer :all]
            [kaocha.config :as c]))

(def rename-key @#'c/rename-key)

(deftest rename-key-test
  (is (= {:yyy 456 :zzz 123}
         (rename-key {:xxx 123 :yyy 456}
                     :xxx :zzz)))

  (is (= {:yyy 456}
         (rename-key {:yyy 456}
                     :xxx :zzz)))

  (is (= {:yyy 123}
         (rename-key {:xxx 123 :yyy 456}
                     :xxx :yyy))))

(deftest default-config-test
  (is (= {:kaocha/reporter   ['kaocha.report/dots]
          :kaocha/color?     true
          :kaocha/fail-fast? false
          :kaocha/plugins [:kaocha.plugin/randomize
                           :kaocha.plugin/filter
                           :kaocha.plugin/capture-output]
          :kaocha/tests [{:kaocha.testable/type    :kaocha.type/clojure.test
                          :kaocha.testable/id      :unit
                          :kaocha/ns-patterns      ["-test$"]
                          :kaocha/source-paths     ["src"]
                          :kaocha/test-paths       ["test"]
                          :kaocha.filter/skip-meta [:kaocha/skip]}]}
         (c/default-config))))

(deftest merge-config-test
  (testing "merges maps"
    (is (= {:a 1 :b 3 :c 4}
           (c/merge-config {:a 1 :b 2} {:b 3 :c 4}))))

  (testing "uses meta-merge"
    (is (= {:a [1 2 3 4]}
           (c/merge-config {:a [1 2]} {:a [3 4]})))

    (is (= {:a [3 4]}
           (c/merge-config {:a [1 2]} {:a ^:replace [3 4]}))))

  (testing "defaults to replacing for certain keys"
    (is (= {:kaocha/reporter ['yyy]}
           (c/merge-config {:kaocha/reporter '[xxx]}
                           {:kaocha/reporter '[yyy]})))))

(deftest normalize-test-suite-test
  (testing "namespaces keywords"
    (is (match? {:kaocha.testable/type    :kaocha.type/clojure.test
                 :kaocha.testable/id      :foo
                 :kaocha.filter/skip-meta [:kaocha/skip :xxx]}
                (c/normalize-test-suite {:type      :kaocha.type/clojure.test
                                         :id        :foo
                                         :skip-meta [:xxx]}))))

  (testing "adds default keys"
    (is (match? {:kaocha/test-paths ["test"]}
                (c/normalize-test-suite {})))

    (is (match? {:kaocha/test-paths ["test/unit"]}
                (c/normalize-test-suite {:kaocha/test-paths ["test/unit"]}))))

  (testing "adds a description"
    (is (match? {:kaocha.testable/desc "foo (clojure.test)"}
                (c/normalize-test-suite {:type :kaocha.type/clojure.test :id :foo})))))

(deftest normalize-test
  (testing "normalizes keys"
    (is (match? {:kaocha/color? false}
                (c/normalize {:color? false}))))

  (testing "normalizes test suites"
    (is (match? {:kaocha/tests [{:kaocha.testable/id :unit}]}
                (c/normalize {:tests [{}]}))))

  (testing "knows about randomize? and capture-output?"
    (is (match? {:kaocha.plugin.capture-output/capture-output? :sentinel1
                 :kaocha.plugin.randomize/randomize? :sentinel2}
                (c/normalize {:capture-output? :sentinel1
                              :randomize? :sentinel2})))))

(deftest load-config-test []
  (testing "loads the config file in the project root"
    (is (match? {:kaocha/tests [{:kaocha.testable/id :unit}
                                {:kaocha.testable/id :integration}]}
                (c/load-config)))))

(deftest apply-cli-opts-test
  (is (= {:kaocha/fail-fast? true,
          :kaocha/reporter   '[kaocha.report/documentation],
          :kaocha/watch?     true,
          :kaocha/color?     true,
          :kaocha/plugins    '[kaocha.plugin/foo],
          :kaocha/cli-options {:fail-fast true
                               :reporter  '[kaocha.report/documentation]
                               :watch     true
                               :color     true
                               :plugin    '[kaocha.plugin/foo kaocha.plugin/foo]}}
         (c/apply-cli-opts {} {:fail-fast true
                               :reporter  ['kaocha.report/documentation]
                               :watch     true
                               :color     true
                               :plugin    ['kaocha.plugin/foo 'kaocha.plugin/foo]}))))

(deftest apply-cli-args-test
  (is (= {:kaocha/tests
          [{:kaocha.testable/id :foo, :kaocha.testable/skip true}
           {:kaocha.testable/id :bar}]
          :kaocha/cli-args [:bar]}
         (c/apply-cli-args {:kaocha/tests [{:kaocha.testable/id :foo}
                                           {:kaocha.testable/id :bar}]}
                           [:bar])))

  (is (= {:kaocha/tests [{:kaocha.testable/id :foo}]}
         (c/apply-cli-args {:kaocha/tests [{:kaocha.testable/id :foo}]}
                           []))))

(defn rep1 [m] (println "rep1 called"))
(defn rep2 [m] (println "rep2 called"))

(deftest resolve-reporter-test
  (is (= kaocha.report/clojure-test-report
         (c/resolve-reporter 'clojure.test/report)))

  (is (= kaocha.report/dots*
         (c/resolve-reporter 'kaocha.report/dots*)))

  (is (= "rep1 called\nrep2 called\n"
         (with-out-str ((c/resolve-reporter [`rep1 `rep2]) {}))))

  (is (thrown? clojure.lang.ExceptionInfo (c/resolve-reporter `does-not-exist))))
