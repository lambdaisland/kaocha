(ns kaocha.plugin.notifier
  "Plugin that shows the test result (fail/pass) and test counts in a
  desktop (system tray) notification.

  Will try multiple approaches to find a system-appropriate way to show a
  desktop notifaction.

  - notify-send
  - terminal-notifier
  - java.awt.SystemTray
  "
  {:authors ["Ryan McCuaig (@rgm)"
             "Arne Brasseur (@plexus)"]}
  (:require [clojure.java.shell :refer [sh]]
            [kaocha.output :as output]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.result :as result]
            [kaocha.platform :as platform]
            [kaocha.shellwords :refer [shellwords]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [throw+]])
  (:import [java.nio.file Files]
           [java.io IOException]
           [java.awt SystemTray TrayIcon TrayIcon$MessageType Toolkit]))

;; special thanks for terminal-notify stuff to
;; https://github.com/glittershark/midje-notifier/blob/master/src/midje/notifier.clj

(defn exists? [program]
  (let [cmd (if (platform/on-windows?)
              "where.exe" "which" )]
    (try
      (= 0 (:exit (sh cmd program)))
      (catch IOException e  ;in the unlikely event neither where.exe nor which is available
        (output/warn (format "Unable to determine whether '%s' exists. Notifications may not work." program)) ))))

(defn detect-command []
  (cond
    (exists? "notify-send")
    "notify-send -a Kaocha %{title} %{message} -i %{icon} -u %{urgency} -t %{timeout}"

    (exists? "terminal-notifier")
    "terminal-notifier -message %{message} -title %{title} -appIcon %{icon}"))

(defn message [result]
  (let [{::result/keys [count pass fail error pending]} (result/testable-totals result)]
    (str count " tests"
         (when (pos-int? error)
           (str ", " error " errors"))
         (when (pos-int? pending)
           (str ", "pending " pending"))
         (when (pos-int? fail)
           (str ", " fail " failures"))
         ".")))

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

(def tray-icon
  "Creates a system tray icon."
  (memoize
   (fn [icon-path]
     (let [^java.awt.Toolkit toolkit (Toolkit/getDefaultToolkit)
           tray-icon (-> toolkit
                         (.getImage ^String icon-path)
                         (TrayIcon. "Kaocha Notification"))]
       (doto (SystemTray/getSystemTray)
         (.add tray-icon))
       tray-icon))))

(defn send-tray-notification
  "Use Java's built-in functionality to display a notification.

  Not preferred over shelling out because the built-in notification sometimes
  looks out of place, and isn't consistently available on Linux."
  [result]
  (try
    (let [icon (tray-icon "kaocha/clojure_logo.png")
          urgency (if (result/failed? result) TrayIcon$MessageType/ERROR TrayIcon$MessageType/INFO) ]
      (.displayMessage icon (title result) (message result) urgency))
    (catch java.awt.HeadlessException _e
      (output/warn (str "Notification not shown because system is headless."
                        "\nConsider disabling the notifier plugin when using in this context.")))
    (catch java.lang.UnsupportedOperationException _e
      (output/warn (str "Notification not shown because system does not support it."
                        "\nConsider disabling the notifier plugin when using in this context or installing"
                        "\neither notify-send (Linux) or terminal-notifier (macOS).")))))

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
        timeout (::timeout result)
        message (message result)
        title   (title result)
        icon    (icon-path)
        failed? (result/failed? result)
        urgency (if failed? "critical" "normal")
        expanded-command (expand-command command {:message message
                                                  :title title
                                                  :icon icon
                                                  :urgency urgency
                                                  :count count
                                                  :pass pass
                                                  :fail fail
                                                  :error error
                                                  :pending pending
                                                  :failed? failed?
                                                  :timeout timeout})
        {:keys [exit err] :as  command-result} (apply sh expanded-command)]
    (when (not (zero? exit))
      (output/warn (format
                    (str
                     "Notification command exited with status code: %s"
                     "Error message (stderr): \"%s\""
                     "Command: \"%s\""
                     "Check your configuration for :kaocha.plugin.notifier/command and :kaocha.plugin.notifier/notification-timeout.")
                    exit
                    err
                    (apply str (interpose \space expanded-command)))))
    command-result))

(defplugin kaocha.plugin/notifier
  "Run a shell command after each completed test run, by default will run a
  command that displays a desktop notification showing the test results, so a
  `kaocha --watch` terminal process can be hidden and generally ignored until it
  goes red.

  Requires https://github.com/julienXX/terminal-notifier on mac or `libnotify` /
  `notify-send` on linux."
  (cli-options [opts]
    (conj opts
          [nil "--[no-]notifications" "Enable/disable the notifier plugin, providing desktop notifications. Defaults to true."]
          [nil "--notification-timeout TIMEOUT" "Set a timeout value for desktop notifications through the notifier plugin."]))

  (config [config]
    (let [cli-options (:kaocha/cli-options config)
          cli-flag (:notifications cli-options)
          timeout (:notification-timeout cli-options (::timeout config -1))]
      (assoc config
             ::command (::command config (detect-command))
             ::timeout timeout
             ::notifications?
             (if (some? cli-flag)
               cli-flag
               (::notifications? config true)))))

  (post-run [result]
    (when (::notifications? result)
      (if-let [command (::command result)]
        (run-command command result)
        (send-tray-notification result)))
    result))
