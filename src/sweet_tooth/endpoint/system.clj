(ns sweet-tooth.endpoint.system
  (:require [integrant.core :as ig]
            [meta-merge.core :as mm]))


(defmulti config
  "Provides a way for client application to name different integrant configs,
  e.g. :test, :dev, :prod, etc"
  identity)

(defrecord Replacement [component])
(defn replacement [component]
  (with-meta (Replacement. component) {:replace true}))

(defn init-key
  "If a component config is a Replacement, then use the replacement
  component instead of initializing it with `ig/init-key`"
  [k v]
  (if (= Replacement (type v))
    (:component v)
    (ig/init-key k v)))

(defn init
  "Like integrant.core/init but allows config values be
  `Replacement`s. The replacements will be used instead of whatever
  would have gotten returned by `ig/init-key`. This makes it much
  easier to use an alternative implementation for a component, for
  instance when mocking them."
  ([config]
   (init config (keys config)))
  ([config keys]
   {:pre [(map? config)]}
   (ig/build config keys init-key #'ig/assert-pre-init-spec ig/resolve-key)))

(defn system
  [config-name & [custom-config]]
  (let [cfg (config config-name)]
    (init (cond (not custom-config)  cfg
                (map? custom-config) (mm/meta-merge cfg custom-config)
                (fn? custom-config)  (custom-config cfg)))))
