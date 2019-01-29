(ns kaocha.assertions
  (:require [kaocha.output :as output]
            [clojure.test :as t]
            [kaocha.report :as report]
            [puget.color :as color]
            [clojure.string :as str]
            [slingshot.slingshot :refer [try+ throw+]]))

(defmethod t/assert-expr 'substring? [msg form]
  (let [[_ s1 s2] form]
    `(if (.contains ~s2 ~s1)
       (t/do-report {:type :pass :message ~msg})
       (t/do-report {:type :fail
                     :expected (list '~'substring? ~s1 ~s2)
                     :actual (list '~'not '~form)
                     :message ~msg}))))

(def x-last
  "Reducing version of [[clojure.core/last]]"
  (completing (fn [_ x] x)))

(defn longest-substring [s1 s2]
  (transduce (comp (map #(subs s1 0 (inc %)))
                   (take-while #(.contains s2 %)))
             x-last
             nil
             (range (count s1))))

(defn show-trailing-whitespace [s]
  (str/replace s
               #"(?m)[ \h\x0B\f\r\x85\u2028\u2029]+$"
               (fn [s]
                 (output/colored :red-bg s))))

(defmethod report/print-expr 'substring? [{:keys [expected] :as m}]
  (let [[_ s1 s2] expected
        long-sub (longest-substring s1 s2)
        remainder (subs s1 (count long-sub))
        printer (output/printer {:color-scheme {::long-sub [:green]
                                                ::header   [:blue]}})]
    (output/print-doc
     [:span
      "Expected: (substring? needle haystack)"
      :break
      (color/document printer ::header (output/colored :underline "Haystack:"))
      :break
      (show-trailing-whitespace s2)
      :break
      (color/document printer ::header (output/colored :underline "Needle:"))
      :break
      (color/document printer ::long-sub long-sub)
      (show-trailing-whitespace remainder)])))

#_
(defmethod t/assert-expr 'thrown+? [msg form]
  (let [expr (second form)
        body (nthnext form 2)]
    `(try+
      ~@body
      (t/do-report {:type :fail, :message ~msg,
                    :expected '~form, :actual nil})
      (catch ~expr e#
        (t/do-report {:type :pass, :message ~msg,
                      :expected '~form, :actual e#})
        e#))))

;; Configured as a pre-load hook
(defn load-assertions [config]
  (require 'matcher-combinators.test)
  config)
