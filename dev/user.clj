(ns user
  (:require [clojure.tools.deps.alpha :as tools.deps]
            [clojure.tools.deps.alpha.reader :as tools.deps.reader]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn read-deps-edn
  "Read deps.edn and canonicalize."
  []
  (tools.deps.reader/slurp-deps "deps.edn"))

(defn- lib-map [aliases]
  (let [deps (read-deps-edn)
        extra-deps (tools.deps/combine-aliases deps aliases)]
    (tools.deps/resolve-deps deps extra-deps)))

(defn print-deps
  "Print a tree of dependencies to stdout."
  ([]
   (print-deps []))
  ([aliases]
   (tools.deps/print-tree (lib-map aliases))))
