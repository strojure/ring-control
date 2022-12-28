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

### `wrap-handler`

Ring handlers are build from handler function and middleware configuration using
[wrap-handler] function.

- `:outer` Standard ring middlewares to wrap around all other wrappers.
- `:enter` Ring request wrapping functions `(fn [request] new-request)`.
- `:leave` Ring response wrapping
  functions `(fn [response request] new-response)`.
  The function receive same `request` as wrapping handler itself.
- `:inner` Standard ring middlewares to wrap just around `handler` after
  `:enter` and before `:leave`.

Wrapper are applying in direct order:

```clojure
;; Call `(enter1 request)` before `(enter2 request)`.
{:enter [enter1
         enter2]}
```

Configuration groups are applied as they are listed above:

- Request flow:
    - `:outer` −> `:enter` −> `:inner` −> handler.
- Response flow:
    - handler −> `:inner` −> `:leave` −> `:outer`.

Such configuration allows to distinguish between request/response handlers,
control order of wrappers more easy and naturally comparing with usage of
standard ring middlewares only.

Wrapping functions can be defined with types using multimethods
`as-handler-wrap`, `as-request-wrap`, `as-response-wrap` and be referred
in configuration:

```clojure
{:enter [::enter1
         ::enter2
         {:type ::enter3 :opt1 true :opt2 false}]}
```

Same type can be defined as request wrapper and as response wrapper. They
should be specified in `:enter` and `:leave` independently.

In this case we can also define dependency of `::enter2` on `::enter2` using
`require-config` multimethod:

```clojure
(defmethod require-config ::enter2 [_]
  {:enter [::enter1]})

;; This fails with exception about missing middleware:
(wrap-handler handler {:enter [::enter2]})

;; This fails with exception about wrong order:
(wrap-handler handler {:enter [::enter2 ::enter1]})

;; But this succeeds anyway:
(wrap-handler handler {:enter [::enter2]
                       :ignore-required [::enter1]})
```

See also [more sophisticated example](doc/usage/core_wrap_handler.clj).

---

[wrap-handler]:
https://cljdoc.org/d/com.github.strojure/ring-stack/CURRENT/api/strojure.ring-stack.core#wrap-handler
