(ns kaocha.report-test
  (:require [clojure.test :refer :all]
            [kaocha.report :as r]
            [kaocha.type :as type]
            [kaocha.test-util :refer [with-test-out-str]]
            [kaocha.output :as output]
            [kaocha.hierarchy :as hierarchy]
            [clojure.test :as t]))

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

(deftest dots*-test
  (is (= "."
         (with-test-out-str
           (r/dots* {:type :pass}))))

  (is (= "[31mF[m"
         (with-test-out-str
           (r/dots* {:type :fail}))))

  (is (= "[31mE[m"
         (with-test-out-str
           (r/dots* {:type :error}))))

  (is (= "[33mP[m"
         (with-test-out-str
           (r/dots* {:type :kaocha/pending}))))

  (is (= "("
         (with-test-out-str
           (r/dots* {:type :kaocha/begin-group}))))

  (is (= ")"
         (with-test-out-str
           (r/dots* {:type :kaocha/end-group}))))

  (is (= "["
         (with-test-out-str
           (r/dots* {:type :begin-test-suite}))))

  (is (= "]"
         (with-test-out-str
           (r/dots* {:type :end-test-suite}))))

  (is (= "\n"
         (with-test-out-str
           (r/dots* {:type :summary})))))

(deftest report-counters-test
  (is (= #:kaocha.result {:pass 1 :error 0 :fail 0 :pending 0}
         (type/with-report-counters
           (r/report-counters {:type :pass})
           (type/report-count))))

  (is (= #:kaocha.result {:pass 0 :error 0 :fail 1 :pending 0}
         (type/with-report-counters
           (r/report-counters {:type :fail})
           (type/report-count))))

  (is (= #:kaocha.result {:pass 0 :error 1 :fail 0 :pending 0}
         (type/with-report-counters
           (r/report-counters {:type :error})
           (type/report-count))))

  (is (= #:kaocha.result {:pass 0 :error 0 :fail 0 :pending 1}
         (type/with-report-counters
           (r/report-counters {:type :kaocha/pending})
           (type/report-count)))))

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

(comment
  (do
    (require 'kaocha.repl)
    (kaocha.repl/run))

  )
