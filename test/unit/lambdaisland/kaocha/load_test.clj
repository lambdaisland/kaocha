(ns lambdaisland.kaocha.load-test
  (:require [clojure.test :refer :all]
            [lambdaisland.kaocha.classpath :as cp]
            [lambdaisland.kaocha.load :as load]
            [clojure.java.io :as io]))

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

(deftest test-vars-test
  (load/load-tests {:test-paths ["fixtures/a-tests"] :ns-patterns #{#"-test$"}})
  (is (= (load/test-vars 'foo.bar-test)
         [(resolve 'foo.bar-test/a-test)])))

(deftest find-tests-test
  ;; call find-tests first, or the var in the result won't resolve
  (let [result (load/find-tests {:test-paths ["fixtures/a-tests"]
                                 :ns-patterns ["-test$"]})]
    (is (= {:test-paths ["fixtures/a-tests"]
            :ns-patterns ["-test$"]
            :tests {'foo.bar-test
                    [(resolve 'foo.bar-test/a-test)]}}))))
