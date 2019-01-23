(ns kaocha.plugin.filter-test
  (:require [clojure.test :refer [is]]
            [kaocha.test :refer [deftest]]
            [kaocha.plugin.filter :as f]
            [kaocha.testable :as testable]))

(deftest matches?-test
  (is (f/matches? {:kaocha.testable/id :foo.bar/baz}
                  '[foo.bar/baz]
                  []))

  (is (f/matches? {:kaocha.testable/id :foo.bar/baz}
                  '[foo.bar]
                  []))

  (is (f/matches? {:kaocha.testable/meta {:foo.bar/baz true}}
                  []
                  '[foo.bar/baz]))

  (is (f/matches? {:kaocha.testable/id :foo.bar}
                  '[foo.bar]
                  [])))

(deftest filters-test
  (is (= '{:skip [skip]
           :focus [focus]
           :skip-meta [:skip-meta]
           :focus-meta [:focus-meta]}
         (f/filters '#:kaocha.filter{:skip [skip]
                                     :focus [focus]
                                     :skip-meta [:skip-meta]
                                     :focus-meta [:focus-meta]}))))

(deftest merge-filters-test
  (is (= {:skip () :skip-meta () :focus nil :focus-meta nil}
         (f/merge-filters {} {})))

  (is (= {:skip '[foo bar] :skip-meta () :focus nil :focus-meta nil}
         (f/merge-filters
          {:skip '[foo]}
          {:skip '[bar]})))

  (is (= {:skip () :skip-meta () :focus '[bar] :focus-meta nil}
         (f/merge-filters
          {:focus '[foo]}
          {:focus '[bar]})))

  (is (= {:skip () :skip-meta () :focus '[foo] :focus-meta nil}
         (f/merge-filters
          {:focus '[foo]}
          {:focud '[]}))))

(deftest truthy-keys-test
  (is (= [:zzz]
         (f/truthy-keys {:xxx false
                         :yyy nil
                         :zzz true}))))

(deftest remove-missing-metadata-keys-test
  (is (= #{:xxx}
         (f/remove-missing-metadata-keys
          [:xxx :yyy]
          {:kaocha.test-plan/tests [{:kaocha.testable/meta {:xxx true}}]}))))

(deftest filter-testable-test
  (is (= (f/filter-testable {:kaocha.testable/id :foo.bar/baz}
                            {:skip '[foo.bar]})
         #:kaocha.testable{:id :foo.bar/baz, :skip true}))

  (is (= (f/filter-testable {:kaocha.testable/id :x/_1
                             :kaocha.test-plan/tests [{:kaocha.testable/id :y/_1}]}
                            {:skip '[y/_1]})
         {:kaocha.testable/id :x/_1
          :kaocha.test-plan/tests [#:kaocha.testable{:id :y/_1, :skip true}]}))

  (is (= (f/filter-testable {:kaocha.testable/id :x/_1
                             :kaocha.test-plan/tests [{:kaocha.testable/id :y/_1}]}
                            {:focus '[x/_1]})
         {:kaocha.testable/id :x/_1, :kaocha.test-plan/tests [#:kaocha.testable{:id :y/_1}]}))

  (is (= (f/filter-testable {:kaocha.testable/id :x/_1
                             :kaocha.test-plan/tests [{:kaocha.testable/id :y/_1}
                                                      {:kaocha.testable/id :z/_1}]}
                            {:focus '[z/_1]})

         {:kaocha.testable/id :x/_1,
          :kaocha.test-plan/tests [#:kaocha.testable{:id :y/_1
                                                     :skip true}
                                   #:kaocha.testable{:id :z/_1}]}))

  (is (= (f/filter-testable {:kaocha.testable/id :x/_1
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

  (is (= (f/filter-testable {:kaocha.testable/id     :x
                             :kaocha.test-plan/tests [{:kaocha.testable/id :y
                                                       :kaocha.test-plan/tests
                                                       [{:kaocha.testable/id :z}
                                                        {:kaocha.testable/id :z/_2}]}]}
                            {:focus '[z]})
         {:kaocha.testable/id :x,
          :kaocha.test-plan/tests [{:kaocha.testable/id :y,
                                    :kaocha.test-plan/tests [#:kaocha.testable{:id :z}
                                                             #:kaocha.testable{:id :z/_2}]}]}))
  (is (= (f/filter-testable {:kaocha.testable/id     :x
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
