Feature: Focusing and skipping tests

  Background:
    Given shared test fixtures
    And the following test configuration
    """
    #kaocha/v1
    {:reporter   [kaocha.report/documentation]
     :randomize? false
     :color?     false

     :tests      [{:id                  :a
                   :test-paths          ["fixtures/a-tests"]
                   :kaocha.filter/focus [foo.bar-test/a-test]}
                  {:id                  :b
                   :test-paths          ["fixtures/b-tests"]}]}
    """


  Scenario: Focusing on a group of tests
    When I run Kaocha with "--focus finn.finn-test"
    Then the output should contain
    """
    --- b (clojure.test) ---------------------------
    finn.finn-test
      the-test

    1 tests, 1 assertions, 0 failures.
    """
