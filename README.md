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

Ring handlers are build from handler function and builder configuration using
[handler/build][handler_build] function.

Configuration options:

- `:outer` – A sequence of standard ring middlewares to wrap handler before all
  other wraps.
- `:enter` – A sequence of Ring request functions `(fn [request] new-request)`.
- `:leave` – A sequence of Ring response
  functions `(fn [response request] new-response)`. The function receives
  same `request` as wrapping handler itself.
- `:inner` – A sequence of standard ring middlewares to wrap the `handler`
  after `:enter` and before `:leave` wraps.

The wraps are applying in direct order:

```clojure
;; Applies enter1 before enter2.
{:enter [`enter1
         `enter2]}
```

Configuration groups are applied as they are listed above:

- Request flow:
    - `:outer` −> `:enter` −> `:inner` −> handler.
- Response flow:
    - handler −> `:inner` −> `:leave` −> `:outer`.

Such configuration allows to distinguish between request/response only wraps,
control order of application more easy and naturally comparing with standard
usage of ring middlewares.

The wraps should be tagged with symbols (and optionally with convenient type
tags) using `config/as-wrap-handler`, `config/set-request-fn`,
`config/as-wrap-response` functions to be referred in configuration:

```clojure
{:enter [`enter1
         `enter2
         {:type `enter3 :opt1 true :opt2 false}]}
```

We can also define dependency of `` `enter2 `` on `` `enter1 `` using
`config/set-required` function:

```clojure
(config/set-required `enter2 {:enter [`enter1]})

;; This fails with exception about missing dependency:
(handler/build handler {:enter [`enter2]})

;; This fails with exception about wrong order:
(handler/build handler {:enter [`enter2 `enter1]})

;; But this succeeds anyway:
(handler/build handler {:enter [`enter2]
                        :ignore-required [`enter1]})
```

See also [more sophisticated example](doc/usage/walkthrough.clj).

---

[handler_build]:
https://cljdoc.org/d/com.github.strojure/ring-control/CURRENT/api/strojure.ring-control.handler#build
