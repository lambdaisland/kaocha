(ns kaocha.plugin.notifier-test
  (:require [clojure.test :refer :all]
            [kaocha.plugin.notifier :as n]
            [clojure.string :as str]
            [kaocha.platform :as platform]
            [clojure.java.io :as io]))

(deftest exists?-test
  (let [cmd-to-check (if (platform/on-windows?) "cmd.exe" "ls")]
    (is (n/exists? cmd-to-check)))
  (is (not (n/exists? "nonsense123"))))

(deftest detect-command-test
  (with-redefs [n/exists? (constantly false)]
    (is (nil? (n/detect-command))))

  (with-redefs [n/exists? #{"notify-send"}]
    (is (str/starts-with? (n/detect-command) "notify-send")))

  (with-redefs [n/exists? #{"terminal-notifier"}]
    (is (str/starts-with? (n/detect-command) "terminal-notifier"))))

(deftest message-test
  (is (= "3 tests, 4 errors, 5 pending, 1 failures."
         (n/message
          {:kaocha.result/tests [{:kaocha.testable/id :foo
                                  :kaocha.testable/type :foo/bar
                                  :kaocha.testable/desc "xxx"
                                  :kaocha.result/count 3
                                  :kaocha.result/pass 2
                                  :kaocha.result/fail 1
                                  :kaocha.result/error 4
                                  :kaocha.result/pending 5}]}))))

(deftest title-test
  (is (= "✅ Passing"
         (n/title
          {:kaocha.result/tests [{:kaocha.testable/id :foo
                                  :kaocha.testable/type :foo/bar
                                  :kaocha.testable/desc "xxx"
                                  :kaocha.result/count 3
                                  :kaocha.result/pass 2
                                  :kaocha.result/fail 0
                                  :kaocha.result/error 0
                                  :kaocha.result/pending 5}]})))

  (is (= "⛔️ Failing"
         (n/title
          {:kaocha.result/tests [{:kaocha.testable/id :foo
                                  :kaocha.testable/type :foo/bar
                                  :kaocha.testable/desc "xxx"
                                  :kaocha.result/count 3
                                  :kaocha.result/pass 2
                                  :kaocha.result/fail 1
                                  :kaocha.result/error 4
                                  :kaocha.result/pending 5}]}))))

(deftest icon-path-test
  (is (= (slurp (io/resource "kaocha/clojure_logo.png"))
         (slurp (n/icon-path)))))

(deftest expand-command-test
  (is (= ["foo" "-x" "123" "-y" "it's all good"]
         (n/expand-command "foo -x %{bar} -y %{baz}"
                           {:bar 123
                            :baz "it's all good"}))))

(deftest run-command-test
  (is (= {:exit 0, :out "5\n", :err ""}
         (n/run-command "expr %{count} + %{pass}"
                        {:kaocha.result/tests [{:kaocha.testable/id :foo
                                                :kaocha.testable/type :foo/bar
                                                :kaocha.testable/desc "xxx"
                                                :kaocha.result/count 3
                                                :kaocha.result/pass 2
                                                :kaocha.result/fail 1
                                                :kaocha.result/error 4
                                                :kaocha.result/pending 5}]}))))

(deftest notifier-cli-options-hook-test
  (is (= [[nil "--[no-]notifications" "Enable/disable the notifier plugin, providing desktop notifications. Defaults to true."]]
         (n/notifier-cli-options-hook []))))

(deftest notifier-config-hook-test
  (is (match? {::n/command #"^notify-send"
               ::n/notifications? true}
              (with-redefs [n/exists? #{"notify-send"}]
                (n/notifier-config-hook {}))))

  (is (match? {::n/command "xyz"
               ::n/notifications? true}
              (n/notifier-config-hook {::n/command "xyz"})))

  (is (match? {::n/notifications? true}
              (n/notifier-config-hook {:kaocha/cli-options {:notifications true}})))

  (is (match? {::n/notifications? false}
              (n/notifier-config-hook {:kaocha/cli-options {:notifications false}}))))

(deftest notifier-post-run-hook-test
  (let [gen-file-name #(str (System/getProperty "java.io.tmpdir") (System/getProperty "file.separator") 
                            (gensym (str (namespace `_) "-" (rand-int 10000))))
        f1 (gen-file-name)
        f2 (gen-file-name)
        cmd (if (platform/on-windows?) "cmd.exe echo $null >> " "touch ")]

    (n/notifier-post-run-hook {::n/notifications? true
                               ::n/command (str cmd f1)})
    (is (.isFile (io/file f1)))

    (n/notifier-post-run-hook {::n/notifications? false
                               ::n/command (str cmd f2)})
    (is (not (.isFile (io/file f2))))))
