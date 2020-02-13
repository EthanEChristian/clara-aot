(ns clara-aot.commands.split-string
  (:require [clara.rules :as r]
            [clara-aot.records :as recs]
            [clojure.string :as str])
  (:import [clara_aot.records
            CommandRequest
            Option]))

(def cmd-name "split-string")

(def string-to-split-option "str-2-split")
(def delimiter-option "delim")

(r/defrule is-our-command?
  [CommandRequest (= command-name cmd-name)]
  =>
  ;; simple flag to allow our rules to recognise that we are running this command
  ;; note: that this might be seen as abusing the fact-type-fn of clara...
  (r/insert! ^{:type ::is-our-command} {}))

(r/defrule missing-string-option
  [::is-our-command]
  [:not [Option (= option-name string-to-split-option)]]
  =>
  (r/insert! (recs/->CommandError (str "Missing option for: " string-to-split-option)))
  (r/insert! ^{:type ::missing-option} {}))

(r/defrule missing-delim-option
  [::is-our-command]
  [:not [Option (= option-name delimiter-option)]]
  =>
  (r/insert! (recs/->CommandError (str "Missing option for: " delimiter-option)))
  (r/insert! ^{:type ::missing-option} {}))

(r/defrule construct-execution
  [::is-our-command]
  [:not [::missing-option]]
  [Option (= option-name string-to-split-option)
   (= ?str-to-split value)]
  [Option (= option-name delimiter-option)
   (= ?delim value)]
  =>
  (r/insert! (recs/->Execution
               #(let [split-str (str/split ?str-to-split (re-pattern ?delim))]
                  (println (str "Original String: " ?str-to-split))
                  (println (str "Split by: " ?delim))
                  (println "Returns: ")
                  (doseq [x split-str]
                    (println (str "    " x)))))))