<!DOCTYPE style-sheet PUBLIC "-//James Clark//DTD DSSSL Style Sheet//EN" [
<!ENTITY dbstyle PUBLIC "-//Norman Walsh//DOCUMENT DocBook HTML Stylesheet//EN"
CDATA DSSSL> ]>

<style-sheet>
<style-specification use="html">
<style-specification-body>

(define %html-ext% ".html")
(define %shade-verbatim% #t)
(define %root-filename% "index")
(define %use-id-as-filename% #t)
(define %body-attr% 
  (list
   (list "BGCOLOR" "#FFFFFF")))
(define %admon-graphics% #f)
(define %spacing-paras% #f)

;; make these pretty
(element guibutton ($bold-seq$))
(element guimenu ($bold-seq$))
(element guimenuitem ($bold-seq$))
(element guisubmenu ($bold-seq$))

;; wordaround for stupid Swing HTML limitation - it can't display
;; DocBook's quotes properly

(element quote
   (make sequence
      (literal "\"")
      (process-children)
      (literal "\"")))

;; again, Swing HTML doesn't support tables properly

(define %gentext-nav-use-tables% #f)

;; DocBook should have some sort of %img-dir% variable, but for now,
;; a stupid hack

;;(define (graphic-file filename)
;;   (let ((ext (file-extension filename)))
;;      (if (or (not filename)
;;              (not %graphic-default-extension%)
;;	      (member ext %graphic-extensions%))
;;	  filename
;;	  (string-append "../images/" filename "." %graphic-default-extension%))))

</style-specification-body>
</style-specification>
<external-specification id="html" document="dbstyle">
</style-sheet>
