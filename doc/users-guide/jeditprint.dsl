<!DOCTYPE style-sheet PUBLIC "-//James Clark//DTD DSSSL Style Sheet//EN" [
<!ENTITY dbstyle PUBLIC "-//Norman Walsh//DOCUMENT DocBook Print Stylesheet//EN"
CDATA DSSSL> ]>

<style-sheet>
<style-specification use="print">
<style-specification-body>

(define %two-side% #t)
(define %section-autolabel% #t)
(define %paper-type% "A4")
(define %admon-graphics% #f)

(declare-characteristic preserve-sdata?
  "UNREGISTERED::James Clark//Characteristic::preserve-sdata?" #f)

(define %visual-acuity% "presbyopic")

; (define %left-margin%
;   4pi)
; (define %top-margin%
;   4pi)
; (define %right-margin%
;   4pi)
; (define %bottom-margin%
;   4pi)
; (define %bf-size%
;   10pt)
;; (define %default-quadding%
;;  'justify)

;; Since we're producing PDF output, we need to use PNG images
;(define %graphic-default-extension% "png")

;; DocBook should have some sort of %img-dir% variable, but for now,
;; a stupid hack

;(define (graphic-file filename)
;   (let ((ext (file-extension filename)))
;      (if (or (not filename)
;              (not %graphic-default-extension%)
;	      (member ext %graphic-extensions%))
;	  filename
;	  (string-append "images/" filename "." %graphic-default-extension%))))

</style-specification-body>
</style-specification>
<external-specification id="print" document="dbstyle">
</style-sheet>
