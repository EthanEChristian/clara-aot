(ns clara-aot.core
  (:require [clara.rules :as r]
            [clara.rules.compiler :as com]
            [clojure.string :as str]
            [clara.rules.accumulators :as acc]
            [clara-aot.records :as recs]
            [clara-aot.commands.split-string])
  (:import [clara_aot.records
            CommandRequest
            Execution
            CommandError HelpMessage])
  (:gen-class))

(r/defrule no-execution-found
  [?r <- CommandRequest]
  [:not [Execution]]
  =>
  (r/insert! (recs/->CommandError (str "No Command found for: " (:command-name ?r)))))

(r/defrule too-many-executions
  [?r <- CommandRequest]
  [?num-execs <- (acc/count) :from [Execution]]
  [:test (>= ?num-execs 2)]
  =>
  (r/insert! (recs/->CommandError (str "Number of Executions exceed one for command: " (:command-name ?r)))))

(r/defquery error-query
  []
  [?err <- CommandError])

(r/defquery exec-query
  []
  [?exec <- Execution])

(r/defquery help-query
  []
  [?msg <- HelpMessage])

(def available-commands
  ['clara-aot.commands.split-string])

(def session
  (com/mk-session (conj available-commands (ns-name *ns*))))

(defn parse-args
  [args]
  (when (>= (count args) 1)
    (loop [facts [(recs/->CommandRequest (first args))]
           remaining-args (rest args)]
      (if (seq remaining-args)
        (let [first-remaining (first remaining-args)
              rest-remaining (rest remaining-args)]
          (if (re-find #"^[-]{1,2}[a-zA-Z0-9]+" first-remaining)
            (let [arg-name (str/replace first-remaining #"^[-]{1,2}" "")
                  first-of-rest (first rest-remaining)
                  is-flag? (and first-of-rest (re-find #"^[-]{1,2}[a-zA-Z0-9]+" first-of-rest))]
              (if is-flag?
                (recur (conj facts (recs/->Flag arg-name)) rest-remaining)
                (recur (conj facts (recs/->Option arg-name first-of-rest)) (rest rest-remaining))))
            (throw (ex-info "Illegal argument" {:arg first-remaining}))))
        facts))))

(defn -main
  [& args]
  (let [parsed-args (parse-args args)
        fired-session (-> session
                          (r/insert-all parsed-args)
                          (r/fire-rules))
        errors (r/query fired-session error-query)
        execs (r/query fired-session exec-query)
        help-msgs (r/query fired-session help-query)]
    (if (seq errors)
      (do
        (when (seq help-msgs)
          (doseq [msg help-msgs]
            (println (:?msg msg))))
        (throw (ex-info "Failed to retrieve command"
                        {:errors (map :?err errors)})))
      (do (println (str "Running Command: " (first args)))
          ((-> execs first :?exec :exec))))))
