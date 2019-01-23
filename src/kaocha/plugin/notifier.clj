(ns kaocha.plugin.notifier
  {:authors ["Ryan McCuaig (@rgm)"
             "Arne Brasseur (@plexus)"]}
  (:require [clojure.java.shell :refer [sh]]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.result :as result]
            [kaocha.shellwords :refer [shellwords]]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.nio.file Files]))

;; special thanks for terminal-notify stuff to
;; https://github.com/glittershark/midje-notifier/blob/master/src/midje/notifier.clj

(defn exists? [program]
  (= 0 (:exit (sh "which" program))))

(defn detect-command []
  (cond
    (exists? "notify-send")
    "notify-send -a Kaocha %{title} %{message} -i %{icon} -u %{urgency}"

    (exists? "terminal-notifier")
    "terminal-notifier -message %{message} -title %{title} -appIcon %{icon}"))

(defn message [result]
  (let [{::result/keys [count pass fail error pending]} (result/testable-totals result)]
    (str count " tests, "
         (+ pass fail error) " assertions, "
         (when (pos-int? error)
           (str error " errors, "))
         (when (pos-int? pending)
           (str pending " pending, "))
         fail " failures.")))

(defn title [result]
  (if (result/failed? result)
    "⛔️ Failing"
    "✅ Passing"))

(def icon-path
  "Return a local path to the Clojure icon.

  If Kaocha is running from a jar, then extract the icon to a temp file first,
  so external programs have access to it. "
  (memoize
   (fn []
     (let [resource (io/resource "kaocha/clojure_logo.png")]
       (if (= "file" (.getProtocol resource))
         (str (io/file resource))
         (let [file (java.io.File/createTempFile "clojure_logo" ".png")]
           (io/copy (io/make-input-stream resource {}) file)
           (str file)))))))

(defn expand-command
  "Takes a command string including replacement patterns, and a map of
  replacements, and returns a vector of command + arguments.

  Replacement patterns are of the form `%{xxx}`"
  [command replacements]
  (map (fn [cmd]
         (reduce
          (fn [cmd [k v]]
            (str/replace cmd (str "%{" (name k) "}") (str v)))
          cmd
          replacements))
       (shellwords command)))

(defn run-command
  "Run the given command string, replacing patterns with values based on the given
  test result."
  [command result]
  (let [{::result/keys [count pass fail error pending]} (result/testable-totals result)
        message (message result)
        title   (title result)
        icon    (icon-path)
        failed? (result/failed? result)
        urgency (if failed? "critical" "normal")]
    (apply sh (expand-command command {:message message
                                       :title title
                                       :icon icon
                                       :urgency urgency
                                       :count count
                                       :pass pass
                                       :fail fail
                                       :error error
                                       :pending pending
                                       :failed? failed?}))))

(defplugin kaocha.plugin/notifier
  "Run a shell command after each completed test run, by default will run a
  command that displays a desktop notification showing the test results, so a
  `kaocha --watch` terminal process can be hidden and generally ignored until it
  goes red.

  Requires https://github.com/julienXX/terminal-notifier on mac or `libnotify` /
  `notify-send` on linux."
  (cli-options [opts]
    (conj opts [nil "--[no-]notifications" "Enable/disable the notifier plugin, providing desktop notifications. Defaults to true."]))

  (config [config]
    (let [cli-flag (get-in config [:kaocha/cli-options :notifications])]
      (assoc config
             ::command (::command config (detect-command))
             ::notifications?
             (if (some? cli-flag)
               cli-flag
               (::notifications? config true)))))

  (post-run [result]
    (if-let [command (and (::notifications? result) (::command result))]
      (run-command command result))
    result))
