---
name: Warm Foodie Culinary System
colors:
  surface: '#fff8f5'
  surface-dim: '#e9d7c8'
  surface-bright: '#fff8f5'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fff1e8'
  surface-container: '#feeadc'
  surface-container-high: '#f8e5d6'
  surface-container-highest: '#f2dfd1'
  on-surface: '#231a11'
  on-surface-variant: '#59413b'
  inverse-surface: '#392e25'
  inverse-on-surface: '#ffeee1'
  outline: '#8c7169'
  outline-variant: '#e0bfb6'
  surface-tint: '#ab350b'
  primary: '#a83208'
  on-primary: '#ffffff'
  primary-container: '#ca4a21'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb59f'
  secondary: '#4c662c'
  on-secondary: '#ffffff'
  secondary-container: '#cbeaa1'
  on-secondary-container: '#506a2f'
  tertiary: '#805200'
  on-tertiary: '#ffffff'
  tertiary-container: '#a16900'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbd1'
  primary-fixed-dim: '#ffb59f'
  on-primary-fixed: '#3a0a00'
  on-primary-fixed-variant: '#862300'
  secondary-fixed: '#ceeda4'
  secondary-fixed-dim: '#b2d18a'
  on-secondary-fixed: '#102000'
  on-secondary-fixed-variant: '#354e16'
  tertiary-fixed: '#ffddb5'
  tertiary-fixed-dim: '#ffb956'
  on-tertiary-fixed: '#2a1800'
  on-tertiary-fixed-variant: '#633f00'
  background: '#fff8f5'
  on-background: '#231a11'
  surface-variant: '#f2dfd1'
typography:
  display-lg:
    fontFamily: Manrope
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Manrope
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Manrope
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0em
  headline-sm:
    fontFamily: Manrope
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.01em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.015em
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.03em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system expresses a "Warm Foodie" aesthetic built upon Material 3 foundations, tailored specifically for mobile-first culinary exploration. It channels the visceral comfort of a home kitchen, the tactile pleasure of raw ingredients, and the modern precision of an AI culinary companion.

### Personality & Tone
- **Inviting & Savory:** Rich, sun-baked kitchen tones (paprika, fresh olive, raw honey) replace clinical digital neutrals with organic warmth.
- **Nourishing & Intuitive:** Interfaces feel soft, tactile, and responsive—like well-seasoned cookware and handwritten recipe cards.
- **Modern Assistant:** Clean structural utility ensures complex recipe generation, pantry audits, and meal plans feel effortless rather than technical.

### Visual Style
The system combines **Tactile Material Minimalism** with **Warm Organicism**:
- Deep corner radii (up to 24px on recipe containers) impart a soft, dough-like approachability.
- Highly diffused ambient illumination replaces harsh drop shadows.
- Cream and warm charcoal base layers avoid sterile white-on-black contrast, sustaining an inviting culinary ambiance across both daylight cooking and ambient evening kitchen sessions.

## Colors

The palette is derived from aromatic spices, cold-pressed herbs, and warm dairy stocks, built according to Material 3 semantic roles.

### Light Mode (Default)
- **Primary (`#D9552B` - Paprika / Terracotta):** The core active accent. Used for key CTAs (e.g., "Start Cooking", "Generate Recipe"), primary active icons, and focal interactive states.
  - *On Primary (`#FFFFFF`):* High-legibility text and glyphs over primary fills.
  - *Primary Container (`#FFDCCF` - Soft Peach):* Tonal surface for highlighted tags, active step banners, and ambient recipe hero backdrops.
  - *On Primary Container (`#431B0E`):* Rich burnt umber for maximum contrast over soft peach containers.
- **Secondary (`#5F7A3D` - Olive Herb):** Herbal, grounding tone for fresh ingredient tags, dietary badges (Vegan, Keto), and pantry stocking states.
  - *Secondary Container (`#E2EBCE`):* Soft herbal tint for recipe taxonomy and filter selections.
