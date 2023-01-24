(ns kaocha.platform
  "Utility functions for specific platforms.")

(defn on-windows?
  "Return whether we're running on Windows."
  []
  (re-find #"Windows" (System/getProperty "os.name")))

(defn on-posix?
  "Return whether we're running on a Posix system."
  []
  (re-find #"(?ix)(MacOS|Linux)" (System/getProperty "os.name")))
