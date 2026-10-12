# Styles Architecture

Organized by component and responsibility for easier maintenance.

## Structure

### Core
- **variables.css** - Design tokens (colors, fonts, spacing)
- **base.css** - Global reset and base styles
- **layout.css** - Main app grid with responsive breakpoints

### Components
- **topbar.css** - Top bar (search, sync pill, avatar)
- **rail.css** - Left icon navigation
- **sidebar.css** - Filters and status sidebar
- **queue.css** - Ticket list and detail row
- **detail.css** - Main right panel
- **detail-aside.css** - Side panel with metadata

### Elements
- **badges.css** - Status pills, priority, label chips
- **buttons.css** - Button styles and variants
- **activity.css** - Activity feed and composer
- **utilities.css** - Toast, tweaks, dropdown, icons

## Breakpoints

```css
1024px  → Tablet: reduced grid
768px   → Mobile: hides the sidebar
480px   → Small mobile: hides the rail/queue
```

## Conventions

- BEM classes: `.component-element--modifier`
- CSS variables for everything (colors, spacing, shadows)
- Media queries inline in each file
- One responsibility per file
