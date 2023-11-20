(ns seerc.core
  (:require
   [clojure.edn :as edn]
   [clojure.string :as stri]
   [seesaw.core :as ss]
   [seesaw.event :as se])
  (:gen-class)
  (:import
   [java.awt.event KeyEvent]
   [java.io IOException]
   [nl.vv32.rcon Rcon]))

; initialize rcon
(def a-rcon (atom ()))

; default configuration
(def a-conf (atom {:host "127.0.0.1" :port 25575 :pwd "admin"}))

; history command stack
(def a-history (atom []))

(def a-history-pointer (atom 0))

(defn conv-encoding [raw]
  ;; (let [bytes (.getBytes raw StandardCharsets/UTF_8)]
  ;;   (String. bytes StandardCharsets/UTF_8))
  raw)

;; WARN inpure func
(defn offset-with-lim [x offset]
  (let [t (+ x offset)
        a (max t 0)]
    (min (dec (count @a-history)) a)))

(def vl-content
  (let [tf-con (ss/text :editable? false :border [10 "Console"] :font "Sans-Serif-18" :multi-line? true)
        tf-input (ss/text :bounds [:* :* 10 :*] :font "monospaced-18")

        scl (ss/scrollable tf-con :vscroll :always)

        btn-submit (ss/button :size [128 :by 40] :text "Submit" :id :submit-btn :enabled? false)

        comb-subm (ss/horizontal-panel :items [tf-input btn-submit] :border 5)]

    (let [act-submit (fn [_]
                       (let [cmd (ss/config tf-input :text)
                             raw-rt (.sendCommand @a-rcon cmd)
                             rt (conv-encoding raw-rt)]
                         (ss/config! tf-con :text (str (ss/config tf-con :text) rt)) ; append text-field
                         (println (str "[INFO] " (stri/trim-newline rt)))
                         (when-not (stri/blank? cmd)
                           (ss/config! tf-input :text "") ; clear input text-field
                           (swap! a-history conj cmd) ; append the history
                           (swap! a-history-pointer inc) ; inc the index pointer
                           )))]
      (se/listen btn-submit
                 :action-performed act-submit)
      (se/listen tf-input
                 :action-performed act-submit
                 :key-released (fn [e]
                                 (let [k (.getKeyCode e)]
                                   (cond
                                     (= 0 (count @a-history)) nil ; do nothing if no command history
                                     (= k (KeyEvent/VK_UP))
                                     (do
                                       (swap! a-history-pointer offset-with-lim -1)
                                       (ss/config! tf-input :text (nth @a-history @a-history-pointer ""))) ; history query back
                                     (= k (KeyEvent/VK_DOWN))
                                     (do
                                       (swap! a-history-pointer offset-with-lim 1)
                                       (ss/config! tf-input :text (nth @a-history @a-history-pointer ""))) ; history query forth
                                     )))))
    (ss/border-panel :vgap 3
                     :center scl
                     :south comb-subm)))

(defn act-conn [frame]
  (let [host (@a-conf :host)
        port (@a-conf :port)]
    (try
      (reset! a-rcon (Rcon/open host port))

      (if (.authenticate @a-rcon (str (@a-conf :pwd)))
        (ss/config! (ss/select (ss/to-root frame) [:#submit-btn]) :enabled? true)
        (throw (IllegalStateException. "Wrong password")))

      (catch IOException ioex (do (println (str "[ERROR] cannot connect remote: " (.getMessage ioex)))
                                  (System/exit -1)))
      (catch IllegalStateException isex (do (println (str "[ERROR] cannot authenticate: " (.getMessage isex)))
                                            (System/exit -2))))))

(defn show-frame []
  (let [fr (ss/frame :title "See RCON"
                     :minimum-size [800 :by 600]
                     :on-close :dispose)]
    (.start (Thread. (fn [] (act-conn fr))))
    (ss/config! fr :content vl-content)
    (ss/pack! fr)
    (ss/show! fr)
    (println (str "[INFO] connected " (merge @a-conf {:pwd "******"})))))

(defn -main [& args]
  ; try use utf-8 encoding
  (System/setProperty "file.encoding" "UTF-8")

  ; read launch args
  (swap! a-conf merge (edn/read-string (format "{%s}" (stri/join " " args))))

  ; draw main window
  (show-frame))
