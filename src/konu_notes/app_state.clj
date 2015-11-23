(ns konu-notes.app-state
  )


;; Global storage for store generated tokens.
(def tokens (atom {}))

(defn get-tokens-state []
  @tokens)

;; TODO refactor to use mongo instead with TTL
(defn add-token [token username]
  (swap! tokens assoc (keyword token) (keyword username)))

;; TODO refactor tokens to use api instead of direct access.
