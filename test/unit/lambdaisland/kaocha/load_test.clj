(ns lambdaisland.kaocha.load-test
  (:require [clojure.test :refer :all]
            [lambdaisland.kaocha.load :as load]))

(def ns-match? @#'load/ns-match?)

(deftest ns-match-test
  (testing "it returns truthy when matching"
    (are [patterns ns-sym] (ns-match? patterns ns-sym)
      #{#"-test$"} 'foo.bar-test))

  (testing "it returns falsy when not matching"
    (are [patterns ns-sym] (not (ns-match? patterns ns-sym))
      #{#"-test$"} 'foo.bar)))

(deftest load-tests-test
  (testing "it returns namespace names"
    (is (= '[foo.bar-test]
           (load/load-tests {:test-paths ["fixtures/a-tests"]
                             :ns-patterns #{#"-test$"}}))) )
  (testing "it loads namespaces"
    (is (= :ok @(resolve 'foo.bar-test/a-var)))))
