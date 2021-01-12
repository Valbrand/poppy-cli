(ns budget.parser.core
  (:require [budget.model.date-time :as date-time]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [instaparse.core :as insta]))

(defn- transformer-with-key
  [[k transform-fn]]
  [k (fn [& args] [k (apply transform-fn args)])])

(defn create-transform-map
  [transform-map]
  (into {} (map transformer-with-key) transform-map))

(defn rules->map
  [rule-name->key]
  (fn [& rules]
    (loop [result {}
           used-keys #{}
           [rule & rest] rules]
      (cond
        (nil? rule)
        result

        (contains? used-keys (first rule))
        (throw (ex-info (format "Ambiguous merge into map: more than one element with key '%s'" (first rule)) {}))

        :else
        (let [[k v] rule]
          (recur (assoc result (get rule-name->key k k) v)
                 (conj used-keys k)
                 rest))))))

(defn try-parse
  [expected-rules parser transform-params line]
  (try
    (let [parse-result (->> line parser (insta/transform transform-params))]
      (cond
        (insta/failure? parse-result)
        (throw (ex-info "An exception was thrown while trying to parse" {:line line :cause parse-result}))

        (not (contains? expected-rules (first parse-result)))
        (throw (ex-info (format "Unsupported entry header: '%s'" line) {:line line}))

        :else
        (second parse-result)))
    (catch Exception e
      (throw (ex-info (format "Exception caught during parse: %s" (ex-message e)) {:line line :cause e})))))

(defn validate-entry-spec
  [spec entry]
  (if (s/valid? spec entry)
    entry
    (throw (ex-info "Entry does not match spec" {:details (s/explain-data spec entry)}))))

(defn parse-lines
  [top-level-parser config [header & body]]
  {:pre (every? #(contains? config %) #{:body-processor :spec})}
  (let [[entry-type header-data] (top-level-parser header)
        {:keys [body-processor spec]} (get config entry-type)]
    [entry-type
     (validate-entry-spec
      spec
      (if (seq body)
        (reduce body-processor header-data body)
        header-data))]))

(def base-grammar-rules
  "date = #'\\d{4}/\\d{2}/\\d{2}';

   <comment> = #'//.+$'

   <whitespace> = #'\\h+';

   <word> = #'(\\w|\\p{L}|-|&)+';

   pre-tag = #'.+(?=(\\h+#)?)';
   tags = tag (<whitespace> tag)*;
   tag = <'#'> word ('/' word)*;

   account-name = account-type (<'/'> word)+;
   <account-type> = 'assets' | 'liabilities' | 'expenses' | 'incomes' | 'equity' | 'budget' | 'goal';

   currency = word;

   signed-number = positive-number | negative-number;
   positive-number = <'+'>? number;
   negative-number = <'-'> number;
   number = #'\\d+(\\.\\d+)?';

   signed-integer = positive-integer | negative-integer;
   positive-integer = <'+'>? integer;
   negative-integer = <'-'> integer;
   integer = #'\\d+';")

(defn transform-signed-integer
  ([[_ value]] value)
  ([[_ signal] [_ value]] (signal value)))

(defn transform-signal
  [signal]
  (case signal
    "+" +
    "-" -))

(defn transform-account-name
  [& args]
  (str/join "/" args))

(defn transform-tags
  [& tags]
  (into #{} (map second) tags))

(def transform-unwrap second)

(def transform-integer bigint)

(def transform-number bigdec)

(def base-transform-map
  (create-transform-map {:date             date-time/str->date
                         :tag              str
                         :tags             transform-tags
                         :integer          transform-integer
                         :positive-integer transform-unwrap
                         :negative-integer (comp - transform-unwrap)
                         :signed-integer   transform-unwrap
                         :positive-number  transform-unwrap
                         :negative-number  (comp - transform-unwrap)
                         :signed-number    transform-unwrap
                         :number           transform-number
                         :currency         keyword
                         :account-name     transform-account-name}))

(defn transform-map-with-base
  [custom-transform-map]
  (merge base-transform-map (into {} (map transformer-with-key) custom-transform-map)))

(defn grammar-with-rules ;; Warning: rule order is important
  [rules-str]
  (str rules-str base-grammar-rules))

(defn ensure-single-trailing-semicolon
  [s]
  (str/replace s #";*$" ";"))

(defn grammar-map->grammar-rules
  [grammar-map]
  (map (comp (partial str/join " = ")
             (juxt (comp name first)
                   (comp ensure-single-trailing-semicolon :rule second)))
       grammar-map))

(defn grammar-map->transform-map
  [grammar-map]
  (into {}
        (map (juxt first (comp :transformer second)))
        grammar-map))

(defn top-level-rule
  [top-level-rule-name grammar-map]
  (format "%s = <whitespace>? %s <whitespace>?;"
          (name top-level-rule-name)
          (->> grammar-map
               (map (comp name first))
               (str/join " | "))))

(defmacro defparser
  [name grammar-map]
  (let [top-level-rule-name (keyword (gensym))]
    `(def ~name
       (partial try-parse
                #{~top-level-rule-name}
                (insta/parser (grammar-with-rules (->> ~grammar-map
                                                       grammar-map->grammar-rules
                                                       (into [(top-level-rule ~top-level-rule-name ~grammar-map)])
                                                       (str/join "\n"))))
                (-> ~grammar-map grammar-map->transform-map transform-map-with-base)))))
