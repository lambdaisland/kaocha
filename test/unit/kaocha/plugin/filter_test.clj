(ns kaocha.plugin.filter-test
  (:require [clojure.test :refer [is]]
            [kaocha.test :refer [deftest]]
            [kaocha.plugin.filter :as f]
            [kaocha.testable :as testable]))

(defn flat-test-seq [t]
  (cons t (mapcat flat-test-seq (:kaocha.test-plan/tests t))))

(defn skipped-tests [testable]
  (into {}
        (for [{:kaocha.testable/keys [id skip]} (flat-test-seq testable)]
          [id (boolean skip)])))

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
  (is (= #:kaocha.testable
         {:id :foo.bar/baz, :skip true}
         (f/filter-testable {:kaocha.testable/id :foo.bar/baz}
                            {:skip '[foo.bar]})))

  (is (= {:kaocha.testable/id     :x/_1
          :kaocha.test-plan/tests [#:kaocha.testable{:id :y/_1, :skip true}]}
         (f/filter-testable {:kaocha.testable/id     :x/_1
                             :kaocha.test-plan/tests [{:kaocha.testable/id :y/_1}]}
                            {:skip '[y/_1]})))

  (is (= {:kaocha.testable/id :x/_1, :kaocha.test-plan/tests [#:kaocha.testable{:id :y/_1}]}
         (f/filter-testable {:kaocha.testable/id     :x/_1
                             :kaocha.test-plan/tests [{:kaocha.testable/id :y/_1}]}
                            {:focus '[x/_1]})))

  (is (= {:kaocha.testable/id     :x/_1,
          :kaocha.test-plan/tests [#:kaocha.testable{:id   :y/_1
                                                     :skip true}
                                   #:kaocha.testable{:id :z/_1}]}

         (f/filter-testable {:kaocha.testable/id     :x/_1
                             :kaocha.test-plan/tests [{:kaocha.testable/id :y/_1}
                                                      {:kaocha.testable/id :z/_1}]}
                            {:focus '[z/_1]})))

  (is (= {:kaocha.testable/id     :x/_1
          :kaocha.test-plan/tests [{:kaocha.testable/id   :y/_1
                                    :kaocha.testable/skip true}
                                   {:kaocha.testable/id :y/_2
                                    :kaocha.test-plan/tests
                                    [{:kaocha.testable/id   :z/_1
                                      :kaocha.testable/skip true}
                                     {:kaocha.testable/id :z/_2}]}]}
         (f/filter-testable {:kaocha.testable/id     :x/_1
                             :kaocha.test-plan/tests [{:kaocha.testable/id :y/_1}
                                                      {:kaocha.testable/id :y/_2
                                                       :kaocha.test-plan/tests
                                                       [{:kaocha.testable/id :z/_1}
                                                        {:kaocha.testable/id :z/_2}]}]}
                            {:focus '[y/_2] :skip '[z/_1]})))

  (is (= {:kaocha.testable/id     :x,
          :kaocha.test-plan/tests [{:kaocha.testable/id     :y,
                                    :kaocha.test-plan/tests [#:kaocha.testable{:id :z}
                                                             #:kaocha.testable{:id :z/_2}]}]}
         (f/filter-testable {:kaocha.testable/id     :x
                             :kaocha.test-plan/tests [{:kaocha.testable/id :y
                                                       :kaocha.test-plan/tests
                                                       [{:kaocha.testable/id :z}
                                                        {:kaocha.testable/id :z/_2}]}]}
                            {:focus '[z]})))
  (is (= {:kaocha.testable/id  :x,
          :kaocha.filter/focus [:z/_2],
          :kaocha.test-plan/tests
          [{:kaocha.testable/id :y
            :kaocha.test-plan/tests
            [#:kaocha.testable{:id   :z
                               :skip true}
             #:kaocha.testable{:id :z/_2}]}]}
         (f/filter-testable {:kaocha.testable/id     :x
                             :kaocha.filter/focus    [:z/_2]
                             :kaocha.test-plan/tests [{:kaocha.testable/id :y
                                                       :kaocha.test-plan/tests
                                                       [{:kaocha.testable/id :z}
                                                        {:kaocha.testable/id :z/_2}]}]}
                            {})))


  ;; These cases need more hammock time to figure out what the "right" behavior should be
  #_
  (is (= {:base false
          :x    false
          :a    false :b false
          :y    false
          :c    true  :d false}
         (skipped-tests (f/filter-testable {:kaocha.testable/id     :base
                                            :kaocha.test-plan/tests [{:kaocha.testable/id     :x
                                                                      :kaocha.test-plan/tests [{:kaocha.testable/id :a}
                                                                                               {:kaocha.testable/id   :b
                                                                                                :kaocha.testable/meta {:foo true}}]}
                                                                     {:kaocha.testable/id     :y
                                                                      :kaocha.test-plan/tests [{:kaocha.testable/id :c}
                                                                                               {:kaocha.testable/id   :d
                                                                                                :kaocha.testable/meta {:foo true}}]}]}
                                           {:focus      [:x]
                                            :focus-meta [:foo]}))

         )))


(deftest filter-focus-and-focus-meta-separately-test
  (is (= {:kaocha.test-plan/tests [{:kaocha.testable/id :negative
                                    :kaocha.test-plan/tests
                                    [{:kaocha.testable/id :foo.bar-test
                                      :kaocha.test-plan/tests [{:kaocha.testable/id :foo.bar-test/some-positive-test
                                                                :kaocha.testable/meta {:positive true}
                                                                :kaocha.testable/skip true}
                                                               {:kaocha.testable/id :foo.bar-test/some-negative-test
                                                                :kaocha.testable/meta {:negative true}}
                                                               {:kaocha.testable/id :foo.bar-test/some-random-test
                                                                :kaocha.testable/meta {:random true}
                                                                :kaocha.testable/skip true}]}]
                                    :kaocha.testable/skip true}
                                   {:kaocha.testable/id :positive
                                    :kaocha.test-plan/tests [{:kaocha.testable/id :foo.bar-test
                                                              :kaocha.test-plan/tests
                                                              [{:kaocha.testable/id :foo.bar-test/some-positive-test
                                                                :kaocha.testable/meta {:positive true}}
                                                               {:kaocha.testable/id :foo.bar-test/some-negative-test
                                                                :kaocha.testable/meta {:negative true}
                                                                :kaocha.testable/skip true}
                                                               {:kaocha.testable/id :foo.bar-test/some-random-test
                                                                :kaocha.testable/meta {:random true}
                                                                :kaocha.testable/skip true}]}]}]
          :kaocha.filter/focus #{:positive}
          :kaocha.filter/focus-meta #{}}
         (f/filter-post-load-hook {:kaocha.test-plan/tests [{:kaocha.testable/id :negative
                                                             :kaocha.test-plan/tests [{:kaocha.testable/id :foo.bar-test
                                                                                       :kaocha.test-plan/tests
                                                                                       [{:kaocha.testable/id :foo.bar-test/some-positive-test
                                                                                         :kaocha.testable/meta {:positive true}}
                                                                                        {:kaocha.testable/id :foo.bar-test/some-negative-test
                                                                                         :kaocha.testable/meta {:negative true}}
                                                                                        {:kaocha.testable/id :foo.bar-test/some-random-test
                                                                                         :kaocha.testable/meta {:random true}}]}]
                                                             :kaocha.filter/focus-meta #{:negative}}
                                                            {:kaocha.testable/id :positive
                                                             :kaocha.test-plan/tests [{:kaocha.testable/id :foo.bar-test
                                                                                       :kaocha.test-plan/tests
                                                                                       [{:kaocha.testable/id :foo.bar-test/some-positive-test
                                                                                         :kaocha.testable/meta {:positive true}}
                                                                                        {:kaocha.testable/id :foo.bar-test/some-negative-test
                                                                                         :kaocha.testable/meta {:negative true}}
                                                                                        {:kaocha.testable/id :foo.bar-test/some-random-test
                                                                                         :kaocha.testable/meta {:random true}}]}]
                                                             :kaocha.filter/focus-meta #{:positive}}]
                                   :kaocha.filter/focus #{:positive}}))))

