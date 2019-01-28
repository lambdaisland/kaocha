(ns kaocha.watch-test
  (:require [clojure.test :refer :all]
            [kaocha.watch :as w]
            [lambdaisland.tools.namespace.dir :as ctn-dir]
            [lambdaisland.tools.namespace.track :as ctn-track]
            [clojure.java.io :as io])
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
