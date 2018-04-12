(ns lambdaisland.kaocha.runner-test
  (:require [clojure.test :refer :all]
            [lambdaisland.kaocha.runner :as runner]))

(deftest main-test
  (let [main-out (fn [& args] (with-out-str (apply runner/-main args)))]
    (testing "it prints help when asked"
      (is (re-find #"USAGE:" (main-out "--test-help"))))

    (testing "it reports unknown command line options"
      (let [exit-code (atom nil)
            err-out (with-redefs [runner/exit-process! #(reset! exit-code %)]
                      (main-out "--foo"))]
        (is (= 1 @exit-code))
        (is (re-find #"Unknown option: \"--foo\"\n" err-out))
        (is (re-find #"USAGE:" err-out))))))

(deftest help-test
  (is (= ["" "USAGE:"
          "" "clj -m lambdaisland.kaocha.runner [OPTIONS]... [TEST-SUITE]..."
          "" "SUMMARY"
          "" "Options may be repeated multiple times for a logical OR effect."]
         (runner/help "SUMMARY"))))
