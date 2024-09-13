(ns kaocha.plugin.capture-output
  (:require
    [clojure.java.io :as io]
    [kaocha.hierarchy :as hierarchy]
            [kaocha.plugin :as plugin :refer [defplugin]])
  (:import (java.io ByteArrayOutputStream
                    FileOutputStream ;; false positive
                    OutputStream
                    PrintStream
                    PrintWriter)))

;; Many props to eftest for much of this code

(def ^:dynamic *test-buffer* nil)

(def ^:dynamic *previous-writers* nil)

(def active-buffers (atom #{}))

(defn make-buffer []
  (ByteArrayOutputStream.))

(defn read-buffer [^ByteArrayOutputStream buffer]
  (when buffer
    (-> buffer (.toByteArray) (String.))))

(defmacro with-test-buffer [buffer & body]
  `(try
     (swap! active-buffers conj ~buffer)
     (binding [*test-buffer* ~buffer]
       ~@body)
     (finally
       (swap! active-buffers disj ~buffer))))

(defn- doto-capture-buffer [f]
  (if *test-buffer*
    (f *test-buffer*)
    (run! f @active-buffers)))

(defn create-proxy-output-stream ^OutputStream []
  (proxy [OutputStream] []
    (write
      ([data]
       (if (instance? Integer data)
         (doto-capture-buffer #(.write ^OutputStream % ^int data))
         (doto-capture-buffer #(.write ^OutputStream % ^bytes data 0 (alength ^bytes data)))))
      ([data off len]
       (doto-capture-buffer #(.write ^OutputStream % data off len))))))

(defn init-capture []
  (let [old-out             System/out
        old-err             System/err
        proxy-output-stream (create-proxy-output-stream)
        new-stream          (PrintStream. proxy-output-stream)
        new-writer          (PrintWriter. proxy-output-stream)]
    (System/setOut new-stream)
    (System/setErr new-stream)
    {:captured-writer new-writer
     :previous-writers {:out *out*, :err *err*}
     :old-system-out  old-out
     :old-system-err  old-err}))

(defn restore-capture [{:keys [old-system-out old-system-err]}]
  (System/setOut old-system-out)
  (System/setErr old-system-err))

(defmacro with-capture [& body]
  `(let [context# (init-capture)
         writer#  (:captured-writer context#)]
     (try
       (binding [*out* writer#
                 *err* writer#
                 *previous-writers* (:previous-writers context#)]
         (with-redefs [*out* writer#, *err* writer#]
           ~@body))
       (finally
         (restore-capture context#)))))

(defplugin kaocha.plugin/capture-output
  (cli-options [opts]
    (conj opts [nil "--[no-]capture-output" "Capture output during tests."]))

  (config [config]
    (let [cli-flag (get-in config [:kaocha/cli-options :capture-output])]
      (assoc config ::capture-output?
             (if (some? cli-flag)
               cli-flag
               (::capture-output? config true)))))

  (wrap-run [run test-plan]
    (if (::capture-output? test-plan)
      (fn [& args]
        (with-capture (apply run args)))
      run))

  (pre-test [testable test-plan]
    (if (::capture-output? test-plan)
      (let [buffer (make-buffer)]
        (cond-> testable
          (hierarchy/leaf? testable)
          (-> (assoc ::buffer buffer)
              (update :kaocha.testable/wrap conj (fn [t] #(with-test-buffer buffer (t)))))))
      testable))

  (post-test [testable test-plan]
    (if (and (::capture-output? test-plan) (::buffer testable))
      (-> testable
          (assoc ::output (read-buffer (::buffer testable)))
          (dissoc ::buffer))
      testable)))

#?(:bb nil
   :clj
   (defmacro bypass
     "Bypass output-capturing within this code block, so any print statements go to
  the process out/err streams without being captured."
     [& body]
     `(let [{out# :out, err# :err} (or *previous-writers* {:out *out*, :err *err*})]
        (binding [*out* out#
                  *err* err#]
          ~@body))))
