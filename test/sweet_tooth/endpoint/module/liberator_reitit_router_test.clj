(ns sweet-tooth.endpoint.module.liberator-reitit-router-test
  (:require [sweet-tooth.endpoint.module.liberator-reitit-router :as sut]
            [sweet-tooth.endpoint.middleware :as em]
            [sweet-tooth.endpoint.test.harness :as eth]
            [sweet-tooth.endpoint.system :as es]
            [sweet-tooth.endpoint.routes.reitit :as err]

            [ring.mock.request :as mock]
            [duct.core :as duct]
            [duct.logger]
            [integrant.core :as ig]
            [clojure.test :refer :all]
            [shrubbery.core :as shrub]
            [sweet-tooth.endpoint.format :as ef]))

(duct/load-hierarchy)

(def decisions
  {:collection {:get    {:handle-ok ["COLL GET"]}
                :post   {:handle-created ["COLL POST"]}
                :put    {:handle-ok ["COLL PUT"]}
                :delete {:respond-with-entity? true
                         :handle-ok            ["COLL DELETE"]}}

   :member {:get     {:handle-ok ["ENT GET"]}
            :post    {:handle-created ["ENT POST"]}
            :put     {:handle-ok ["ENT PUT"]}
            :delete  {:respond-with-entity? true
                      :handle-ok            ["ENT DELETE"]}
            :unknown {}}

   :member/history {:get    {:handle-ok ["ENT HISTORY GET"]}
                    :post   {:handle-created ["ENT HISTORY POST"]}
                    :put    {:handle-ok ["ENT HISTORY PUT"]}
                    :delete {:respond-with-entity? true
                             :handle-ok            ["ENT HISTORY DELETE"]}}})

(def ns-routes
  (err/expand-routes [[:sweet-tooth.endpoint.module.liberator-reitit-router-test
                       {::err/expand-with [:collection :member :member/history
                                           ["/custom/path" {:name :custom-path}]]}]
                      ["/" {:woo :yeah :handler "x"}]]))

(def duct-config
  {:duct.profile/base {:duct.core/project-ns  'sweet-tooth
                       :duct.core/environment :production}

   :sweet-tooth.endpoint.module/liberator-reitit-router {:routes ::ns-routes}

   :duct.module/logging {}
   :duct.module.web/api {}

   :sweet-tooth.endpoint.module/middleware {}})

(deftest builds-duct-config
  (let [duct-config (-> (select-keys (duct/prep-config duct-config) [::sut/reitit-router ::collection-handler ::member-handler
                                                                     ::member-history-handler ::custom-path-handler])
                        ;; TODO figure out how to not have to do these
                        ;; shenanigans

                        ;; dropping the first middleware because it's
                        ;; an anonymous function so I can't really test for that
                        (update ::sut/reitit-router #(mapv (fn [route]
                                                             (update-in route [1 :middleware] (fn [x] (drop 1 x))))
                                                           %)))]

    (is (= #{::collection-handler ::member-handler ::sut/reitit-router ::member-history-handler ::custom-path-handler}
           (set (keys duct-config))))

    (testing "router"
      (is (= {::sut/reitit-router [["/module/liberator-reitit-router-test"
                                    {:name          :module.liberator-reitit-router-tests
                                     :id-key        :id
                                     :auth-id-key   :id
                                     ::ef/formatter ::ef/segments
                                     :ent-type      :liberator-reitit-router-test
                                     :middleware    [em/wrap-merge-params]
                                     :handler       (ig/ref ::collection-handler)
                                     ::err/ns       :sweet-tooth.endpoint.module.liberator-reitit-router-test
                                     ::err/type     :collection}]
                                   ["/module/liberator-reitit-router-test/{id}"
                                    {:name          :module.liberator-reitit-router-test
                                     :id-key        :id
                                     :auth-id-key   :id
                                     ::ef/formatter ::ef/segments
                                     :ent-type      :liberator-reitit-router-test
                                     :middleware    [em/wrap-merge-params]
                                     :handler       (ig/ref ::member-handler)
                                     ::err/ns       :sweet-tooth.endpoint.module.liberator-reitit-router-test
                                     ::err/type     :member}]
                                   ["/module/liberator-reitit-router-test/{id}/history"
                                    {:name          :module.liberator-reitit-router-test/history
                                     :id-key        :id
                                     :auth-id-key   :id
                                     ::ef/formatter ::ef/segments
                                     :ent-type      :liberator-reitit-router-test
                                     :middleware    [em/wrap-merge-params]
                                     :handler       (ig/ref ::member-history-handler)
                                     ::err/ns       :sweet-tooth.endpoint.module.liberator-reitit-router-test
                                     ::err/type     :member/history}]
                                   ["/module/liberator-reitit-router-test/custom/path"
                                    {:name          :custom-path
                                     :handler       (ig/ref ::custom-path-handler)
                                     :ent-type      :liberator-reitit-router-test
                                     :middleware    [em/wrap-merge-params]
                                     :id-key        :id
                                     :auth-id-key   :id
                                     ::ef/formatter ::ef/segments
                                     ::err/ns       :sweet-tooth.endpoint.module.liberator-reitit-router-test
                                     ::err/type     :custom-path}]
                                   ["/" {:woo        :yeah
                                         :handler    "x"
                                         :middleware [em/wrap-merge-params]}]]}
             (select-keys duct-config [::sut/reitit-router]))))

    (testing "coll handler"
      (is (= {::collection-handler {:name        :module.liberator-reitit-router-tests
                                    :id-key      :id
                                    :auth-id-key :id
                                    :ctx         {:id-key                         :id
                                                  :auth-id-key                    :id
                                                  :logger                         (ig/ref :duct/logger)
                                                  :sweet-tooth.endpoint/namespace :sweet-tooth.endpoint.module.liberator-reitit-router-test}
                                    :decisions   'decisions
                                    :ent-type    :liberator-reitit-router-test
                                    ::err/ns     :sweet-tooth.endpoint.module.liberator-reitit-router-test
                                    ::err/type   :collection
                                    ::sut/path   "/module/liberator-reitit-router-test"}}
             (select-keys duct-config [::collection-handler]))))

    (testing "ent handler"
      (is (= {::member-handler {:name        :module.liberator-reitit-router-test
                                :id-key      :id
                                :auth-id-key :id
                                :ctx         {:id-key                         :id
                                              :auth-id-key                    :id
                                              :logger                         (ig/ref :duct/logger)
                                              :sweet-tooth.endpoint/namespace :sweet-tooth.endpoint.module.liberator-reitit-router-test}
                                :decisions   'decisions
                                :ent-type    :liberator-reitit-router-test
                                ::err/ns     :sweet-tooth.endpoint.module.liberator-reitit-router-test
                                ::err/type   :member
                                ::sut/path   "/module/liberator-reitit-router-test/{id}"}}
             (select-keys duct-config [::member-handler]))))))

