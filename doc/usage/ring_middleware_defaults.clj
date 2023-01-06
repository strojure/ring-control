(ns usage.ring-middleware-defaults
  (:require [ring.middleware.defaults :as ring-defaults]
            [strojure.ring-control.config.ring-middleware-defaults :as defaults]
            [strojure.ring-control.handler :as handler]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn -handler
  "Test handler"
  [request]
  {:request request
   :cookies {:a 1}
   :session {:a 1}})

(def -request
  "Test request."
  {:request-method :get
   :scheme :https :uri "/index.html" :query-string "a=1&foo[]=bar"
   :headers {"host" "localhost"}})

(defn handle-request
  [config request]
  ((handler/build -handler config) request))

(-> (defaults/config ring-defaults/api-defaults)
    (handle-request -request))
#_{:request {:request-method :get,
             :scheme :https,
             :uri "/index.html",
             :query-string "a=1&foo[]=bar",
             :headers {"host" "localhost"},
             :form-params {},
             :params {:a "1", "foo[]" "bar"},
             :query-params {"a" "1", "foo[]" "bar"}},
   :cookies {:a 1},
   :session {:a 1},
   :headers {"Content-Type" "text/html; charset=utf-8"}}

(-> (defaults/config ring-defaults/secure-api-defaults)
    (handle-request -request))
#_{:request {:request-method :get,
             :scheme :https,
             :uri "/index.html",
             :query-string "a=1&foo[]=bar",
             :headers {"host" "localhost"},
             :form-params {},
             :params {:a "1", "foo[]" "bar"},
             :query-params {"a" "1", "foo[]" "bar"}},
   :cookies {:a 1},
   :session {:a 1},
   :headers {"Content-Type" "text/html; charset=utf-8",
             "Strict-Transport-Security" "max-age=31536000; includeSubDomains"}}

(-> (defaults/config ring-defaults/site-defaults)
    (handle-request -request))
#_{:request {:cookies {},
             :params {:a "1", :foo ["bar"]},
             :flash nil,
             :headers {"host" "localhost"},
             :form-params {},
             :session/key nil,
             :query-params {"a" "1", "foo[]" "bar"},
             :uri "/index.html",
             :anti-forgery-token "a27lBKV6DL2X3G231d383IpLdQFkbmtzeNmFndg3Vl9ZyBC8rYtE2cxY2ewY8myXOndOHwVwVHgNuTZB",
             :query-string "a=1&foo[]=bar",
             :multipart-params {},
             :scheme :https,
             :request-method :get,
             :session {}},
   :headers {"Set-Cookie" '("a=1" "ring-session=68b8d22e-ffaf-4862-9eca-a1a21e8068dd;Path=/;HttpOnly;SameSite=Strict"),
             "Content-Type" "text/html; charset=utf-8",
             "X-Frame-Options" "SAMEORIGIN",
             "X-Content-Type-Options" "nosniff"}}

(-> (defaults/config ring-defaults/secure-site-defaults)
    (handle-request -request))
#_{:request {:cookies {},
             :params {:a "1", :foo ["bar"]},
             :flash nil,
             :headers {"host" "localhost"},
             :form-params {},
             :session/key nil,
             :query-params {"a" "1", "foo[]" "bar"},
             :uri "/index.html",
             :anti-forgery-token "XpVlSa35E5yLyU1MJonSuD5AXDZ1jW+DFaKcRlVQ/78kbN3bseupxOC91BbA7W++IL4ycvYkPQS9zziz",
             :query-string "a=1&foo[]=bar",
             :multipart-params {},
             :scheme :https,
             :request-method :get,
             :session {}},
   :headers {"Set-Cookie" '("a=1" "secure-ring-session=310594cf-f7dd-4935-9eea-8f3256402217;Path=/;HttpOnly;SameSite=Strict;Secure"),
             "Content-Type" "text/html; charset=utf-8",
             "X-Frame-Options" "SAMEORIGIN",
             "X-Content-Type-Options" "nosniff",
             "Strict-Transport-Security" "max-age=31536000; includeSubDomains"}}
