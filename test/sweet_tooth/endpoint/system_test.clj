(ns sweet-tooth.endpoint.system-test
  (:require [clojure.test :refer [deftest is]]
            [sweet-tooth.endpoint.system :as es]
            [integrant.core :as ig]
            [shrubbery.core :as shrub]))

(defmethod ig/init-key ::a [_ opts]
  opts)

(defmethod es/config ::test [_]
  {::a :b})

(deftest system-test
  (is (= {::a :b}
         (es/system ::test))))

(deftest custom-system-test
  (is (= {::a :c}
         (es/system ::test {::a :c})))

  (is (= {::a :c}
         (es/system ::test (fn [cfg] (assoc cfg ::a :c))))))


(defmethod ig/init-key ::b [_ opts]
  {:opts opts})

(defmethod es/config ::replace-test [_]
  {::b {:foo :bar}})

(defprotocol Stubby
  (blurm [_]))

(deftest replace-component
  (is (= {::b {:opts {:foo :bar}}}
         (es/system ::replace-test)))

  (is (= {::b {:replacement :component}}
         (es/system ::replace-test {::b (es/replacement {:replacement :component})})))

  (let [system (es/system ::replace-test {::b (es/replacement (shrub/stub Stubby {:blurm "blurmed!"}))})]
    (is (= "blurmed!" (blurm (::b system))))))
