(ns kaocha.platform.systray
  (:import (java.nio.file Files)
           (java.io IOException)
           #_       (java.awt SystemTray
                              TrayIcon
                              TrayIcon$MessageType
                              Toolkit)))

(def tray-icon
  "Creates a system tray icon."
  (memoize
   (fn [icon-path]
     #_(let [^java.awt.Toolkit toolkit (Toolkit/getDefaultToolkit)
             tray-icon (-> toolkit
                           (.getImage ^String icon-path)
                           (TrayIcon. "Kaocha Notification"))]
         (doto (SystemTray/getSystemTray)
           (.add tray-icon))
         tray-icon))))

(defn display-message
  "Use Java's built-in functionality to display a notification.

  Not preferred over shelling out because the built-in notification sometimes
  looks out of place, and isn't consistently available on Linux."
  [title message urgency]
  #_(try
      (.displayMessage (tray-icon "kaocha/clojure_logo.png")
                       title
                       message
                       (get {:error TrayIcon$MessageType/ERROR
                             :info TrayIcon$MessageType/INFO}
                            urgency))
      :ok
      (catch java.awt.HeadlessException _e
        :headless)
      (catch java.lang.UnsupportedOperationException _e
        :unsupported)))
