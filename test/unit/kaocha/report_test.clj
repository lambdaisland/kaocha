(ns kaocha.report-test
  (:require [clojure.test :refer :all]
            [kaocha.report :as r]
            [kaocha.test-util :refer [with-test-out-str]]
            [kaocha.output :as output]
            [kaocha.hierarchy :as hierarchy]))

(deftest dispatch-extra-keys-test
  (testing "it dispatches to custom clojure.test/report extensions"
    (.addMethod r/clojure-test-report
                ::yolo
                (fn [m]
                  (clojure.test/with-test-out
                    (println "YOLO expected"
                             (:expected m)
                             "actual"
                             (:actual m)))))

    (is (= "YOLO expected :x actual :y\n"
           (with-test-out-str
             (r/dispatch-extra-keys {:type ::yolo
                                     :expected :x
                                     :actual :y})))))

  (testing "it does nothing if there is no matching multimethod implementation"
    (is (= ""
           (with-test-out-str
             (r/dispatch-extra-keys {:type ::nolo})))))

  (testing "it does nothing if it's a key known to Kaocha"
    (hierarchy/derive! ::knowlo :kaocha/known-key)
    (.addMethod r/clojure-test-report
                ::knowlo
                (fn [m] (clojure.test/with-test-out (println "KNOWLO"))))
    (is (= ""
           (with-test-out-str
             (r/dispatch-extra-keys {:type ::knowlo})))))

  (testing "it does nothing if the key is globally marked as \"known\""
    (derive ::knowlo :kaocha/known-key)
    (.addMethod r/clojure-test-report
                ::knowlo
                (fn [m] (clojure.test/with-test-out (println "KNOWLO"))))
    (is (= ""
           (with-test-out-str
             (r/dispatch-extra-keys {:type ::knowlo}))))))

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
