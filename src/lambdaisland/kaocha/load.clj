(ns lambdaisland.kaocha.load
  (:require [lambdaisland.kaocha.classpath :as cp]
            [clojure.java.io :as io]
            [clojure.tools.namespace.find :as ctn.find]))

(defn- ns-match? [patterns ns-sym]
  (some #(re-find % (name ns-sym)) patterns))

(defn- find-test-nss [test-paths ns-patterns]
  (sequence (comp
             (map io/file)
             (map ctn.find/find-namespaces-in-dir)
             cat
             (filter (partial ns-match? ns-patterns)))
            test-paths))

(defn load-tests [{:keys [test-paths ns-patterns]}]
  (cp/maybe-add-dynamic-classloader)
  (run! cp/add-classpath test-paths)
  (let [test-nss (find-test-nss test-paths ns-patterns)]
    (run! require test-nss)
    test-nss))
