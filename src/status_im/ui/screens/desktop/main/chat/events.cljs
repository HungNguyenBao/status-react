(ns screens.desktop.main.chat.events
  (:require [re-frame.core :as re-frame]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.handlers :as handlers]))

(defn show-profile-desktop [identity {:keys [db] :as cofx}]
  (navigation/navigate-to-cofx :chat-profile nil {:db (assoc db :contacts/identity identity)}))

(handlers/register-handler-fx
 :show-profile-desktop
 [re-frame/trim-v]
 (fn [cofx [identity]]
   (show-profile-desktop identity cofx)))