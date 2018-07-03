(ns kaocha.plugin.filter-test
  (:require [clojure.test :refer :all]
            [kaocha.plugin.filter :as filter]))


(deftest matches?-test
  (is (filter/matches? {:kaocha.testable/id :foo.bar/baz}
                       '[foo.bar/baz] []))

  (is (filter/matches? {:kaocha.testable/id :foo.bar/baz}
                       '[foo.bar] []))

  (is (filter/matches? {:kaocha.testable/meta {:foo.bar/baz true}}
                       [] '[foo.bar/baz]))

  (is (filter/matches? {:kaocha.testable/id :foo.bar}
                       '[foo.bar] [])))

(deftest filter-testable-test
  (is (= (filter/filter-testable {:kaocha.testable/id :foo.bar/baz}
                                 {:skip '[foo.bar]})
         #:kaocha.testable{:id :foo.bar/baz, :skip true}))

  (is (= (filter/filter-testable {:kaocha.testable/id :x/_1
                                  :kaocha.test-plan/tests [{:kaocha.testable/id :y/_1}]}
                                 {:skip '[y/_1]})
         {:kaocha.testable/id :x/_1
          :kaocha.test-plan/tests [#:kaocha.testable{:id :y/_1, :skip true}]}))

  (is (= (filter/filter-testable {:kaocha.testable/id :x/_1
                                  :kaocha.test-plan/tests [{:kaocha.testable/id :y/_1}]}
                                 {:focus '[x/_1]})
         {:kaocha.testable/id :x/_1, :kaocha.test-plan/tests [#:kaocha.testable{:id :y/_1}]}))

  (is (= (filter/filter-testable {:kaocha.testable/id :x/_1
                                  :kaocha.test-plan/tests [{:kaocha.testable/id :y/_1}
                                                           {:kaocha.testable/id :z/_1}]}
                                 {:focus '[z/_1]})

         {:kaocha.testable/id :x/_1,
          :kaocha.test-plan/tests [#:kaocha.testable{:id :y/_1
                                                     :skip true}
                                   #:kaocha.testable{:id :z/_1}]}))

  (is (= (filter/filter-testable {:kaocha.testable/id :x/_1
                                  :kaocha.test-plan/tests       [{:kaocha.testable/id :y/_1}
                                                                 {:kaocha.testable/id :y/_2
                                                                  :kaocha.test-plan/tests
                                                                  [{:kaocha.testable/id :z/_1}
                                                                   {:kaocha.testable/id :z/_2}]}]}
                                 {:focus '[y/_2] :skip '[z/_1]})
         {:kaocha.testable/id :x/_1
          :kaocha.test-plan/tests       [{:kaocha.testable/id   :y/_1
                                          :kaocha.testable/skip true}
                                         {:kaocha.testable/id :y/_2
                                          :kaocha.test-plan/tests
                                          [{:kaocha.testable/id   :z/_1
                                            :kaocha.testable/skip true}
                                           {:kaocha.testable/id :z/_2}]}]}))

  (is (= (filter/filter-testable {:kaocha.testable/id     :x
                                  :kaocha.test-plan/tests [{:kaocha.testable/id :y
                                                            :kaocha.test-plan/tests
                                                            [{:kaocha.testable/id :z}
                                                             {:kaocha.testable/id :z/_2}]}]}
                                 {:focus '[z]})
         {:kaocha.testable/id :x,
          :kaocha.test-plan/tests [{:kaocha.testable/id :y,
                                    :kaocha.test-plan/tests [#:kaocha.testable{:id :z}
                                                             #:kaocha.testable{:id :z/_2}]}]}))
  (is (= (filter/filter-testable {:kaocha.testable/id     :x
                                  :kaocha.filter/focus    [:z/_2]
                                  :kaocha.test-plan/tests [{:kaocha.testable/id :y
                                                            :kaocha.test-plan/tests
                                                            [{:kaocha.testable/id :z}
                                                             {:kaocha.testable/id :z/_2}]}]}
                                 {})
         {:kaocha.testable/id :x,
          :kaocha.filter/focus [:z/_2],
          :kaocha.test-plan/tests
          [{:kaocha.testable/id :y
            :kaocha.test-plan/tests
            [#:kaocha.testable{:id :z
                               :skip true}
             #:kaocha.testable{:id :z/_2}]}]})))
