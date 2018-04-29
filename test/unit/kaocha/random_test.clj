(ns kaocha.random-test
  (:require [clojure.test :refer :all]
            [kaocha.random :refer :all]))

(deftest rand-ints-test
  (is (= (take 10 (repeatedly (rng 321098)))
         [-141996321 1985580023 -305308934 1158906095 -1212597759 -42859192 -98240991 1350981438 31847656 -294128715]))

  (is (= (take 10 (repeatedly (rng 1)))
         [-1155869325 431529176 1761283695 1749940626 892128508 155629808 1429008869 -1465154083 -138487339 -1242363800])))

(deftest randomize-tests-test
  (is (= (randomize-tests 1 '{first.ns [first.var1 first.var2 first.var3]
                              other.ns [other.var1 other.var2 other.var3]
                              third.ns [third.var1 third.var2 third.var3]})
         '([first.ns (first.var2 first.var1 first.var3)]
           [other.ns (other.var1 other.var2 other.var3)]
           [third.ns (third.var3 third.var2 third.var1)])))

  (is (= (randomize-tests 431 '{first.ns [first.var1 first.var2 first.var3]
                                other.ns [other.var1 other.var2 other.var3]
                                third.ns [third.var1 third.var2 third.var3]})
         '([other.ns (other.var3 other.var2 other.var1)]
           [third.ns (third.var1 third.var3 third.var2)]
           [first.ns (first.var3 first.var2 first.var1)]))))
