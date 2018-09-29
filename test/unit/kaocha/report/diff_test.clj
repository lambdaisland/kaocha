(ns kaocha.report.diff-test
  (:require [clojure.test :refer :all]
            [kaocha.report.diff :as diff]))

(defmacro ^{:style/indent [1]} context [& args] `(testing ~@args))

(run
  (deftest diff-test

    (testing "diffing atoms"
      (context "nil"
        (is (= (diff/->Mismatch nil 1)
               (diff/diff nil 1))))

      (context "when different"
        (is (= (diff/->Mismatch :a :b)
               (diff/diff :a :b))))

      (context "when equal"
        (is (= :a
               (diff/diff :a :a)))))

    (testing "diffing collections"
      (context "different types"
        (is (= (diff/->Mismatch [1 2 3] #{1 2 3})
               (diff/diff [1 2 3] #{1 2 3}))))

      (context "sequences"
        (is (= []
               (diff/diff [] [])))

        (is (= [1 2 3]
               (diff/diff (into-array [1 2 3]) [1 2 3])))

        (is (= [:a]
               (diff/diff [:a] [:a])))

        (is (= [:a (diff/->Deletion :b) :c (diff/->Insertion :d)]
               (diff/diff [:a :b :c] [:a :c :d])))

        (is (= [:a (diff/->Deletion :b) :c (diff/->Insertion :d)]
               (diff/diff (list :a :b :c) (list :a :c :d))))

        (is (= [(diff/->Insertion :a)]
               (diff/diff [] [:a])))

        (is (= [(diff/->Deletion :a)]
               (diff/diff [:a] [])))

        (is (= [(diff/->Insertion :x) (diff/->Insertion :y) :a]
               (diff/diff [:a] [:x :y :a])))

        (is (= [:a (diff/->Mismatch :b :x) (diff/->Insertion :y) :c]
               (diff/diff [:a :b :c] [:a :x :y :c])))

        (is (= [{:x (diff/->Mismatch 1 2)}]
               (diff/diff [{:x 1}] [{:x 2}]))))

      (context "sets"
        (is (= #{:a}
               (diff/diff #{:a} #{:a})))

        (is (= #{(diff/->Insertion :a)}
               (diff/diff #{} #{:a})))

        (is (= #{(diff/->Deletion :a)}
               (diff/diff #{:a} #{})))

        (is (= #{(diff/->Deletion :a) :b :c}
               (diff/diff #{:a :b :c} #{:c :b}))))

      (context "maps"
        (is (= {} (diff/diff {} {})))

        (is (= {:a (diff/->Mismatch 1 2)}
               (diff/diff {:a 1} {:a 2})))

        (is (= {:a (diff/->Mismatch 1 2)
                (diff/->Deletion :b) 2
                (diff/->Insertion :x) 2
                :c 3}
               (diff/diff {:a 1 :b 2 :c 3} {:a 2 :x 2 :c 3})))

        (is (= {:a [1 (diff/->Deletion 2) 3]}
               (diff/diff {:a [1 2 3]} {:a [1 3]})))))))

(comment
  (use 'kaocha.repl))
