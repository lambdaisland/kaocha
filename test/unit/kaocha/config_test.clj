(ns kaocha.config-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [kaocha.config :as c]
            [matcher-combinators.matchers :as m]
            [slingshot.slingshot :refer [try+]]))

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

(def expected-default-config
  {:kaocha/reporter   ['kaocha.report/dots]
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
                   :kaocha.filter/skip-meta [:kaocha/skip]}]})

(deftest default-config-test
  (is (= expected-default-config
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
                           {:kaocha/reporter '[yyy]}))))
  (testing "does not override metadata for replace-by-default key tests"
    (is (= {:kaocha/tests [{:id :integration} {:id :unit}]}
          (c/merge-config {:kaocha/tests [{:id :unit}]}
                          {:kaocha/tests ^:prepend [{:id :integration}]}))))
  (testing "does not override metadata for replace-by-default key test-paths"
    (is (= {:kaocha/test-paths ["unit-tests" "integration-tests" ]}
          (c/merge-config {:kaocha/test-paths ["unit-tests"]}
                          {:kaocha/test-paths ^:append ["integration-tests"]})))))

(deftest merge-ns-patterns-issue-124-test
  (testing "https://github.com/lambdaisland/kaocha/issues/124"
    (is (= #:kaocha{:early-exit 250}
           (try+
            (c/merge-config {:kaocha/ns-patterns "test"} {:kaocha/ns-patterns "test"})
            (catch :kaocha/early-exit e
              e))))))

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

(deftest normalize-plugin-names-test
  (is (= [:kaocha.plugin/foo :foo/bar]
         (c/normalize-plugin-names [:foo :foo/bar])))

  (is (thrown? clojure.lang.ExceptionInfo (c/normalize-plugin-names '[foo]))))

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

(deftest load-config-test
  (testing "from file path"
    (testing "loads the config file in the project root"
      (is (match? {:kaocha/tests [{:kaocha.testable/id :unit}
                                  {:kaocha.testable/id :integration}]}
                  (c/load-config))))

    (testing "supports Aero manipulation"
      (is (match? {:kaocha/reporter ['kaocha.report.progress/report]
                   :kaocha/plugins  (m/embeds [:some.kaocha.plugin/foo :other.kaocha.plugin/bar])}
                  (c/load-config "test/unit/kaocha/config/loaded-test.edn"))))

    (testing "falls back to default when file does not exist"
      (is (= expected-default-config
             (c/load-config "file-that-does-not-exist.edn")))))

  (testing "from resource"
    (testing "supports Aero manipulation"
      (is (match? {:kaocha/reporter ['kaocha.report.progress/report]
                   :kaocha/fail-fast? true
                   :kaocha/plugins (m/embeds [:some.kaocha.plugin/qux :other.kaocha.plugin/bar])}
                  (c/load-config (io/resource "kaocha/config/loaded-test-resource.edn")))))

    (testing "falls back to default when resource does not exist"
      (is (= expected-default-config
             (c/load-config (io/resource "resource-that-does-not-exist.edn")))))))

(deftest load-config2-test
  (testing "from file path"

    (testing "supports Aero manipulation"
      (is (match? {:kaocha/reporter ['kaocha.report.progress/report]
                   :kaocha/plugins  (m/embeds [:some.kaocha.plugin/foo :other.kaocha.plugin/bar])}
                  (c/load-config2 "test/unit/kaocha/config/loaded-test.edn"))))

    (testing "falls back to default when file does not exist"
      (is (= expected-default-config
             (c/load-config2 "file-that-does-not-exist.edn")))))

  (testing "from resource"
    (testing "supports Aero manipulation"
      (is (match? {:kaocha/reporter ['kaocha.report.progress/report]
                   :kaocha/fail-fast? true
                   :kaocha/plugins (m/embeds [:some.kaocha.plugin/qux :other.kaocha.plugin/bar])}
                  (c/load-config2 (io/resource "kaocha/config/loaded-test-resource.edn")))))

    (testing "falls back to default when resource does not exist"
      (is (= expected-default-config
             (c/load-config2 (io/resource "resource-that-does-not-exist.edn")))))))

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
