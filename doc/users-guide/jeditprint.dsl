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

(element funcsynopsis (process-children))

</style-specification-body>
</style-specification>
<external-specification id="print" document="dbstyle">
</style-sheet>
