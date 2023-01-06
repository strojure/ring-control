# ring-control

More controllable composition of Ring middlewares.

[![cljdoc badge](https://cljdoc.org/badge/com.github.strojure/ring-control)](https://cljdoc.org/d/com.github.strojure/ring-control)
[![Clojars Project](https://img.shields.io/clojars/v/com.github.strojure/ring-control.svg)](https://clojars.org/com.github.strojure/ring-control)

## Motivation

This library is attempt to resolve following difficulties with usage of Ring
middlewares:

- Unclear middleware responsibility, if they handle request, response or both.
- No hints for middleware dependencies, when one middleware works only after
  another.
- Inverted order when written with `->` macro what makes hard to reason about
  actual request/response flow between wrappers.

## Usage

### Building handlers

Ring handlers are build from handler function and sequence of middleware
configurations using [handler/build][handler_build] function.

Default type of ring handler is sync handler. To produce async ring handler
use either `:async` option or `{:async true}` in handler's meta.

Every middleware configuration is a map with keys:

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

See also [walkthrough](doc/usage/walkthrough.clj).

### Ring middlewares

TODO

---

[handler_build]:
https://cljdoc.org/d/com.github.strojure/ring-control/CURRENT/api/strojure.ring-control.handler#build
