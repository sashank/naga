(ns ^{:doc "Defines rule structures and constructors to keep them consistent"
      :author "Paula Gearon"}
    naga.rules
    (:require [schema.core :as s]
              [naga.schema.structs :as st :refer [EPVPattern RulePatternPair Body Axiom Program]]
              [naga.util :as u])
    (:import [clojure.lang Symbol]
             [naga.schema.structs Rule]))

(defn- gen-rule-name [] (name (gensym "rule-")))

(s/defn rule :- Rule
  "Creates a new rule"
  ([head body] (rule head body (gen-rule-name)))
  ([head body name]
   (assert (and (sequential? body) (or (empty? body) (every? sequential? body)))
           "Body must be a sequence of constraints")
   (assert (sequential? head) "Head must be a constraint")
   (st/new-rule head body name)))

(s/defn named-rule :- Rule
  "Creates a rule the same as an existing rule, with a different name."
  [name :- Rule
   {:keys [head body salience downstream]} :- s/Str]
  (st/new-rule head body name downstream salience))

(defn- de-ns
  "Remove namespaces from symbols in a pattern"
  [pattern]
  (letfn [(clean [e] (if (symbol? e) (symbol (name e)) e))]
    (apply vector (map clean pattern))))

(defmacro r
  "Create a rule, with an optional name.
   Var symbols need not be quoted."
  [& [f :as rs]]
  (let [[nm# rs#] (if (string? f) [f (rest rs)] [(gen-rule-name) rs])
        not-sep# (partial not= :-)
        head# (de-ns (first (take-while not-sep# rs#)))
        body# (map de-ns (rest (drop-while not-sep# rs#)))]
    `(rule (quote ~head#) (quote ~body#) ~nm#)))

(defn check-symbol
  "Asserts that symbols are unbound variables for a query. Return true if it passes."
  [sym]
  (let [n (name sym)]
    (assert (= \? (first n)) (str "Unknown symbol type in rule: " n)) )
  true)

(defprotocol Matching
  (compatible [x y] "Returns true if both elements are compatible"))

(extend-protocol Matching
  Symbol
  (compatible [x _]
    (check-symbol x))
  Object
  (compatible [x y]
    (or (= x y) (and (symbol? y) (check-symbol y)))))

(s/defn match? :- s/Bool
  "Does pattern a match pattern b?"
  [a :- EPVPattern, b :- EPVPattern]
  (every? identity (map compatible a b)))

(s/defn find-matches :- [RulePatternPair]
  "returns a sequence of name/pattern pairs where a matches a pattern in a named rule"
  [a :- EPVPattern,
   [nm sb] :- [(s/one s/Str "rule-name") (s/one Body "body")]]
  (letfn [(matches? [b]
            "Return a name/pattern if a matches the pattern in b"
            (if (match? a b) [nm b]))]
    (keep matches? sb)))

(defn dbg [x] (println x) x)

(s/defn create-program :- Program
  "Converts a sequence of rules into a program.
   A program consists of a map of rule names to rules, where the rules have dependencies."
  [rules :- [Rule]
   axioms :- [Axiom]]
  (let [name-bodies (u/mapmap :name :body rules)
        triggers (fn [head] (mapcat (partial find-matches head) name-bodies))
        deps (fn [{:keys [head body name]}]
               (st/new-rule head body name (triggers head)))]
    {:rules (u/mapmap :name identity (map deps rules))
     :axioms axioms}))


