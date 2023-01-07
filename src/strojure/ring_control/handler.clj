(ns strojure.ring-control.handler
  "Functions for building Ring handler from configuration.")

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn validate-deps
  "Throws exception if `xs` contains middleware with `:deps` and corresponding
  dependency is missing. The `:deps` is a map of required middleware names with
  value `:before`/`:after`:

      [{:name `name1}
       {:name `name2 :deps {`name1 :before, `name3 :after}}
       {:name `name3}]
  "
  ([xs] (validate-deps xs nil))
  ([xs ignored]
   (letfn [(match-name [n] (fn [x] (isa? x n)))]
     (doseq [{x-name :name deps :deps} (filter :deps xs)
             [dep-name k],,,,,,,,,,,,, deps
             :when (not (some->> ignored (some (match-name dep-name))))]
       (when-not (->> (cond-> xs (= :after k) reverse)
                      (keep :name)
                      (take-while (complement (match-name x-name)))
                      (some (match-name dep-name)))
         (throw (ex-info (str "Require middleware " (pr-str dep-name) " " (name k) " " (pr-str x-name))
                         {:names (keep :name xs)})))))))

(comment
  (validate-deps [{:name `name1}
                  {:name `name2 :deps {`name1 :before, `name3 :after}}
                  {:name `name3}])
  (validate-deps [{:name `name2 :deps {`name1 :before, `name3 :after}}
                  {:name `name1}
                  {:name `name3}]
                 #{`name1})
  )

(defn- map->wrap
  [async? {:keys [wrap enter leave] :as m}]
  (when m
    (cond wrap
          (do (when (or enter leave)
                (throw (ex-info (str "Cannot use :enter/:leave and :wrap simultaneously " m) m)))
              wrap)
          (or enter leave)
          (fn [handler]
            (cond
              (and enter leave)
              (if async? (fn async-handler
                           [request respond raise]
                           (let [request (enter request)]
                             (handler request
                                      (fn [resp] (respond (leave resp request)))
                                      raise)))
                         (fn sync-handler
                           [request]
                           (let [request (enter request)]
                             (leave (handler request) request))))
              enter
              (if async? (fn async-handler
                           [request respond raise]
                           (handler (enter request) respond raise))
                         (fn sync-handler
                           [request]
                           (handler (enter request))))
              leave
              (if async? (fn async-handler
                           [request respond raise]
                           (handler request
                                    (fn [resp] (respond (leave resp request)))
                                    raise))
                         (fn sync-handler
                           [request]
                           (leave (handler request) request)))))
          :else
          (throw (ex-info (str "Require :wrap/:enter/:leave key in " m) m)))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn build
  "Returns ring handler wrapped with middleware configurations `xs` in direct
  order.

  Default type of ring handler is sync handler. To produce async ring handler
  use either `:async` option or `{:async true}` in handler's meta.

  Every middleware is a map with keys:

  - `{:keys [wrap]}`

      - `:wrap`  – a function `(fn [handler] new-handler)` to wrap handler.

  - `{:keys [enter leave]}`

      - `:enter` — a function `(fn [request] new-request)` to transform request.
      - `:leave` – a function `(fn [response request] new-response)` to transform
                   response.

  - Optional middleware keys:

      - `:name` – a name symbol/keyword as middleware description, also used as
                  reference in `:deps` key.
      - `:deps` — a map of required middleware names with value
                  `:before`/`:after`, see [[validate-deps]].
      - `:meta` — middleware specific data which can be used to distinguish
                  middlewares with same name but different options, i.e.
                  `:root-path` in `wrap-resource`.

  Maps with `:wrap` and `:enter`/`:leave` causes exception.

  Only middlewares with `:wrap` can short-circuit, `:enter`/`:leave` just modify
  request/response.

  The middlewares are applied in the order:

  - Request flows from first to last.
  - Response flows from last to first.
  - Every middleware receives request from *previous* `:wrap`/`:enter`
    middlewares only.
  - Every `:leave`/`:wrap` receives response from *next* middlewares.

  The `xs` is validating for dependencies with [[validate-deps]]. The option
  `:ignored-deps` contains dependency names to ignore in validation.
  "
  {:arglists '([handler xs]
               [handler xs {:keys [async, ignored-deps]}])}
  ([handler xs] (build handler xs {}))
  ([handler xs options]
   (validate-deps xs (:ignored-deps options))
   (let [async? (:async options (-> handler meta :async))
         handler (->> xs
                      (keep (partial map->wrap async?))
                      (reverse)
                      (reduce (fn [handler wrapper] (wrapper handler))
                              handler))]
     (-> handler (vary-meta assoc :async async?)))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
