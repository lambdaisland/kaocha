(ns kaocha.output-test
  (:require [kaocha.output :as output]
            [clojure.test :refer :all]
            [kaocha.test-util :as util]))

(deftest colored-test
  (is (= "[32mfoo[m" (output/colored :green "foo")))

  (is (= "foo"
         (binding [output/*colored-output* false]
           (output/colored :green "foo")))))

(deftest  warn-test
  (testing "without color"
    (is (= {:err "WARNING: Oh no!\n", :out "", :result nil}
           (binding [output/*colored-output* false]
             (util/with-out-err
               (output/warn "Oh no!"))))))

  (testing "with color"
    (is (= {:err "[33mWARNING: [mOh no!\n", :out "", :result nil}
           (util/with-out-err
             (output/warn "Oh no!")))))

  (testing "multiple arguments"
    (is (= {:err "[33mWARNING: [mone mississippi, two mississippi\n", :out "", :result nil}
           (util/with-out-err
             (output/warn "one mississippi" ", " "two mississippi"))))))

(deftest  error-test
  (testing "without color"
    (is (= {:err "ERROR: Oh no!\n", :out "", :result nil}
           (binding [output/*colored-output* false]
             (util/with-out-err
               (output/error "Oh no!"))))))

  (testing "with color"
    (is (= {:err "[31mERROR: [mOh no!\n", :out "", :result nil}
           (util/with-out-err
             (output/error "Oh no!")))))

  (testing "multiple arguments"
    (is (= {:err "[31mERROR: [mone mississippi, two mississippi\n", :out "", :result nil}
           (util/with-out-err
             (output/error "one mississippi" ", " "two mississippi"))))))

(deftest format-doc-test
  (testing "without color"
    (is (= '[:group "[" [:align ([:group "{" [:align ([:span ":x" " " ":y"])] "}"])] "]"]
           (binding [output/*colored-output* false]
             (output/format-doc [{:x :y}])))))

  (testing "with color"
    (is (= '[:group
             [:span [:pass "[1;31m"] "[" [:pass "[0m"]]
             [:align
              ([:group
                [:span [:pass "[1;31m"] "{" [:pass "[0m"]]
                [:align
                 ([:span
                   [:span [:pass "[1;33m"] ":x" [:pass "[0m"]]
                   " "
                   [:span [:pass "[1;33m"] ":y" [:pass "[0m"]]])]
                [:span [:pass "[1;31m"] "}" [:pass "[0m"]]])]
             [:span [:pass "[1;31m"] "]" [:pass "[0m"]]]
           (output/format-doc [{:x :y}])))))


(deftest print-doc-test
  (testing "prints with fipp"
    (is (= {:err ""
            :out "[1;31m[[0m[1;33m:aaa[0m [1;33m:bbb[0m [1;33m:ccc[0m[1;31m][0m\n",
            :result nil}
           (util/with-out-err
             (-> (output/format-doc [:aaa :bbb :ccc])
                 (output/print-doc))))))

  (testing "respects *print-length*"
    (is (= {:err "",
            :out "[1;31m[[0m[1;33m:aaa[0m\n[1;33m :bbb[0m\n[1;33m :ccc[0m[1;31m][0m\n",
            :result nil}
           (util/with-out-err
             (binding [*print-length* 1]
               (-> (output/format-doc [:aaa :bbb :ccc])
                   (output/print-doc))))))))
