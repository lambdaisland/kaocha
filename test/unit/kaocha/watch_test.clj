(ns kaocha.watch-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as spec]
            [kaocha.watch :as w]
            [kaocha.platform :as platform]
            [kaocha.test-util :as util]
            [lambdaisland.tools.namespace.dir :as ctn-dir]
            [clojure.java.shell :as shell]
            [lambdaisland.tools.namespace.track :as ctn-track]
            [lambdaisland.tools.namespace.reload :as ctn-reload]
            [kaocha.integration-helpers :as integration]
            [clojure.java.io :as io]
            [kaocha.config :as config]
            [clojure.test :as t]
            [clojure.string :as str]
            [slingshot.slingshot :refer [try+]]
            [matcher-combinators.matchers :as matchers])
  (:import (java.io File)))

(require 'matcher-combinators.test)

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

(deftest convert-test
  (is (= "src/**" (w/convert "src/")))
  (is (= "src/**" (w/convert "src/ ")))
  (is (= "**.html" (w/convert "*.html")))
  (is (= "src/\\{ill-advised-filename\\}.clj" (w/convert "src/{ill-advised-filename}.clj")))
  (is (= "README.md" (w/convert "README.md")))
  (is (= "README.md" (w/convert "README.md "))))

(deftest glob-converted-unchanged-test
  ; Validate that compatible patterns still match/fail to match after conversion.
  (is (w/glob? (.toPath (io/file "xxxx.clj")) [(w/convert "xxx*")]))
  (is (w/glob? (.toPath (io/file "x.class")) [(w/convert "[a-z].class")]))
  (is (not (w/glob? (.toPath (io/file "xxxx.clj")) [(w/convert "xxy*")])))
  (is (w/glob? (.toPath (io/file "xxxx.clj")) [(w/convert "**xxx.clj")]))
  (is (w/glob? (.toPath (io/file "test/xxxx.clj")) [(w/convert "**xxx.clj")]))
  (is (w/glob? (.toPath (io/file "test/xxxx.clj")) [(w/convert "***xxx.clj")])))

(deftest glob-converted-test
  ; Validate that incompatible patterns are converted and match after conversion.
  (is (w/glob? (.toPath (io/file "xxxx.clj")) [(w/convert "xxx* ")]))
  (is (w/glob? (.toPath (io/file "xxxx.clj")) [(w/convert "xxx*  ")]))
  (when-not (platform/on-windows?)
    (is (w/glob? (.toPath (io/file "xxxx.clj ")) [(w/convert "xxx*\\ ")])))
  (when-not (platform/on-windows?)
   (is (w/glob? (.toPath (io/file "xxxx.clj ")) [(w/convert "xxx*\\  ")])))
  (when-not (platform/on-windows?)
   (is (w/glob? (.toPath (io/file "xxxx.clj  ")) [(w/convert "xxx*\\ \\ ")])))
  (is (w/glob? (.toPath (io/file "src/xxx.class")) [(w/convert "src/")]))
  (is (w/glob? (.toPath (io/file "src/xxx.class")) [(w/convert "*.class")]))
  (is (w/glob? (.toPath (io/file "src/clj/test.tmp")) [(w/convert "src/**/test.tmp")]))
  (is (w/glob? (.toPath (io/file "src/test.tmp")) [(w/convert "src/**/test.tmp")]))
  (is (w/glob? (.toPath (io/file "src/test/xxx.clj")) [(w/convert "*rc/test/")]))
  (is (w/glob? (.toPath (io/file "src/xxx.clj")) [(w/convert "*rc/")]))
  (is (w/glob? (.toPath (io/file "src/test2.tmp")) [(w/convert "src/**/*.tmp")]))

  (is (not (w/glob? (.toPath (io/file "src/clj/test.tmp")) [(w/convert "src**test.tmp")])))
  (is (not (w/glob? (.toPath (io/file "src/test.tmp")) [(w/convert "src**test.tmp")])))
  (is (not (w/glob? (.toPath (io/file "src/ill-advised-filename.clj")) [(w/convert "src/{ill-advised-filename}.clj")]))))

(deftest reload-config-test
  (is (match?
       {:kaocha/tests [{:kaocha.testable/id :foo}]}
       (let [tmp-file (File/createTempFile "tests" ".edn")]
         (spit tmp-file "#kaocha/v1 {:tests [{:id :foo}]}")
         (first (w/reload-config {:kaocha/cli-options {:config-file (str tmp-file)}} []))))))

(deftest ^{:min-java-version "1.11"} watch-test
  (let [{:keys [config-file test-dir] :as m} (integration/test-dir-setup {})
        config (-> (config/load-config config-file)
                   (assoc-in [:kaocha/cli-options :config-file] (str config-file))
                   (assoc-in [:kaocha/tests 0 :kaocha/source-paths] [])
                   (assoc-in [:kaocha/tests 0 :kaocha/test-paths] [(str test-dir)]))
        prefix (str (gensym "foo"))
        finish? (atom false)
        q       (w/make-queue)
        out-str (promise)
        test-file-path (str "test/" prefix "/bar_test.clj")]
    (integration/spit-file m "tests.edn" (prn-str config))
    (integration/spit-file m test-file-path (str "(ns " prefix ".bar-test (:require [clojure.test :refer :all])) (deftest xxx-test (is (= :xxx :yyy)))"))

    (future (deliver out-str (util/with-test-out-str
                               (t/with-test-out
                                 (util/with-test-ctx
                                   (w/run* config finish? q))))))

    (Thread/sleep 100)
    (integration/spit-file m test-file-path (str "(ns " prefix ".bar-test (:require [clojure.test :refer :all])) (deftest xxx-test (is (= :xxx :zzz)))"))
    (w/qput q (.resolve (:dir m) test-file-path))
    (Thread/sleep 500)
    (reset! finish? true)
    (w/qput q :finish)
    (Thread/sleep 100)

    (let [split-str (str/split-lines @out-str)]
      (is (match?
            (matchers/embeds
              ["[(F)]"
               ""
               (str/replace "FAIL in foo.bar-test/xxx-test (bar_test.clj:1)" "foo" prefix)
               "Expected:"
               "  :xxx"
               "Actual:"
               "  -:xxx +:yyy"
               "1 tests, 1 assertions, 1 failures."])
            split-str))

      (is (match?
            (matchers/embeds 
              [(format "bin/kaocha --config-file %s --focus '%s.bar-test/xxx-test'" (str config-file) prefix)
               ""
               (str/replace "[watch] Reloading #{foo.bar-test}" "foo" prefix)
               (str/replace "[watch] Re-running failed tests #{:foo.bar-test/xxx-test}" "foo" prefix)
               "[(F)]"
               ""
               (str/replace "FAIL in foo.bar-test/xxx-test (bar_test.clj:1)" "foo" prefix)
               "Expected:"
               "  :xxx"
               "Actual:"
               "  -:xxx +:zzz" ])
            split-str)))))

(deftest ignore-files-merged
  (let [{:keys [_config-file test-dir] :as m} (integration/test-dir-setup {})]
    (integration/spit-file  m (str test-dir "/.gitignore") "one" )
    (integration/spit-file  m (str test-dir "/.ignore") "two" )
    (is (=  #{"one" "two"}  (set (w/merge-ignore-files (str test-dir)))))))

(deftest child-files-merged
  (let [{:keys [_config-file test-dir] :as m} (integration/test-dir-setup {})]
    (integration/spit-file  m (str test-dir "/.gitignore") "one" )
    (integration/spit-dir m (str test-dir "/src/") )
    (integration/spit-file  m (str test-dir "/src/.gitignore") "two" )
    (is (=  #{"one" "two"}   (set (w/merge-ignore-files (str test-dir)))))))

(deftest watch-set-dynamic-vars-test
  ; sanity test for #133. Should succeed when this file
  ; is checked via ./bin/kaocha with --watch mode
  (is (do (set! *warn-on-reflection* false)
          true))
  (let [{:keys [config-file test-dir] :as m} (integration/test-dir-setup {})
        config (-> (config/load-config config-file)
                   (assoc-in [:kaocha/cli-options :config-file] (str config-file))
                   (assoc-in [:kaocha/tests 0 :kaocha/source-paths] [])
                   (assoc-in [:kaocha/tests 0 :kaocha/test-paths] [(str test-dir)]))
        prefix (str (gensym "foo"))
        finish? (atom false)
        exit-code (promise)
        out-str (promise)]
    (integration/spit-file m "tests.edn" (prn-str config))
    (integration/spit-file m (str "test/" prefix "/bar_test.clj") (str "(ns " prefix ".bar-test (:require [clojure.test :refer :all])) (deftest xxx-test (is (do (set! *warn-on-reflection* true) true)))"))

    (future (deliver out-str (util/with-test-out-str
                               (t/with-test-out
                                 (util/with-test-ctx
                                   (let [[ec finish!] (w/run config)]
                                     (loop []
                                       (Thread/sleep 100)
                                       (if @finish?
                                         (finish!)
                                         (recur)))
                                     (deliver exit-code @ec)))))))

    (Thread/sleep 500)
    (reset! finish? true)

    (is (= "[(.)]\n1 tests, 1 assertions, 0 failures.\n\n[watch] watching stopped.\n"
           @out-str))
    (is (= 0 @exit-code))))

(deftest reload-test
  (testing "reloading a configuration file produces valid config"
    (let [orig-config (config/load-config-for-cli-and-validate "test/unit/kaocha/config/loaded-test.edn" {})
          [reloaded-config _] (w/reload-config orig-config nil)]
      (is (spec/valid? :kaocha/config reloaded-config)
          (spec/explain :kaocha/config reloaded-config))))
  (testing "reloading a configuration file produces the same config"
    (let [orig-config (config/load-config-for-cli-and-validate "test/unit/kaocha/config/loaded-test.edn" {})
          [reloaded-config _] (w/reload-config orig-config nil)]
      (is (= orig-config reloaded-config))))
  (testing "reloading a configuration file produces the same config when using a profile"
    (let [orig-config (config/load-config-for-cli-and-validate "test/unit/kaocha/config/loaded-test-profile.edn" {:profile :test})
          [reloaded-config _] (w/reload-config orig-config nil)]
      (is (= orig-config reloaded-config)))))
     
;;TODO move to cucumber
(deftest ^{:min-java-version "1.11"} watch-load-error-test
  (let [{:keys [config-file test-dir] :as m} (integration/test-dir-setup {})
        config (-> (config/load-config config-file)
                   (assoc-in [:kaocha/cli-options :config-file] (str config-file))
                   (update :kaocha/tests (fn [suites]
                                           (let [base-suite (assoc (first suites)
                                                                  :kaocha/source-paths []
                                                                  :kaocha/test-paths [(str test-dir)])]
                                             [(assoc base-suite :kaocha.testable/id :first-suite)
                                              (assoc base-suite :kaocha.testable/id :second-suite)]))))
        _ (spit (str config-file) (pr-str config))
        spit-good-test #(integration/spit-file m "test/bar_test.clj" (str "(ns bar-test (:require [clojure.test :refer :all])) (deftest good-test (is true))"))
        spit-bad-test #(integration/spit-file m "test/bar_test.clj" (str "(ns bar-test) (throw (Exception. \"Intentional compilation error\"))"))

        dbg (bound-fn* prn)
        _ (dbg "before")
        _ (spit-good-test)
        exit (integration/interactive-process m [":second-suite" "--watch"]
               (try
                 (dbg "first lines")
                 (integration/expect-lines
                   ["[(.)]"
                    "1 tests, 1 assertions, 0 failures."
                    ""])
                 (spit-bad-test)
                 (dbg "after bad test")
                 (integration/expect-regexes
                   [#"\[watch\] Reloading #\{bar-test\}"
                    #"\[E\]"
                    #""
                    #"ERROR in second-suite \(.*?/bar_test.clj:35\)"
                    #"Failed reloading bar-test:"])
                 (dbg "compiler exception")
                 (integration/next-line-matches
                   ;; Errors usually look something along the lines of:
                   ;; {"Exception: clojure.lang.Compiler$CompilerException: Syntax error macroexpanding at (test/bar_test.clj:1:15)."
                   ;;   "Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at (test/bar_test.clj:1:15)."
                   ;;   "Exception: clojure.lang.Compiler$CompilerException: java.lang.Exception: Intentional compilation error, compiling:(test/bar_test.clj:1:15)"}
                   #(re-matches #"Exception: clojure\.lang\.Compiler\$CompilerException: (Syntax error macroexpanding at |Syntax error compiling at |java.lang.Exception: Intentional compilation error, compiling:)\(.*bar_test.clj:1:15\)\.?" %))
                 (dbg "big trace")
                 (integration/read-until #{"1 tests, 1 assertions, 1 errors, 0 failures."})
                 (integration/expect-lines
                   [""
                    "[watch] Error reloading, all tests skipped."])
                 ;; fix the compilation error...
                 (spit-good-test)
                 (dbg "after good-test")
                 (integration/expect-lines
                   ["[watch] Reloading #{bar-test}"
                    "[(.)]"
                    "1 tests, 1 assertions, 0 failures."])
                 (finally
                   ;; FIXME unsure how to exit process via (println) ... eg., how to enter ^C ?
                   ;; Idea from Alys: What you have seems to work, but maybe if *process* were a map in an atom,
                   ;; you could have a :process key containing an actual Process and :continue key telling it whether to keep going?
                   ;; I think this might make *interactive-process* a little tidier, too.
                   (.destroy integration/*process*))))]
    (is (= 143 exit))))
