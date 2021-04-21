(ns budget.reporter.common
  (:require [budget.logic.money :as logic.money]
            [budget.model.date-time :as model.date-time]
            [budget.model.money :as model.money]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [net.danielcompton.defn-spec-alpha :as ds]))

(def ^:dynamic *indentation* 0)

(defmacro indent
  [n & body]
  `(binding [*indentation* (+ ~n *indentation*)]
     ~@body))

(defn println'
  [& stuff]
  (print (str/join (repeat *indentation* \space)))
  (apply println stuff))

(ds/defn print-monetary-values
  [values :- (s/coll-of ::model.money/money)]
  (when-let [formatted-values (->> values
                                   (remove logic.money/zero-value?)
                                   (map logic.money/money->string)
                                   seq)]
    (let [max-length (count (apply max-key count formatted-values))
          format-string (str "%" max-length "s")]
      (doseq [value formatted-values]
        (println' (format format-string value))))))

(defn monthly-report-spec
  [spec]
  (s/tuple ::model.date-time/date spec))
(defn yearly-report-spec
  [spec]
  (s/tuple int? spec))
(defn periodic-report-spec
  [spec]
  (s/or :yearly-report (yearly-report-spec spec)
        :monthly-report (monthly-report-spec spec)))

(ds/defn sorted-report :- (s/coll-of (periodic-report-spec any?))
  [monthly-reports :- (s/coll-of (monthly-report-spec any?))
   yearly-reports :- (s/coll-of (yearly-report-spec any?))]
  (loop [[[month-year :as monthly] & rest-monthly :as monthly-reports] (sort-by first monthly-reports)
         [[year :as yearly] & rest-yearly :as yearly-reports] (sort-by first yearly-reports)
         result []]
    (cond
      (and (nil? monthly)
           (nil? yearly))
      result

      (nil? monthly)
      (recur rest-monthly
             rest-yearly
             (conj result yearly))

      (nil? yearly)
      (recur rest-monthly
             rest-yearly
             (conj result monthly))

      (< year (model.date-time/year month-year))
      (recur monthly-reports
             rest-yearly
             (conj result yearly))

      :else
      (recur rest-monthly
             yearly-reports
             (conj result monthly)))))

(ds/defn ^:private present-monthly-report!
  [report :- (monthly-report-spec any?)
   present-report-contents! :- (s/fspec :args any?)]
  (let [[month-year delta] report]
    (println' (model.date-time/month-year-string month-year))
    (indent 2
      (present-report-contents! delta))))

(ds/defn ^:private present-yearly-report!
  [report :- (yearly-report-spec any?)
   present-report-contents! :- (s/fspec :args any?)]
  (let [[year delta] report]
    (println' year)
    (indent 2
      (present-report-contents! delta))))

(ds/defn present-periodic-reports!
  "present-report-contents! is the function that will present the contents of each periodic report"
  [report :- (s/coll-of (periodic-report-spec any?))
   present-report-contents! :- (s/fspec :args any?)]
  (indent 2
    (doseq [periodic-report report
            :let [[report-type report-contents] (s/conform (periodic-report-spec any?) periodic-report)]]
      (case report-type
        :monthly-report
        (present-monthly-report! report-contents present-report-contents!)

        :yearly-report
        (present-yearly-report! report-contents present-report-contents!)))))
