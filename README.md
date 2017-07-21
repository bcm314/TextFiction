Zplet based Z-Machine interpreter for Android with modern, SMS app style UI.

Find ZPlet here: 
http://sourceforge.net/projects/zplet/

The icon is taken from Gartoon Redux: 
http://gnome-look.org/content/show.php?content=74841

The icons for the commandbuttons are taken from:
http://typicons.com/

External dependencies:
Android support library

Homepage:
http://www.onyxbits.de/textfiction

Google Play:
https://play.google.com/store/apps/details?id=de.onyxbits.textfiction

This is fork by me (BCM) from V1.7pre

Changes:
- bugfix: menu was not shown (KitKat and Jelly Bean, maybe also others)
- bugfix: StatusWindow no longer work after game restore
- show StatusWindow changes as toast
- trim+add space after append-word and remove-word
- remove empty lines at top and bottom of bubbles
- support uppercase German umlaute
- added more icons
- added Sub-Buttoncmds (even multistep)
- implemented storylanguage autodetection
- changed design
. moved bubbles (but done for all themes) closer to the border
. Jason and Atalanta: move "right_bubble" to left side

ToDo:
- decide for default command buttons
- need fixed: GridView (id/list) rows overlapping - make row height fit the tallest item
