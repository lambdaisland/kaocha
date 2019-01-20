(ns kaocha.plugin-test
  (:require [kaocha.plugin :as plugin]
            [clojure.test :refer :all]
            [kaocha.test-util :as util]
            [kaocha.output :as output])
  (:import (clojure.lang ExceptionInfo)))

(deftest missing-plugin-test
  (is (thrown-with-msg? ExceptionInfo
                        #"Couldn't load plugin :kaocha.missing.plugin/gone"
                        (plugin/load-all [:kaocha.missing.plugin/gone])))
  (is (= {:err "ERROR: Couldn't load plugin :kaocha.missing.plugin/gone\n" :out "" :result nil}
         (binding [output/*colored-output* false]
           (util/with-out-err
             (try
               (plugin/load-all [:kaocha.missing.plugin/gone])
               (catch ExceptionInfo e
                 nil)))))))
