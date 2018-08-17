(ns status-im.native-module.impl.module
  (:require-macros
   [cljs.core.async.macros :refer [go-loop go]])
  (:require [status-im.ui.components.react :as r]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [cljs.core.async :as async]
            [status-im.utils.platform :as p]
            [status-im.utils.async :as async-util]
            [status-im.react-native.js-dependencies :as rn-dependencies]
            [clojure.string :as string]))

;; if StatusModule is not initialized better to store
;; calls and make them only when StatusModule is ready
;; this flag helps to handle this
(defonce module-initialized? (atom (or p/ios? js/goog.DEBUG p/desktop?)))

;; array of calls to StatusModule
(defonce calls (atom []))

(defn module-initialized! []
  (reset! module-initialized? true))

(defn store-call [args]
  (log/debug :store-call args)
  (swap! calls conj args))

(defn call-module [f]
  ;;(log/debug :call-module f)
  (if @module-initialized?
    (f)
    (store-call f)))

(defonce loop-started (atom false))

(when-not @loop-started
  (go-loop [_ nil]
    (reset! loop-started true)
    (if (and (seq @calls) @module-initialized?)
      (do (swap! calls (fn [calls]
                         (doseq [call calls]
                           (call))))
          (reset! loop-started false))
      (recur (async/<! (async-util/timeout 500))))))

(def status
  (when (exists? (.-NativeModules rn-dependencies/react-native))
    (.-Status (.-NativeModules rn-dependencies/react-native))))

(defonce listener-initialized (atom false))

(when-not @listener-initialized
  (reset! listener-initialized true)
  (.addListener r/device-event-emitter "gethEvent"
                #(re-frame/dispatch [:signal-event (.-jsonEvent %)])))

(defn should-move-to-internal-storage? [on-result]
  (when status
    (call-module #(.shouldMoveToInternalStorage status on-result))))

(defn move-to-internal-storage [on-result]
  (when status
    (call-module #(.moveToInternalStorage status on-result))))

(defn stop-node []
  (when status
    (call-module #(.stopNode status))))

(defn start-node [config]
  (when status
    (call-module #(.startNode status config))))

(defonce account-creation? (atom false))

(defn create-account [password on-result]
  (when status
    (let [callback (fn [data]
                     (reset! account-creation? false)
                     (on-result data))]
      (swap! account-creation?
             (fn [creation?]
               (if-not creation?
                 (do
                   (call-module #(.createAccount status password callback))
                   true)
                 false))))))

(defn notify-users [{:keys [message payload tokens] :as m} on-result]
  (when status
    (call-module #(.notifyUsers status message payload tokens on-result))))

(defn add-peer [enode on-result]
  (when status
    (call-module #(.addPeer status enode on-result))))

(defn recover-account [passphrase password on-result]
  (when status
    (call-module #(.recoverAccount status passphrase password on-result))))

(defn login [address password save-password on-result]
  (when status
    (call-module #(.login status address password save-password on-result))))

(defn approve-sign-request
  [id password callback]
  (log/debug :approve-sign-request (boolean status) id)
  (when status
    (call-module #(.approveSignRequest status id password callback))))

(defn approve-sign-request-with-args
  [id password gas gas-price callback]
  (log/debug :approve-sign-request-with-args (boolean status) id gas gas-price)
  (when status
    (call-module #(.approveSignRequestWithArgs status id password gas gas-price callback))))

(defn discard-sign-request
  [id]
  (log/debug :discard-sign-request id)
  (when status
    (call-module #(.discardSignRequest status id))))

(defn- append-catalog-init [js]
  (str js "\n" "var catalog = JSON.stringify(_status_catalog); catalog;"))

(defn set-soft-input-mode [mode]
  (when status
    (call-module #(.setSoftInputMode status mode))))

(defn clear-web-data []
  (when status
    (call-module #(.clearCookies status))
    (call-module #(.clearStorageAPIs status))))

(defn call-web3 [payload callback]
  (when status
    (call-module #(.sendWeb3Request status payload callback))))

(defn call-web3-private [payload callback]
  (when status
    (call-module #(.sendWeb3PrivateRequest status payload callback))))

(defn close-application []
  (.closeApplication status))

(defn connection-change [{:keys [type expensive?]}]
  (.connectionChange status type expensive?))

(defn app-state-change [state]
  (.appStateChange status state))

(defn get-device-UUID [callback]
  (call-module
   #(.getDeviceUUID
     status
     (fn [UUID]
       (callback (string/upper-case UUID))))))
