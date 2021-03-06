(ns ^{:doc "Properties (aka configuration) of the application.
            Properties are stored in property files.  There is a
            default file which can be overridden by placing an
            alternative properties file on the classpath, or by
            setting system properties."}
    ctia.properties
  (:require [clj-momo.properties :as mp]
            [clj-momo.lib
             [map :as map]
             [schema :as mls]]
            [schema.core :as s]
            [schema-tools.core :as st]
            [ctia.store :refer [stores]]
            [ctia.store :as store])
  (:import java.util.Properties))

(def files
  "Property file names, they will be merged, with last one winning"
  ["ctia-default.properties"
   "ctia.properties"])

(defonce properties
  (atom {}))

(defn default-store-properties [store]
  {(str "ctia.store." store) s/Str})

(defn es-store-impl-properties [store]
  {(str "ctia.store.es." store ".transport") (s/enum :http :native)
   (str "ctia.store.es." store ".host") s/Str
   (str "ctia.store.es." store ".port") s/Int
   (str "ctia.store.es." store ".clustername") s/Str
   (str "ctia.store.es." store ".indexname") s/Str
   (str "ctia.store.es." store ".refresh") s/Bool})

(defn atom-store-impl-properties [store]
  {(str "ctia.store.atom." store ".mode") (s/enum :durable :memory)
   (str "ctia.store.atom." store ".path") s/Str})

(s/defschema StorePropertiesSchema
  "All entity store properties for every implementation"
  (let [configurable-stores (map name (keys @store/stores))
        store-names (conj configurable-stores "default")]
    (st/optional-keys
     (reduce merge {}
             (map (fn [s] (merge (default-store-properties s)
                                 (es-store-impl-properties s)
                                 (atom-store-impl-properties s)))
                  store-names)))))

(s/defschema PropertiesSchema
  "This is the schema used for value type coercion.
  It is also used for validating the properties that are read in so that required
   properties must be present.  Only the following properties may be
   set.  This is also used for selecting system properties to merge
   with the properties file."
  (st/merge
   StorePropertiesSchema
   (st/required-keys {"ctia.auth.type" s/Keyword})
   (st/optional-keys {"ctia.auth.threatgrid.cache" s/Bool
                      "ctia.auth.threatgrid.whoami-url" s/Str})

   (st/required-keys {"ctia.http.enabled" s/Bool
                      "ctia.http.port" s/Int
                      "ctia.http.min-threads" s/Int
                      "ctia.http.max-threads" s/Int})

   (st/optional-keys {"ctia.http.dev-reload" s/Bool
                      "ctia.http.show.protocol" s/Str
                      "ctia.http.show.hostname" s/Str
                      "ctia.http.show.path-prefix" s/Str
                      "ctia.http.show.port" s/Int
                      "ctia.http.bulk.max-size" s/Int})

   (st/required-keys {"ctia.nrepl.enabled" s/Bool
                      "ctia.hook.es.enabled" s/Bool
                      "ctia.hook.redis.enabled" s/Bool})

   (st/optional-keys {"ctia.events.log" s/Bool
                      "ctia.nrepl.port" s/Int
                      "ctia.hook.redis.host" s/Str
                      "ctia.hook.redis.port" s/Int
                      "ctia.hook.redis.channel-name" s/Str
                      "ctia.hook.redis.timeout-ms" s/Int

                      "ctia.hook.es.transport" (s/enum :http :native)
                      "ctia.hook.es.host" s/Str
                      "ctia.hook.es.port" s/Int
                      "ctia.hook.es.clustername" s/Str
                      "ctia.hook.es.indexname" s/Str
                      "ctia.hook.es.slicing.strategy" (s/enum :filtered-alias :aliased-index)
                      "ctia.hook.es.slicing.granularity" (s/enum :minute :hour :day :week :month :year)

                      "ctia.hooks.before-create" s/Str
                      "ctia.hooks.after-create" s/Str
                      "ctia.hooks.before-update" s/Str
                      "ctia.hooks.after-update" s/Str
                      "ctia.hooks.before-delete" s/Str
                      "ctia.hooks.after-delete" s/Str
                      "ctia.hooks.event" s/Str

                      "ctia.metrics.console.enabled" s/Bool
                      "ctia.metrics.console.interval" s/Int
                      "ctia.metrics.jmx.enabled" s/Bool
                      "ctia.metrics.riemann.enabled" s/Bool
                      "ctia.metrics.riemann.host" s/Str
                      "ctia.metrics.riemann.port" s/Int
                      "ctia.metrics.riemann.interval" s/Int})))

(def configurable-properties
  (mls/keys PropertiesSchema))

(def init! (mp/build-init-fn files
                             PropertiesSchema
                             properties))

(defn get-http-show []
  (get-in @properties [:ctia :http :show]))
