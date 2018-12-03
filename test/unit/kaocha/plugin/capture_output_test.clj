(ns kaocha.plugin.capture-output-test
  (:require [clojure.test :refer :all]
            [clojure.tools.cli :as cli]
            [kaocha.plugin :as plugin]
            [kaocha.plugin.capture-output :as c])
  (:import java.io.ByteArrayOutputStream))

(deftest read-buffer-test
  (is (= "" (c/read-buffer (c/make-buffer))))

  (let [buffer (c/make-buffer)]
    (.write buffer (.getBytes "kingfisher"))
    (is (= "kingfisher" (c/read-buffer buffer)))))

(deftest with-test-buffer-test
  (let [buffer (c/make-buffer)]
    (with-redefs [c/active-buffers (atom #{})]
      (c/with-test-buffer buffer
        (is (= #{buffer} @c/active-buffers))
        (is (= buffer c/*test-buffer*)))
      (is (= #{} @c/active-buffers)))))

(deftest init-capture-test
  (let [capture (c/init-capture)]
    (try
      (let [buffer (c/make-buffer)]
        (binding [*out* (:captured-writer capture)]
          (c/with-test-buffer buffer
            (println "kingfisher")))
        (is (= "kingfisher\n" (c/read-buffer buffer))))
      (finally
        (c/restore-capture capture)))))

(deftest create-proxy-output-stream-test
  (let [buffer (c/make-buffer)
        stream (c/create-proxy-output-stream)]

    (binding [c/*test-buffer* buffer]
      (.write stream (.getBytes "ABC"))
      (.write stream 59)
      (.write stream (.getBytes "--XYZ--") 2 3))

    (is (= "ABC;XYZ" (c/read-buffer buffer)))))

(deftest with-capture-test
  (let [buffer (c/make-buffer)]
    (binding [c/*test-buffer* buffer]
      (c/with-capture buffer
        (println "kingfisher")
        (binding [*out* *err*]
          (println "feicui"))
        (.write System/out 60)
        (.write System/out (byte-array [80 81 82]))))
    (is (= "kingfisher\nfeicui\n<PQR" (c/read-buffer buffer))))

  (let [buffer1 (c/make-buffer)
        buffer2 (c/make-buffer)
        buffers #{buffer1 buffer2}]
    (binding [c/*test-buffer* nil]
      (with-redefs [c/active-buffers (atom buffers)]
        (c/with-capture
          (println "kingfisher")
          (binding [*out* *err*]
            (println "feicui"))
          (.write System/out 60)
          (.write System/out (byte-array [80 81 82])))))
    (is (= ["kingfisher\nfeicui\n<PQR"
            "kingfisher\nfeicui\n<PQR"]
           (map c/read-buffer buffers)))))

(defn run-plugin-hook [hook init & extra-args]
  (let [chain (plugin/load-all [:kaocha.plugin/capture-output])]
    (apply plugin/run-hook* chain hook init extra-args)))

(deftest cli-options-test
  (let [cli-opts (run-plugin-hook :kaocha.hooks/cli-options [])]

    (is (= [[nil "--[no-]capture-output" "Capture output during tests."]]
           cli-opts))

    (is (= {:capture-output true}
           (:options (cli/parse-opts ["--capture-output"] cli-opts))))

    (is (= {:capture-output false}
           (:options (cli/parse-opts ["--no-capture-output"] cli-opts))))

    (is (= "      --[no-]capture-output  Capture output during tests."
           (:summary (cli/parse-opts [] cli-opts))))))

(deftest config-test
  (is (= {:kaocha.plugin.capture-output/capture-output? true}
         (run-plugin-hook :kaocha.hooks/config {})))

  (is (= {:kaocha.plugin.capture-output/capture-output? false}
         (run-plugin-hook :kaocha.hooks/config {::c/capture-output? false})))

  (is (match? {:kaocha.plugin.capture-output/capture-output? true}
              (run-plugin-hook :kaocha.hooks/config {:kaocha/cli-options {:capture-output true}})))

  (is (match? {:kaocha.plugin.capture-output/capture-output? false}
              (run-plugin-hook :kaocha.hooks/config {:kaocha/cli-options {:capture-output false}}))))

(deftest wrap-run-test
  (let [buffer (c/make-buffer)
        run    (run-plugin-hook :kaocha.hooks/wrap-run
                                #(print "-")
                                {:kaocha.plugin.capture-output/capture-output? false})]
    (binding [c/*test-buffer* buffer]
      (run))

    (is (= "" (c/read-buffer buffer))))

  (let [buffer (c/make-buffer)
        run    (run-plugin-hook :kaocha.hooks/wrap-run
                                #(println "kingfisher")
                                {:kaocha.plugin.capture-output/capture-output? true})]
    (binding [c/*test-buffer* buffer]
      (run))

    (is (= "kingfisher\n" (c/read-buffer buffer)))))

(deftest pre-test-test
  (testing "output capturing is off"
    (let [test-plan (run-plugin-hook :kaocha.hooks/pre-test

                                     {:kaocha.testable/type :kaocha.type/var}
                                     {:kaocha.plugin.capture-output/capture-output? false})]
      (is (not (contains? test-plan ::c/buffer))
          (not (contains? test-plan :kaocha.testable/wrap)))))

  (testing "non-leaf test type"
    (let [test-plan (run-plugin-hook :kaocha.hooks/pre-test
                                     {:kaocha.testable/type :kaocha.type/ns}
                                     {:kaocha.plugin.capture-output/capture-output? true})]
      (is (not (contains? test-plan ::c/buffer))
          (not (contains? test-plan :kaocha.testable/wrap)))))

  (testing "output capturing on"
    (is (match? {::c/buffer            #(instance? ByteArrayOutputStream %)
                 :kaocha.testable/wrap [ifn?]}
                (run-plugin-hook :kaocha.hooks/pre-test
                                 {:kaocha.testable/type :kaocha.type/var}
                                 {:kaocha.plugin.capture-output/capture-output? true})))))

;; This test shows how wrap-run, pre-test, and post-test coordinate
(deftest pre-post-test
  (let [testable  {:kaocha.testable/type :kaocha.type/var}
        test-plan {:kaocha.plugin.capture-output/capture-output? true}

        ;; Run pre-test to create the buffer and associate it with the testable,
        ;; as well as adding a wrapping function that runs `with-test-buffer`.
        testable  (run-plugin-hook :kaocha.hooks/pre-test testable test-plan)

        ;; These are "middleware" functions for the innermost test
        ;; function (i.e. the raw test var). This wrapping is handled inside
        ;; test type implementations right before handing over control to user
        ;; testing code.
        wrap      (:kaocha.testable/wrap testable)

        ;; Apply all wrappers to get a "wrapped" runner
        run       (reduce #(%2 %1) #(println "emerald") wrap)

        ;; The wrap-run hook wraps around `run-testable`, so it sits "outside"
        ;; test type implementations.
        run       (run-plugin-hook :kaocha.hooks/wrap-run run test-plan)]
    (run)
    (let [testable (run-plugin-hook :kaocha.hooks/post-test testable test-plan)]
      (is (= "emerald\n" (::c/output testable))))))


(comment
  (do
    (require 'kaocha.repl)
    (kaocha.repl/run {:kaocha.plugin.capture-output/capture-output? false})))
