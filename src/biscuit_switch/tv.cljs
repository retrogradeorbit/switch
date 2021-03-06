(ns biscuit-switch.tv
  (:require [biscuit-switch.assets :as assets]
            [biscuit-switch.game :as game]
            [biscuit-switch.rising :as rising]

            [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.events :as e]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.pixelfont :as pf]
            [infinitelives.utils.math :as math]
            [infinitelives.utils.events :as events]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.boid :as boid]


            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [infinitelives.pixi.macros :as m]
                   [infinitelives.pixi.pixelfont :as pf]))


(defonce state
  (atom {:shape :any
         :number 10
         :level 1}))


(defn reset []
  (reset! state {:shape :any
                 :number 10
                 :level 1}))

(def wait-min 1000)
(def wait-max 2000)

(def levels
  [
   [:triangle 5]

   [:level-up]

   [:square 5]

   [:level-up]

   [:circle 5]

   [:level-up]

   [:triangle 10]
   [:square 10]
   [:circle 10]

   [:level-up]

   [:triangle 10]
   [:circle 10]
   [:triangle 10]
   [:square 10]

   [:level-up]

   [:any 5]
   [:triangle 10]
   [:square 5]
   [:circle 10]
   [:square 10]

   [:level-up]

   [:random 15]
   [:random 15]
   [:random 15]
   [:random 15]
   [:random 15]
   [:random 15]

   [:level-up]

   [:random 20]
   [:random 20]
   [:random 20]
   [:random 20]
   [:random 20]
   [:random 20]
   [:random 20]
   [:random 20]
   [:random 20]

   [:level-up]

   [:random 40]
   [:random 20]
   [:random 10]
   [:random 20]
   [:random 30]
   [:random 10]
   [:random 5]
   [:random 20]
   [:random 10]

   [:level-up]

   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]

   [:level-up]

   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]

[:level-up]

   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]

[:level-up]

   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]

[:level-up]

   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]

   [:level-up]

   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]
   [:random 10]


   ])

(defn alter [shape number]
  (swap! state assoc :shape shape :number number))

(defn one-made []
  (swap! state update :number dec))


(defn level-up [canvas]
  (.log js/console "Level Up!")
  (sound/play-sound :level-up 1.0 false)
  (swap! state update :level inc)
  (swap! biscuit-switch.dough/state update :speed + 0.1)
  (swap! biscuit-switch.roller/state update :interval - 10)
  (rising/growing-text canvas "LEVEL UP!" 10
                        (vec2/vec2 0 0)
                        (vec2/vec2 0 -10)
                        60
                        1.05
                        0x80ffff))

(defn tv-number-thread [canvas]
  (go
    (loop []
      (when-let [amount (:number @state)]
        (m/with-sprite canvas :ui
          [money (pf/make-text :font (str amount)
                               :scale 3
                               :x 280
                               :y -275
                               :visible true)]

          (while (= amount (:number @state))
            (<! (e/next-frame)))))

      (<! (e/next-frame))
      (recur))))

(defn tv-control-thread [canvas]
  (go
    ;; start
    (loop [[[type number] & rem] levels]
      (if (= :level-up type)
        (level-up canvas)

        (let [type
              (if (= :random type)
                (rand-nth [:triangle :circle :square :any])
                type)]
          (alter type number)

          ;; wait until done
          (while (pos? (:number @state))
            (<! (e/next-frame)))))

      (when rem
        (recur rem))
      ))
)

(defn tv-thread [canvas]
  (go
    (m/with-sprite canvas :machines
      [tv (s/make-sprite :tv :scale 4 :x 300 :y -305)
       triangle (s/make-sprite :tv-triangle :scale 4 :x 260 :y -275 :visible false)
       square (s/make-sprite :tv-square :scale 4 :x 260 :y -275 :visible false)
       circle (s/make-sprite :tv-circle :scale 4 :x 260 :y -275 :visible false)
       ]
      (tv-number-thread canvas)
      (tv-control-thread canvas)
      (loop []
        (let [[tri sq circ] (case (:shape @state)
                              :triangle
                              [true false false]

                              :square
                              [false true false]

                              :circle
                              [false false true]

                              :any
                              [false false false])]
          (s/set-visible! triangle tri)
          (s/set-visible! square sq)
          (s/set-visible! circle circ)
          )

        (<! (e/next-frame))
        (recur)))))
