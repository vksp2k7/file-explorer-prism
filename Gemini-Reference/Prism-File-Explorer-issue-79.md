# More UI improvements

**Issue:** [#79](https://github.com/techflyers/Prism-File-Explorer/issues/79)  
**Status:** Open  
**Author:** [@vertigo220](https://github.com/vertigo220)  
**Opened:** October 5, 2026

## Description

1. Add free space to the home screen.
2. Allow rearranging sections in the sidebar.
3. Allow hiding individual items in the sidebar. One possible approach is to add a button, perhaps an eye icon, next to the section-collapse button. This would toggle an edit mode that shows all items in the section. Each item would have a similar control to toggle whether it is normally visible outside edit mode. The icon could indicate the visibility state (an eye or an eye with a diagonal line), or hidden items could be greyed out.
4. Add a long-press context menu for tabs with prominent functions such as duplicate/clone, reopen closed tabs, and pin. The author later discovered that a context menu exists, but it appears on tap rather than long-press. It should appear with either gesture, or be configurable. Duplicate and pin options are still missing. Pinning would require deciding how pinned tabs should behave, likely following conventions used by browsers and other apps.
5. Reposition the “new task has been added” popup so that it does not cover the tabs. The popup currently prevents switching tabs to paste copied data until it disappears.
6. Implement the additional checksum functionality mentioned in [issue #32](https://github.com/techflyers/Prism-File-Explorer/issues/32).

## Discussion

### October 5, 2026

The author suggested a possible “zone mode,” inspired by [issue #72](https://github.com/techflyers/Prism-File-Explorer/issues/72). A toolbar button could activate columns configured by the user. Each column would have a slightly different light-grey shade and an action icon. Tapping a file in a column would perform that column’s action—for example, show file information, copy the file, or open the rename dialog. The author was unsure whether the feature would be useful enough to implement and asked what others thought.

### October 7, 2026

The author added two requests:

1. Automatically hide the toolbar while scrolling, with separate settings for portrait and landscape orientations.
2. In landscape orientation, move the tabs to the available space beside the breadcrumb bar. This would let the tabs display normally and provide more room for the breadcrumb bar.

![Landscape layout reference image](https://private-user-images.githubusercontent.com/12024557/667843837-ad14f9eb-3d90-4617-87c0-044b6e78e6db.jpg)

The author also reported three issues:

1. Swiping from the left edge still switches tabs instead of opening the sidebar. The tab-switch gesture should be limited to swipes that do not start at the very edge.
2. Long-pressing a recent file on the home screen opens its location, but pressing Back does not return to the home screen. Instead, it behaves like the Up-directory button and requires navigating back through every directory. Back should return to the previous location. On the first such attempt, several recent files also disappeared from the Recent section and returned only after navigating away and back.
3. Tab positions are still buggy. When many tabs extend off-screen and the current tab is at the far left or right, switching in or out of the home screen pushes the current tab off-screen. The author believed this had previously been fixed, though they were not certain it had been verified.

### October 8, 2026

The author added three tab-management requests:

1. Ask for confirmation before using **Close other tabs**, since it can be selected accidentally instead of another option.
2. When reopening a closed tab, restore it to its previous position in the tab order.
3. Provide a list of recently closed tabs so a specific tab can be selected for reopening. This would avoid having to reopen tabs in reverse order and then close the unwanted ones.

## Source

[Original GitHub issue #79](https://github.com/techflyers/Prism-File-Explorer/issues/79)
