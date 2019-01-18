(ns kaocha.monkey-patch-test
  (:require [clojure.test :refer :all]
            [kaocha.monkey-patch :as monkey-patch]
            [kaocha.plugin :as plugin]))

;; Note: this test is doing some somersaults to avoid crashing into clojure.test
;; while it is itself running.
(deftest pre-report-hook-is-used
  (let [result (atom nil)]
    (binding [plugin/*current-chain* [{:kaocha.hooks/pre-report (fn [event] (assoc event :been-here true))}]]
      (with-redefs [monkey-patch/report (fn [event] (reset! result event))]
        (monkey-patch/do-report {:type :pass})))
    (is (:been-here @result))))
