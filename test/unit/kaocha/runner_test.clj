(ns kaocha.runner-test
  (:require [clojure.test :as t :refer :all]
            [kaocha.runner :as runner]
            [kaocha.test-util :refer [with-out-err]]))

(defn -main [& args]
  (with-out-err
    (apply #'runner/-main* args)))

(deftest working-tools-cli?-test
  (is (#'runner/working-tools-cli?)))

(deftest main-test
  (testing "--test-help"
    (let [{:keys [out err result]} (-main "--test-help")]
      (is (re-find #"USAGE:" out))
      (is (= 0 result))))

  (testing "unknown command line options"
    (let [{:keys [out err result]} (-main "--foo")]
      (is (= -1 result))
      (is (re-find #"Unknown option: \"--foo\"\n" out))
      (is (re-find #"USAGE:" out)))))

(deftest exec-args->cli-args-test
  (testing "converts exec-args map to CLI args vector w/ values after arg-names"
    (let [cli-args (runner/exec-args->cli-args {:config-file "tests.edn"
                                                :fail-fast   true
                                                :color       false
                                                :diff-style  :deep})]
      (are [arg-name arg-val]
        (let [arg-index (.indexOf cli-args arg-name)]
          (and (<= 0 arg-index)
               (or (= ::no-val arg-val)
                   (= (inc arg-index) (.indexOf cli-args arg-val)))))
        "--config-file" "tests.edn"
        "--fail-fast"   ::no-val
        "--no-color"    ::no-val
        "--diff-style"  ":deep"))))
