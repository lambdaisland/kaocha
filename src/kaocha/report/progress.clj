(ns kaocha.report.progress
  (:require [clojure.test :as t]
            [progrock.core :as pr]
            [kaocha.testable :as testable]
            [kaocha.output :as output]
            [kaocha.report :as report]
            [kaocha.hierarchy :as hierarchy]))

(def bar (atom nil))

(defn color [{:keys [failed? pending?]}]
  (cond
    failed?  :red
    pending? :yellow
    :else    :green))

(defn format-bar [{:keys [label label-width] :as bar}]
  (str (format (str "%" label-width "s") label)
       ":   :percent% ["
       (output/colored (color bar) ":bar")
       "] :progress/:total"))

(defn print-bar []
  (t/with-test-out
    (pr/print @bar {:format (format-bar @bar)})))

(defmulti progress :type :hierarchy #'hierarchy/hierarchy)
(defmethod progress :default [_])

(defmethod progress :begin-test-suite [m]
  (let [testable   (:kaocha/testable m)
        test-plan  (:kaocha/test-plan m)
        leaf-tests (->> testable
                        testable/test-seq
                        (filter hierarchy/leaf?))]
    (reset! bar (assoc (pr/progress-bar (count leaf-tests))
                       :label (name (:kaocha.testable/id testable))
                       :label-width (->> test-plan
                                         :kaocha.test-plan/tests
                                         (remove :kaocha.testable/skip)
                                         (map (comp count name :kaocha.testable/id))
                                         (apply max))))
    (print-bar)))

(defmethod progress :kaocha/end-test [m]
  (swap! bar pr/tick)
  (print-bar))

(defmethod progress :end-test-suite [m]
  (swap! bar assoc :done? true)
  (print-bar))

(defmethod progress :kaocha/fail-type [m]
  (swap! bar assoc :failed? true)
  (print-bar))

(defmethod progress :kaocha/pending [m]
  (swap! bar assoc :pending? true)
  (print-bar))

(defmethod progress :error [m]
  (when @bar
    (swap! bar assoc :failed? true)
    (print-bar)))

(def report [progress report/result])
