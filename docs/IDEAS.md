# Ideas

A living scratchpad for future directions. Nothing here is committed work — add
freely, prune ruthlessly. When an idea graduates into actual work, move it to an
issue / plan and delete it from here.

Format: one `##` section per theme, one bullet per idea. Add a `→` note if an
idea has a known starting point in the code.

## Motion & polish

- Ripples/indication on custom tappable surfaces (the add-section card, dial
  values) — some custom `pointerInput` handlers bypass Material ripples today.
  → `ui/components/RadialPicker.kt`, `PlannerScreen.kt`
- Animate section add/remove/reorder in the planner list (`animateItem` on
  LazyColumn items) instead of instant appearance.
- Screen-to-screen transitions: shared-axis or fade-through between home,
  planner, and active timer via NavHost transition APIs.
- Animated progress hand-off when a plan starts: morph from the planner's play
  button into the active timer's arc.
- Haptics: light tick when the radial dial crosses a value, confirm buzz on
  selection (like the system time picker).
- Animate the repeat FAB icon swap (Loop ↔ Start) with a rotate/fade rather
  than an instant switch.
- Springy count-in animation on the active timer countdown digits.

## Responsive & adaptive UI

- Landscape / tablet layouts: planner as two panes (sections list + detail);
  active timer with side-by-side arc and controls.
- Respect font-scale and larger touch targets throughout (audit 32dp icon
  buttons).
- Edge-to-edge insets done properly (replace deprecated
  `window.statusBarColor` usage in `ui/theme/Theme.kt`).

## Planner & editing

- Read-mostly section cards: collapse a section to a one-line summary
  ("8 × 30s · 3s break") that expands to edit — calmer list, better overview.
- Duplicate section action (common when building symmetric routines).
- Total plan duration shown in the header, live-updating as values change.
- Drag-to-reorder affordance improvements: elevation + scale while dragging.

## Timer experience

- Optional voice announcements of section names between sections.
- Configurable ping sounds / volume per plan.
- Wear OS or notification-based quick controls.

## Housekeeping seeds

- Wire the picker dialog `title` into `FocusingInputFieldWithPicker` (the
  simple timer screen's pickers currently open untitled).
