# ring-stack

More controllable composition of Ring middlewares.

[![cljdoc badge](https://cljdoc.org/badge/com.github.strojure/ring-stack)](https://cljdoc.org/d/com.github.strojure/ring-stack)
[![Clojars Project](https://img.shields.io/clojars/v/com.github.strojure/ring-stack.svg)](https://clojars.org/com.github.strojure/ring-stack)

## Motivation

This library is attempt to resolve following difficulties with usage of Ring
middlewares:

- Unclear middleware responsibility, if they handle request, response or both.
- No hints for middleware dependencies, when one middleware works only after
  another.
- Inverted order when written with `->` macro what makes hard to reason about
  actual request/response flow between wrappers.

## Usage

Ring handlers are build from handler function and middleware configuration using
[handler_build] function.

- `:outer` Standard ring middlewares to wrap around all other middlewares.
- `:enter` Ring request functions `(fn [request] new-request)`.
- `:leave` Ring response functions `(fn [response request] new-response)`.
  The function receive same `request` as wrapping handler itself.
- `:inner` Standard ring middlewares to wrap just `handler` after `:enter` and
  before `:leave` middlewares.

Middlewares are applying in direct order:

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

Such configuration allows to distinguish between request/response only
middleware, control order of application more easy and naturally comparing with
standard usage of ring middlewares.

Middleware functions should be associated with symbols (and additional
convenient type aliases) using `middleware/set-handler-fn`,
`middleware/set-request-fn`, `middleware/set-response-fn` functions to be
referred in configuration:

```clojure
{:enter [`enter1
         `enter2
         {:type `enter3 :opt1 true :opt2 false}]}
```

Same type symbol can be used for request and response. It is used in `:enter`
and `:leave` independently.

We can also define dependency of `` `enter2 `` on `` `enter1 `` using
`middleware/set-require-config` function:

```clojure
(middleware/set-require-config `enter2 {:enter [`enter1]})

;; This fails with exception about missing middleware:
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
https://cljdoc.org/d/com.github.strojure/ring-stack/CURRENT/api/strojure.ring-stack.handler#build