- **Tertiary (`#F2A93B` - Warm Honey):** Playful amber for ratings, cook timers, AI tips, and pairing notes.
- **Background (`#FFF8F0` - Warm Cream):** Canvas base that removes sterile blue light and replicates natural kitchen surfaces.
- **Surface (`#FFFFFF` - Pure Bone):** Elevated surfaces, bottom sheets, and elevated card modules.
- **On Surface (`#2B2118` - Warm Charcoal):** Deep espresso/charcoal for effortless typographic contrast and readability.
- **Outline (`#E8DDD3` - Toasted Oat):** Soft low-contrast dividers, pill container edges, and inactive borders.
- **Success (`#4C7A34`):** Earthy garden green for completed steps and pantry confirmations.
- **Error (`#B3261E`):** Classic deep scarlet for missing ingredients and system notices.

### Dark Mode Semantic Tokens
- **Background:** `#241C16` (Deep Roasted Earth)
- **Surface:** `#312720` (Dark Cocoa Hearth)
- **Surface Container:** `#3D322A` (Elevated Tile Fill)
- **On Surface:** `#F5EFEB` (Warm Flour)
- **Primary:** `#FF8B66` (Luminous Paprika)
- **Outline:** `#4A3E34` (Muted Cacao)

## Typography

The typographic hierarchy establishes distinct roles: **Manrope** provides expressive geometric friendliness for recipe names, dish collections, and AI prompts, while **Inter** ensures crisp, uncompromised legibility for granular ingredient quantities, prep instructions, and nutritional data.

### Hierarchy & Usage Rules
- **Manrope Display & Headlines (`SemiBold 600` & `Bold 700`):** Used for recipe titles, section headers, step milestones, and modal welcomes. Tighter negative letter spacing on sizes 24px and above gives an editorial, high-end cookbook quality.
- **Inter Body (`Regular 400`):** Sized generously with a minimum 1.4x line-height ratio to ensure easy scanning while cooking with a phone resting on a kitchen counter.
- **Inter Labels (`Medium 500` & `SemiBold 600`):** Used across macro metric tags (e.g., "Prep: 15m"), interactive pill chips, dietary flags, and button labels.

## Layout & Spacing

Designed primarily for portrait mobile devices (specifically targeted around Google Pixel frames, width: 412pt), the layout adopts a 4-column fluid mobile grid with dedicated safe zones for navigation and cooking action bars.

### Grid & Canvas Specs
- **Columns:** 4-column fluid grid.
- **Margins:** `16px` (`1rem`) outer canvas padding on phone portrait; escalates to `24px` on compact tablet breakpoints.
- **Gutter:** `16px` (`1rem`) between vertical and horizontal grid tracks.
- **Vertical Spacing Rhythm:** Built strictly around a 4px/8px modular scale:
  - `space-xs` (4px): Inset badge padding, text-to-icon gaps.
  - `space-sm` (8px): Spacing between grouped elements (ingredient name and measure).
  - `space-md` (16px): Standard gap between form fields, list rows, and card interior borders.
  - `space-lg` (24px): Separation between distinct recipe sections (Ingredients vs. Preparation).
  - `space-xl` (32px): Major chapter breaks, screen title transitions, and sheet headers.

### Mobile Reflow & Ergonomics
All primary cooking CTAs (e.g., "Next Step", "Voice AI Assistant") reside in a sticky bottom navigation thumb zone with a minimum height of 56px and 16px lateral clearance. Horizontal carousels (e.g., "Quick Pantry Dinners") bleed full width into the right margin using negative margin offsets with 16px interior padding.

## Elevation & Depth

Visual depth follows a soft culinary metaphor, simulating physical plates and parchment layers resting gently on a kitchen countertop. Hard drop shadows are strictly prohibited.

### Elevation Levels
- **Level 0 (Base Canvas):** Warm cream background (`#FFF8F0`). Flat, non-elevated.
- **Level 1 (Resting Cards & Lists):** Pure Bone Surface (`#FFFFFF`) sitting on background canvas with an ambient tinted shadow:
  - *Shadow formula:* `0px 4px 16px rgba(43, 33, 24, 0.06)` (tinted with warm charcoal, never neutral grey).
  - Paired with an ultra-fine border: `1px solid #E8DDD3`.
