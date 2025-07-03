on run(volumeName, windowX, windowY, windowWidth, windowHeight, iconSize, backgroundImage, appName, appIconX, appIconY, appFolderX, appFolderY)
	tell application "Finder"
		tell disk volumeName
			open
			set backgroundImage to file backgroundImage
			tell its container window
				set its current view to icon view
				set its toolbar visible to false
				set its statusbar visible to false
				set its bounds to {windowX, windowY, windowX + windowWidth, windowY + windowHeight}
				tell its icon view options
					set its arrangement to not arranged
					set its icon size to iconSize
					set its background picture to backgroundImage
				end tell
				set position of item (appName & ".app") of it to {appIconX, appIconY}
				set position of item "Applications" of it to {appFolderX, appFolderY}
			end tell
			close
			open
			update without registering applications
			delay 2
		end tell
	end tell
end run
