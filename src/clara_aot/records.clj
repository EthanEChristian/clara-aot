(ns clara-aot.records)

;; Records to identify and construct the thunk
(defrecord CommandRequest [command-name])
(defrecord Option [option-name value])
(defrecord Flag [flag-name])
(defrecord HelpMessage [msg])

;; The requested execution, expected to contain a thunk
(defrecord Execution [exec])

(defrecord CommandError [msg])