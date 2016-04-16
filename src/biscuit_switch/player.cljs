(ns biscuit-switch.player
  (:require [biscuit-switch.assets :as assets]
            [biscuit-switch.game :as game]

            [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.events :as e]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.utils.math :as math]
            [infinitelives.utils.events :as events]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.boid :as boid]

            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [infinitelives.pixi.macros :as m]))

(def p-boid
  {:mass 1.0
   :pos (vec2/vec2 0 120)
   :vel (vec2/vec2 0 0)
   :max-force 5.0
   :max-speed 7.0})

(defn left? []
  (events/is-pressed? :left))

(defn right? []
  (events/is-pressed? :right))

(defn up? []
  (events/is-pressed? :up))

(defn down? []
  (events/is-pressed? :down))

(defn drag [bd]
  (assoc bd :vel (vec2/scale (:vel bd)
                             (if (or (left?) (right?) (up?) (down?))
                               ;; when walking drag is lower
                               0.95
                               0.90)))
  )

(defn limit-y
  "constrain boid to walkable area"
  [{:keys [pos vel] :as bd} cmp limit]
  (let [[x y] (vec2/as-vector pos)
        [vx vy] (vec2/as-vector vel)]
    (if (cmp y limit)
      (assoc bd
             :vel (vec2/vec2 vx 0)
             :pos (vec2/vec2 x limit))
      bd))
)

(defn limit-x
  "constrain boid to walkable area"
  [{:keys [pos vel] :as bd} cmp limit]
  (let [[x y] (vec2/as-vector pos)
        [vx vy] (vec2/as-vector vel)]

    ;; limit bottom
    (if (cmp x limit)
      (assoc bd
             :vel (vec2/vec2 0 vy)
             :pos (vec2/vec2 limit y))
      bd))
)

(defn player [canvas]
  (go
    (m/with-sprite canvas :player
      [
       player (s/make-sprite :player-stand-left
                             :scale 4
                             :x 0 :y 120)
       ]
      (loop [c 20000
             b p-boid
             ]
        (if (or (left?) (right?) (up?) (down?))
          (let [fnum (int (/ c 10))]
            (if (odd? fnum)
              (s/set-texture! player :player-stride-left)
              (s/set-texture! player :player-stand-left-2)
              )
            )
          (if (< (vec2/magnitude (:vel b)) 0.01)
            (let [fnum (int (/ c 30))]
              (if (odd? fnum)
                (s/set-texture! player :player-stand-left)
                (s/set-texture! player :player-stand-left-2)
                ))

            (s/set-texture! player :player-stand-left)


            ))

        ;; make player sprite reflect boid
        (s/set-pos! player (:pos b))
        (let [face-left? (neg? (aget (:vel b) 0))]
          (if face-left?
            (s/set-scale! player 4 4)
            (s/set-scale! player -4 4)))


        (<! (e/next-frame))

        (recur (dec c)

               (-> b
                   (boid/apply-steering
                    (vec2/vec2
                     (if (left?) -0.2 (if (right?) 0.2 0.0))
                     (if (up?) -0.2 (if (down?) 0.2 0.0)))
                    )
                   drag
                   (limit-y < 0)
                   (limit-y > 200)
                   (limit-x < -450)
                   (limit-x > 450)))))))