(defmethod es/config ::test [_]
  (dissoc (duct/prep-config duct-config) :duct.server.http/jetty))

;; With reitit-routed handlers as the final product of a lot of magic,
;; this test also tests other layers in the system, like gzip
;; middleware
(deftest handler-works
  (let [url "/module/liberator-reitit-router-test"
        ent-url (str url "/1")
        ent-history-url (str ent-url "/history")]
    (eth/with-system ::test
      (is (= ["COLL GET"]
             (eth/read-body (eth/req :get url))))
      (is (= ["COLL POST"]
             (eth/read-body (eth/req :post url))))
      (is (= ["COLL PUT"]
             (eth/read-body (eth/req :put url))))
      (is (= ["COLL DELETE"]
             (eth/read-body (eth/req :delete url))))

      (is (= ["ENT GET"]
             (eth/read-body (eth/req :get ent-url))))
      (is (= ["ENT POST"]
             (eth/read-body (eth/req :post ent-url))))
      (is (= ["ENT PUT"]
             (eth/read-body (eth/req :put ent-url))))
      (is (= ["ENT DELETE"]
             (eth/read-body (eth/req :delete ent-url))))

      (is (= ["ENT HISTORY GET"]
             (eth/read-body (eth/req :get ent-history-url))))
      (is (= ["ENT HISTORY POST"]
             (eth/read-body (eth/req :post ent-history-url))))
      (is (= ["ENT HISTORY PUT"]
             (eth/read-body (eth/req :put ent-history-url))))
      (is (= ["ENT HISTORY DELETE"]
             (eth/read-body (eth/req :delete ent-history-url))))

      (is (= {"Content-Type"           "application/transit+json"
              "Content-Encoding"       "gzip"
              "Vary"                   "Accept, Accept-Encoding"}
             (-> (eth/base-request :get "/module/liberator-reitit-router-test" {})
                 (mock/header "accept-encoding" "gzip")
                 ((eth/handler))
                 :headers
                 (dissoc "Set-Cookie")))))))

(deftest ns-routes-exception-handling
  (is (#'sut/resolve-ns-routes ::ns-routes))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Your duct configuration for :sweet-tooth.endpoint.module/liberator-reitit-router is incorrect. Could not find the var specified by :routes."
                        (#'sut/resolve-ns-routes ::blar))))

(deftest default-serves-index-html
  (eth/with-system ::test
    (is (= "hi! i show the fallback route works.\n"
           (slurp (:body ((eth/handler) (mock/request :get "/no-route"))))))))

(deftest warns-unknown-methods
  (eth/with-custom-system ::test {:duct.logger/timbre (es/shrubbery-mock {})}
    (is (shrub/received? (eth/component :duct.logger/timbre)
                         duct.logger/-log
                         [:warn]))))
