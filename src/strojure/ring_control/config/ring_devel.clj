(ns strojure.ring-control.config.ring-devel
  "Configuration functions for the middlewares from the `ring.middleware`
  namespace in `ring/ring-devel` package.

  NOTE: Requires `ring/ring-devel` to be added in project dependencies.
  "
  (:require [ring.middleware.lint :as lint]
            [ring.middleware.reload :as reload]
            [ring.middleware.stacktrace :as stacktrace]
            [strojure.ring-control.config.ring-core :as ring]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([])}
  lint-handler
  "Wrap a handler to validate incoming requests and outgoing responses
  according to the current Ring specification. An exception is raised if either
  the request or response is invalid."
  (ring/as-handler-fn `lint-handler (ring/without-options lint/wrap-lint)
                      {:tags [::lint-handler]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [dirs, reload-compile-errors?]}])}
  reload-handler
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
  (ring/as-handler-fn `reload-handler reload/wrap-reload
                      {:tags [::reload-handler]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [color?]}])}
  stacktrace-handler
  "Wrap a handler such that exceptions are caught, a corresponding stacktrace is
  logged to `*err*`, and an HTML representation of the stacktrace is returned as
  a response.

  Accepts the following option:

  - `:color?` – if true, apply ANSI colors to terminal stacktrace (default false)
  "
  (ring/as-handler-fn `stacktrace-handler stacktrace/wrap-stacktrace
                      {:tags [::stacktrace-handler]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
