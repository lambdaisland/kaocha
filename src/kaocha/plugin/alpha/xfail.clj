(ns kaocha.plugin.alpha.xfail
  "This plugin inverses fail & pass for tests marked with the ^:kaocha/xfail
  metadata. A failing test can be marked with this metadata, and will now be
  considered passing. Once the test passes again, it fails.

  Note that this current implementation inverses each assertion in turn, so if
  you have multiple assertions in a xfail test, then they all must fail for the
  test to pass."
  (:require [kaocha.plugin :as plugin]
            [kaocha.testable :as testable]
            [kaocha.hierarchy :as hierarchy]))

(plugin/defplugin kaocha.plugin.alpha/xfail
  (post-load [test-plan]
    (update
     test-plan
     :kaocha/reporter
     (fn [reporter]
       (fn [event]
         (reporter
          (if (-> event :kaocha/testable ::testable/meta :kaocha/xfail)
            (cond
              (hierarchy/isa? (:type event) :kaocha/fail-type)
              (assoc event :type :pass)

              (= (:type m) :pass)
              (assoc event :type :fail)

              :else
              event)
            event)))))))