- **Level 2 (Active Controls & Sticky Modules):** Active step cards, floating bottom action bars, and cooking timers:
  - *Shadow formula:* `0px 8px 24px rgba(43, 33, 24, 0.08)`.
- **Level 3 (Modals, AI Cooking Drawers & Bottom Sheets):**
  - *Shadow formula:* `0px -8px 32px rgba(43, 33, 24, 0.12)`.
  - Backdrop overlay: `rgba(43, 33, 24, 0.40)` with a subtle `backdrop-filter: blur(4px)`.

## Shapes

The shape system expresses a soft, dough-like, and organic posture while preserving structural clarity for dense culinary data.

### Structural Radius Scale
- **Pills / Full Round (`999px`):** All interactive buttons, action chips, category filters, calorie indicators, and timer tags.
- **Inputs & Controls (`16px`):** Search boxes, ingredient customizers, AI prompt textareas, and step input fields.
- **Cards & Modules (`24px`):** Main recipe preview cards, nutritional highlight containers, meal calendar slots, and ingredient group enclosures.
- **Bottom Sheets & Floating Panels (`28px` top radii):** Modal surfaces entering from the viewport edges.

## Components

### Buttons
- **Primary CTA:** Full-radius pill (`999px`), background `#D9552B`, text `#FFFFFF` (`label-lg`, 600 weight). Minimum tap target: 52px height. Resting state has subtle ambient shadow; pressed state slightly scales to `0.98` with darker terracotta tint (`#C2451D`).
- **Secondary (Tonal) Button:** Pill (`999px`), background `#FFDCCF`, text `#431B0E`. Used for alternative recipe variations and pantry swaps.
- **Outlined / Tertiary Button:** Pill (`999px`), border `1.5px solid #E8DDD3`, background `transparent`, text `#2B2118`.

### Chips & Filters
- **Dietary & Cuisine Chips:** Height 36px, full pill (`999px`).
  - *Inactive:* Background `#FFFFFF`, border `1px solid #E8DDD3`, text `#2B2118`.
  - *Active:* Background `#5F7A3D`, text `#FFFFFF`, border `none`.
  - *Primary Variant:* Active background `#D9552B`, text `#FFFFFF`.

### Recipe & Ingredient Cards
- **Card Enclosure:** Radius `24px`, surface `#FFFFFF`, border `1px solid #E8DDD3`, ambient shadow `0 4px 16px rgba(43,33,24,0.06)`. Internal padding `16px`.
- **Media Presentation:** Recipe hero photography takes a top border radius of `20px` (or full bleed `24px` when inset) with an aspect ratio of 16:11.
- **Ingredient Item Row:** Height 48px, horizontal layout with a circular check toggle on the left, ingredient name in `body-md`, and quantity right-aligned in `label-md` with tertiary honey accent.

### Input Fields & AI Prompts
- **Text Inputs:** Height 56px, radius `16px`, background `#FFFFFF`, border `1px solid #E8DDD3`, padding horizontal `16px`. Text typed in `body-md` (`#2B2118`), placeholder in `#8C7E74`.
- **Focus State:** Border shifts to `2px solid #D9552B` with zero harsh glow, retaining ambient card softness.
- **AI Prompt Bar:** Elevated floating container with radius `999px`, housing a microphone icon, natural text placeholder ("Ask what to make with tomatoes & chicken..."), and a circular primary action arrow.

### Checkboxes & Step Radios
- **Step Checkbox:** Size 24px, radius `8px` (soft pill square). Unchecked: border `2px solid #E8DDD3`. Checked: fill `#5F7A3D` with pure bone check icon.
- **Cooking Mode Step Check:** Ticking a step strikethroughs the text in `body-md` and dims the ingredient row opacity to `0.45` to maintain cooking focus.

### Additional Culinary Components
- **Cooking Timer Floating Badge:** Pill container (`999px`) with `#F2A93B` tertiary container background, active countdown clock in `label-md`, and gentle pulse animation.
- **AI Chef Suggestion Card:** Radius `24px`, dual background gradient (`linear-gradient(135deg, #FFF8F0 0%, #FFDCCF 100%)`), outlined in `#D9552B` at 20% opacity with a distinctive starburst glyph.