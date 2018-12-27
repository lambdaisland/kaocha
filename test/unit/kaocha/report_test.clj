(ns kaocha.report-test
  (:require [clojure.test :refer :all]
            [kaocha.report :as r]
            [kaocha.test-util :refer [with-test-out-str]]
            [kaocha.output :as output]))

(deftest tap-test
  (is (= "ok  (foo.clj:20)\n"
         (with-test-out-str
           (binding [output/*colored-output* false]
             (r/tap {:type :pass
                     :file "foo.clj"
                     :line 20})))))

  (is (= (str "not ok  (foo.clj:20)\n"
              "#  FAIL in  (foo.clj:20)\n"
              "#  Expected:\n"
              "#    3\n"
              "#  Actual:\n"
              "#    -3 +4\n")
         (with-test-out-str
           (binding [output/*colored-output* false]
             (r/tap {:type :fail
                     :file "foo.clj"
                     :line 20
                     :expected '(= 3 4)
                     :actual '(not (= 3 4))}))))))
