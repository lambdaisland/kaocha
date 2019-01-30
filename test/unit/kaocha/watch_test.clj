(ns kaocha.watch-test
  (:require [clojure.test :refer :all]
            [kaocha.watch :as w]
            [kaocha.test-util :as util]
            [lambdaisland.tools.namespace.dir :as ctn-dir]
            [lambdaisland.tools.namespace.track :as ctn-track]
            [lambdaisland.tools.namespace.reload :as ctn-reload]
            [kaocha.integration-helpers :as integration]
            [clojure.java.io :as io]
            [kaocha.config :as config]
            [clojure.test :as t]
            [clojure.string :as str])
  (:import [java.io File]))

(deftest make-queue-test
  (is (instance? java.util.concurrent.BlockingQueue (w/make-queue))))

(deftest put-poll-test
  (let [q (w/make-queue)]
    (w/qput q :x)
    (w/qput q :y)
    (is (= :x (w/qpoll q)))
    (is (= :y (w/qpoll q)))))

(deftest drain-queue-test
  (let [q (w/make-queue)]
    (w/qput q :x)
    (w/qput q :y)
    (w/drain-queue! q)
    (is (empty? q))))

(def freshly-loaded? true)

(deftest track-reload!-test
  (alter-var-root #'freshly-loaded? (constantly false))
  (let [tracker (-> (ctn-track/tracker)
                    (ctn-dir/scan-dirs ["test/unit/kaocha/watch_test.clj"])
                    w/track-reload!)]
    (is @(find-var `freshly-loaded?))))

(deftest print-scheduled-operations-test
  (is (= "[watch] Unloading #{baq.ok}
[watch] Loading #{bar.baz}
[watch] Reloading #{foo.bar}
[watch] Re-running failed tests #{foos.ball}\n"
         (with-out-str
           (w/print-scheduled-operations! {::ctn-track/load '(foo.bar bar.baz)
                                           ::ctn-track/unload '(foo.bar baq.ok)}
                                          '[foos.ball])))))

(deftest glob-test
  (is (w/glob? (.toPath (io/file "xxxx.clj")) ["xxx*"]))
  (is (not (w/glob? (.toPath (io/file "xxxx.clj")) ["xxy*"]))))

(deftest reload-config-test
  (is (match?
       {:kaocha/tests [{:kaocha.testable/id :foo}]}
       (let [tmp-file (File/createTempFile "tests" ".edn")]
         (spit tmp-file "#kaocha/v1 {:tests [{:id :foo}]}")
         (first (w/reload-config {:kaocha/cli-options {:config-file (str tmp-file)}}))))))

(deftest watch-test
  (let [{:keys [config-file test-dir] :as m} (integration/test-dir-setup {})
        config (-> (config/load-config config-file)
                   (assoc-in [:kaocha/cli-options :config-file] (str config-file))
                   (assoc-in [:kaocha/tests 0 :kaocha/source-paths] [])
                   (assoc-in [:kaocha/tests 0 :kaocha/test-paths] [(str test-dir)]))
        prefix (str (gensym "foo"))
        finish? (atom false)
        q       (w/make-queue)
        out-str (promise)]
    (integration/spit-file m "tests.edn" (prn-str config))
    (integration/spit-file m (str "test/" prefix "/bar_test.clj") (str "(ns " prefix ".bar-test (:require [clojure.test :refer :all])) (deftest xxx-test (is (= :xxx :yyy)))"))

    (future (deliver out-str (util/with-test-out-str
                               (t/with-test-out
                                 (util/with-test-ctx
                                   (w/run* config finish? q))))))

    (Thread/sleep 100)
    (integration/spit-file m (str "test/" prefix "/bar_test.clj") (str "(ns " prefix ".bar-test (:require [clojure.test :refer :all])) (deftest xxx-test (is (= :xxx :zzz)))"))
    (Thread/sleep 100)
    (reset! finish? true)
    (w/qput q :finish)

    (is (str/replace "[(F)]\n\nFAIL in foo.bar-test/xxx-test (bar_test.clj:1)\nExpected:\n  :xxx\nActual:\n  -:xxx +:yyy\n1 tests, 1 assertions, 1 failures.\n\n[watch] Reloading #{foo.bar-test}\n[watch] Re-running failed tests #{:foo.bar-test/xxx-test}\n[(F)]\n\nFAIL in foo.bar-test/xxx-test (bar_test.clj:1)\nExpected:\n  :xxx\nActual:\n  -:xxx +:zzz\n1 tests, 1 assertions, 1 failures.\n\n[watch] watching stopped.\n"
                     "foo"
                     prefix)
        @out-str)))

(deftest watch-test-do-not-trigger-on-dir-changes
  (let [{:keys [config-file test-dir] :as m} (integration/test-dir-setup {})
        config (-> (config/load-config config-file)
                   (assoc-in [:kaocha/cli-options :config-file] (str config-file))
                   (assoc-in [:kaocha/tests 0 :kaocha/source-paths] [])
                   (assoc-in [:kaocha/tests 0 :kaocha/test-paths] [(str test-dir)]))
        finish? (atom false)
        q       (w/make-queue)
        out-str (promise)]
    (integration/spit-file m "tests.edn" (prn-str config))
    (integration/spit-dir m "test/somedir")

    (future (deliver out-str (util/with-test-out-str
                               (t/with-test-out
                                 (util/with-test-ctx
                                   (w/run* config finish? q))))))

    (Thread/sleep 100)
    (w/qput q (clojure.java.io/file (.resolve (:dir m) "test/somedir")))
    (Thread/sleep 100)
    (reset! finish? true)
    (w/qput q :finish)

    (is (= "[]\n0 tests, 0 assertions, 0 failures.\n\n[watch] watching stopped.\n" @out-str))))
