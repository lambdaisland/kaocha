(ns lambdaisland.kaocha.runner-test
  (:require [clojure.test :refer :all]
            [lambdaisland.kaocha.runner :as runner]
            [lambdaisland.kaocha.test-util :refer [with-out-err]]))

(defn -main [& args]
  (with-out-err (apply #'runner/-main* args)))

(deftest main-test
  (testing "--test-help"
    (let [{:keys [out err result]} (-main "--test-help")]
      (is (re-find #"USAGE:" out))
      (is (= 0 result))))

  (testing "unknown command line options"
    (let [{:keys [out err result]} (-main "--foo")]
      (is (= 1 result))
      (is (re-find #"Unknown option: \"--foo\"\n" out))
      (is (re-find #"USAGE:" out)))))

(deftest help-test
  (is (= ["" "USAGE:"
          "" "clj -m lambdaisland.kaocha.runner [OPTIONS]... [TEST-SUITE]..."
          "" "SUMMARY"
          "" "Options may be repeated multiple times for a logical OR effect."]
         (runner/help "SUMMARY"))))
