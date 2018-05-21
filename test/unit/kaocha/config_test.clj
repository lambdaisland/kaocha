(ns kaocha.config-test
  (:require [kaocha.config :as config]
            [kaocha.test-util :refer [with-out-err]]
            [clojure.test :refer :all]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]
            [kaocha.test-helper]
            [kaocha.specs]))

(def rename-key @#'config/rename-key)

(deftest default-config-test
  (is (spec-valid? :kaocha/config (config/default-config))))

(deftest load-config-test
  (testing "it warns when no such file is found"
    (is (= (with-out-err (config/load-config "foo.edn"))
           {:err "\u001b[31mWARNING: \u001b[0mConfig file not found: foo.edn, using default values.\n"
            :out ""
            :result nil})))

  (testing "it loads and parses the file"
    (is (match? (with-out-err (config/load-config "fixtures/tests.edn"))
                {:err ""
                 :out ""
                 :result #:kaocha{:suites
                                  [#:kaocha{:id :a
                                            :test-paths ["fixtures/a-tests"]}
                                   #:kaocha{:id :b
                                            :test-paths ["fixtures/b-tests"]}]}}))))

(deftest rename-key-test
  (testing "it ignores keys that don't exist"
    (is (= (rename-key {:ok 123} :foo :bar)
           {:ok 123})))

  (testing "it renames the key"
    (is (= (rename-key {:foo 123} :foo :bar)
           {:bar 123}))))

(deftest normalize-cli-opts-test
  (are [x y] (match? (m/equals x) (config/normalize-cli-opts y))
    {}                                         {}
    {:ok 123}                         {:ok 123}
    #:kaocha{:test-paths []}                  {:test-path []}
    #:kaocha{:ns-patterns :ok}                {:ns-pattern :ok}
    #:kaocha{:test-paths [] :ns-patterns :ok} {:test-path [] :ns-pattern :ok}))

(deftest resolve-reporter
  (testing "it resolves symbols but leaves functions alone"
    (are [x y] (= x (config/resolve-reporter y))
      identity identity
      identity 'clojure.core/identity))

  (testing "it composes multiple functions into a single reporter"
    (let [fx (atom {})
          reporter (config/resolve-reporter [(fn [x] (swap! fx assoc :x x))
                                             (fn [y] (swap! fx assoc :y y))])]

      (is (ifn? reporter))
      (reporter :ok)
      (is (= @fx {:x :ok :y :ok}))))

  (testing "it throws when a reporter couldn't be resolved"
    (is (thrown? clojure.lang.ExceptionInfo (config/resolve-reporter 'no-ns-symbol)))
    (is (thrown? clojure.lang.ExceptionInfo (config/resolve-reporter 'clojure.core/no-such-var)))
    (is (thrown? clojure.lang.ExceptionInfo (config/resolve-reporter 'bar/no-such-ns)))))

(deftest normalize-test
  (testing "it merges suite-config given at the top level into suites"
    (is (match? #:kaocha{:suites
                         [#:kaocha {:ns-patterns ["-test$"]
                                    :test-paths ["test"]
                                    :id :unit}
                          #:kaocha {:ns-patterns ["-test$"]
                                    :test-paths ["test"]
                                    :id :integration}]
                         :color?   true}
                (config/normalize #:kaocha{:suites     [#:kaocha{:id :unit}
                                                        #:kaocha{:id :integration}]
                                           :test-paths ["test"]}))))

  (testing "it filters unknown keys"
    (is (match? #:kaocha{:suites
                         [#:kaocha {:id           :unit
                                    :source-paths ["src"]
                                    :ns-patterns  ["-test$"]
                                    :test-paths   ["test"]}]
                         :color?     true
                         :randomize? true
                         :watch?     false
                         :reporter  'kaocha.report/progress}
                (config/normalize {:foo :bar})))))
