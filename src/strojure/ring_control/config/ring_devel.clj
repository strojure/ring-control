(ns strojure.ring-control.config.ring-devel
  "Configuration functions for the middlewares from the `ring.middleware`
  namespace in `ring/ring-devel` package.

  NOTE: Requires `ring/ring-devel` to be added in project dependencies.
  "
  (:require [ring.middleware.lint :as lint]
            [ring.middleware.reload :as reload]
            [ring.middleware.stacktrace :as stacktrace]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn wrap-lint
  "Wrap a handler to validate incoming requests and outgoing responses
  according to the current Ring specification. An exception is raised if either
  the request or response is invalid."
  {:arglists '([] [false])}
  [& {:as options}]
  (when-not (false? options)
    {:name `wrap-lint
     :wrap lint/wrap-lint}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn wrap-reload
  "Reload namespaces of modified files before the request is passed to the
  supplied handler.

  Accepts the following options:

  - `:dirs`
      + A list of directories that contain the source files.
      + Defaults to `[\"src\"]`.

  - `:reload-compile-errors?`
      + If true, keep attempting to reload namespaces that have compile errors.
      + Defaults to `true`.
  "
  {:arglists '([& {:keys [dirs, reload-compile-errors?]}]
               [false])}
  [& {:as options}]
  (when-not (false? options)
    (let [options (or options {})]
      {:name `wrap-reload
       :wrap (fn [handler] (reload/wrap-reload handler options))})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn wrap-stacktrace
  "Wrap a handler such that exceptions are caught, a corresponding stacktrace is
  logged to `*err*`, and an HTML representation of the stacktrace is returned as
  a response.

  Accepts the following option:

  - `:color?` – if true, apply ANSI colors to terminal stacktrace (default false)
  "
  {:arglists '([& {:keys [color?]}])}
  [& {:as options}]
  (when-not (false? options)
    (let [options (or options {})]
      {:name `wrap-stacktrace
       :wrap (fn [handler] (stacktrace/wrap-stacktrace handler options))})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
