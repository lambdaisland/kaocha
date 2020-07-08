(ns strict-focus-meta-hook
  (:require [kaocha.repl :as repl]
            [clojure.walk :as w]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.testable :as testable]))

;; #kaocha @madstap 2020-07-08 2:50 PM
;;
;; Hi, when using kaocha in our ci we were surprised by the behavior of
;; --focus-meta when there are no tests tagged with that metadata, which is to
;; run all tests. What we would like is to run no tests and succeed, because not
;; all our projects will have that kind of test. Is there a way to configure
;; this?

#_
(def plan (repl/test-plan))

(defn my-post-load-hook [test-plan]
  (w/postwalk
   (fn [testable]
     (if (and (hierarchy/leaf? testable) (not (:really-focus (::testable/meta testable))))
       (assoc testable ::testable/skip true)
       testable))
   plan))


(comment
  ;; tests.edn:
  ;; #kaocha/v1
  {:plugins [:hooks]
   :kaocha.hooks/post-load [strict-focus-meta-hook/my-post-load-hook]})
