(ns kaocha.report.progress
  (:require [clojure.test :as t]
            [progrock.core :as pr]
            [kaocha.testable :as testable]
            [kaocha.output :as out]
            [kaocha.report :as report]))

(def bar (atom nil))

(defn format-bar [{:keys [label label-width failed?] :as bar}]
  (str (format (str "%" label-width "s") label)
       ":   :percent% ["
       (out/colored (if failed? :red :green) ":bar")
       "] :progress/:total"))

(defn print-bar []
  (t/with-test-out
    (pr/print @bar {:format (format-bar @bar)})))

(defmulti progress :type)
(defmethod progress :default [_])

(defmethod progress :begin-test-suite [m]
  (let [testable   (:kaocha/testable m)
        test-plan  (:kaocha/test-plan m)
        leaf-types (set (:kaocha/leaf-types test-plan))
        leaf-tests (->> testable
                        testable/test-seq
                        (filter #(contains? leaf-types (::testable/type %))))]
    (reset! bar (assoc (pr/progress-bar (count leaf-tests))
                       :label (name (:kaocha.testable/id testable))
                       :label-width (->> test-plan
                                         :kaocha.test-plan/tests
                                         (remove ::testable/skip)
                                         (map (comp count name :kaocha.testable/id))
                                         (apply max))))
    (print-bar)))

(defmethod progress :end-test-var [m]
  (swap! bar pr/tick)
  (print-bar))

(defmethod progress :end-test-suite [m]
  (swap! bar assoc :done? true)
  (print-bar))

(defmethod progress :fail [m]
  (swap! bar assoc :failed? true)
  (print-bar))

(defmethod progress :error [m]
  (swap! bar assoc :failed? true)
  (print-bar))

(def report [progress report/result])
