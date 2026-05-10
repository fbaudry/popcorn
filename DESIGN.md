# Neon Velocity Design System

### 1. Overview & Creative North Star
**Creative North Star: "Cinematic Immersion"**
The Neon Velocity system is designed to transform the interface into a high-end editorial experience. It moves away from the sterile "SaaS" look toward a digital canvas that feels like a cinematic production. By using deep blacks, vibrant cyan accents, and intentional asymmetry, the system prioritizes visual depth and atmospheric storytelling over rigid, boxy layouts.

### 2. Colors
The palette is built upon a Foundation of "Obsidian Depth" (#0C0E12) with high-energy "Electric Cyan" accents.

- **Primary & Tonal Spot:** The primary color (Cyan) is used for high-intent actions and critical status indicators (like "Live" or progress bars).
- **The "No-Line" Rule:** Sectioning is strictly achieved through background shifts (e.g., transitioning from `surface` to `surface_container_low`) or the use of large-scale atmospheric imagery. Solid 1px borders are prohibited for layout division.
- **Surface Hierarchy & Nesting:** Use `surface_container` levels to define card depth. A background in `surface` may host a `surface_container_low` bento grid, which in turn contains `surface_container_highest` glass-morphed interactive elements.
- **Glass & Gradient Rule:** Navigation and persistent controls (like the floating player) must use a 70-80% opacity fill with a minimum 20px backdrop blur.
- **Signature Textures:** Main CTAs must utilize a diagonal linear gradient from `primary` to `primary_container` to simulate a "glowing" effect.

### 3. Typography
The typography system uses a dual-font approach to balance technical precision with editorial impact.

- **Display & Headlines:** *Plus Jakarta Sans*. Used for high-impact titles. The scale is aggressive, utilizing `3.75rem` (60px) and `1.875rem` (30px) for section headers. Tracking should be tightened (-0.05em) for Display styles to create a "poster" feel.
- **Body & Labels:** *Manrope*. A highly legible sans-serif used for descriptions and metadata.
- **Typography Rhythm:**
  - **Hero Heading:** 60px / 1.0 line-height / Black weight.
  - **Section Title:** 30px / 1.2 line-height / Bold.
  - **Card Title:** 20px (1.25rem).
  - **Body Text:** 18px (1.125rem).
  - **Metadata/Labels:** 14px (0.875rem) or 10px for uppercase "Eyebrow" tags.

### 4. Elevation & Depth
Depth is conveyed through light and transparency rather than physical shadows.

- **The Layering Principle:** Stack `surface-container` tiers. The deeper the element (further back), the darker the surface.
- **Ambient Shadows:** Only used for floating objects. We utilize `shadow-xl` and `shadow-2xl` values which are highly diffused (large blur radius) and colored with a hint of the `primary` or `blue-900` hue at low (10-20%) opacity.
- **The "Ghost Border":** For buttons that aren't primary, use an `outline_variant` border at 20% opacity.
- **Glassmorphism:** The floating player and sidebars use `backdrop-filter: blur(20px)` combined with a subtle border to create a "layered glass" effect.

### 5. Components
- **Buttons:** Primary buttons are large (16px+ padding) with rounded corners (`0.75rem`) and a subtle cyan drop-shadow.
- **Bento Cards:** Use `surface_container_low` with hidden overflows. Images inside cards should have a 700ms scale-up transition on hover.
- **Progress Bars:** Minimal height (4px). Background is `surface_container_highest`, foreground is `primary`.
- **Navigation:** Side navigation uses a vertical active-state bar (4px width) in `primary` color to indicate the current section.
- **Search Input:** Pill-shaped (`rounded-full`) or highly rounded (`12px`) with no border, using `surface_container_lowest` for maximum contrast against the sidebar.

### 6. Do's and Don'ts
**Do:**
- Use heavy-weight typography for branding and headings.
- Use semi-transparent gradients to fade images into the background.
- Apply `backdrop-blur` to any element that floats over content.
- Use high-contrast "Streaming Now" pill tags to draw attention.

**Don't:**
- Use sharp 90-degree corners; maintain a consistent `roundedness` level.
- Use pure white text for body copy; use `on_surface_variant` (#AAABB0) for descriptions to reduce eye strain and maintain the dark aesthetic.
- Use traditional "Modal" boxes; use glass-morphed overlays or full-screen atmospheric transitions instead.