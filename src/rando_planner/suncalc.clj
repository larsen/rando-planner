(ns rando-planner.suncalc
  (:require [clj-time.coerce :as cjc]))

;; # Sunset & sunrise calculations

(def epoch-julian-day 2440587.5)

(defn ts->julian-date [ts]
  (+ (/ ts 86400.0)
     epoch-julian-day))

(defn julian-date->ts [j]
  (* (- j epoch-julian-day)
     86400))

(def j2000 2451545)

(defn julian-date->julian-day [jd]
  (Math/ceil (+ (- jd (+ j2000 0.0009))
                (/ 69.184 86400.0))))

(defn mean-solar-time [julian-day lon]
  (- (+ julian-day 0.0009)
     (/ lon 360)))

(defn mean-anomaly [mean-solar-time]
  ;; Values for plan Earth
  (let [m0 357.5291
        m1 0.98560028]
    (mod (+ m0 (* m1 mean-solar-time))
         360)))

(defn equation-of-the-center [mean-anomaly]
  (let [c1 1.9148
        c2 0.0200
        c3 0.0003]
    (+ (* c1 (Math/sin mean-anomaly))
       (* c2 (Math/sin (* 2 mean-anomaly)))
       (* c3 (Math/sin (* 3 mean-anomaly))))))

(defn sunset-sunrise-times [ts lat lon elevation]
  (let [j (ts->julian-date ts)
        ;; Julian day (from Jan 1st, 2000)
        ;; with adjustments
        n (julian-date->julian-day j)

        ;; Solar mean anomaly
        mean-solar-time (mean-solar-time n lon)
        M-deg (mean-anomaly mean-solar-time)
        M-rad (Math/toRadians M-deg)

        ;; Equation of the center
        C-deg (equation-of-the-center M-rad)

        ;; Ecliptic longitude
        perihelion 102.9373
        L-deg (mod (+ M-deg C-deg 180 perihelion) 360)
        L-rad (Math/toRadians L-deg)

        ;; Solar transit
        J-transit (+ j2000
                     mean-solar-time
                     (* 0.0053 (Math/sin M-rad))
                     (* 0.0069 (Math/sin (* 2 L-rad))))

        ;; Declination of the Sun
        obliquity 23.4397
        sin-d (* (Math/sin L-rad)
                 (Math/sin (Math/toRadians obliquity)))
        cos-d (Math/cos (Math/asin sin-d))

        ;; Hour angle

        ;; (
        ;;   sin(
        ;;     radians(-0.833 - 2.076 * sqrt(elevation) / 60.0)
        ;;   )
        ;;   - sin(radians(f)) * sin_d
        ;; )
        ;; / (cos(radians(f)) * cos_d)
        lat-rad (Math/toRadians lat)
        w0-cos (/ (- (Math/sin
                      (Math/toRadians
                       (- 0.833
                          (/ (* 2.076 (Math/sqrt elevation))
                             60.0))))
                     (* (Math/sin lat-rad)
                        sin-d))
                  (* (Math/cos lat-rad)
                     cos-d))
        w0-deg (Math/toDegrees (Math/acos w0-cos))

        j-rise (- J-transit (/ w0-deg 360))
        j-set (+ J-transit (/ w0-deg 360))]
    [(cjc/from-long (long (* 1000 (julian-date->ts j-rise))))
     (cjc/from-long (long (* 1000 (julian-date->ts j-set))))]))

;;    Corresponding ~ to Veneto
;;    [45.485717 11.315627]

(comment (sunset-sunrise-times 1707225141 45.485717 11.315627 0))
