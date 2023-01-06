(ns strojure.ring-control.handler
  "Functions for building Ring handler from configuration.")

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

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

(defn build
  "Returns ring handler wrapped with middlewares `xs` in direct order.

  Default type of ring handler is sync handler. To produce async ring handler
  use either `:async` option or `{:async true}` in handler's meta.

  Every middleware is a map with keys:

  - `{:keys [wrap]}`

      - `:wrap`  – a function `(fn [handler] new-handler)` to wrap handler.

  - `{:keys [enter leave]}`

      - `:enter` — a function `(fn [request] new-request)` to transform request.
      - `:leave` – a function `(fn [response request] new-response)` to transform
                   response.

  Maps with `:wrap` and `:enter`/`:leave` causes exception.

  Only middlewares with `:wrap` can short-circuit, `:enter`/`:leave` just modify
  request/response.

  The middlewares are applied in the order:

  - Request flows from first to last.
  - Response flows from last to first.
  - Every middleware receives request from *previous* `:wrap`/`:enter`
    middlewares only.
  - Every `:leave`/`:wrap` receives response from *next* middlewares.
  "
  {:arglists '([handler xs]
               [handler xs {:keys [async]}])}
  ([handler xs] (build handler xs {}))
  ([handler xs options]
   (let [async? (:async options (-> handler meta :async))
         handler (->> xs
                      (keep (partial map->wrap async?))
                      (reverse)
                      (reduce (fn [handler wrapper] (wrapper handler))
                              handler))]
     (-> handler (vary-meta assoc :async async?)))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
