(ns kaocha.core-ext-test
  (:refer-clojure :exclude [symbol])
  (:require [clojure.test :refer :all]
            [kaocha.core-ext :refer :all]))

(deftest regex?-test
  (is (regex? #"foo"))
  (is (not (regex? "foo"))))

(deftest exception?-test
  (is (exception? (Exception. "oh no")))
  (is (not (exception? (Throwable. "oh no")))))

(deftest error?-test
  (is (error? (Error. "oh no")))
  (is (not (error? (Exception. "oh no"))))
  (is (not (error? (Throwable. "oh no")))))

(deftest throwable?-test
  (is (throwable? (Error. "oh no")))
  (is (throwable? (Exception. "oh no")))
  (is (throwable? (Throwable. "oh no"))))

(deftest ns?-test
  (is (ns? *ns*))
  (is (not (ns? {}))))

(deftest regex-test
  (is (= "ok" (re-find (regex "[ko]+") "--ok--")))
  (is (= "ok" (re-find (regex #"[ko]+") "--ok--")))
  (is (= "ok" (re-find (regex "o" "k") "--ok--")))
  (is (thrown? clojure.lang.ExceptionInfo (regex 123) "")))

(deftest mapply-test
  (let [f (fn [& {:as opts}]
            {:opts opts})]
    (is (= {:opts {:foo :bar :abc :xyz}}
           (mapply f {:foo :bar :abc :xyz})))))
