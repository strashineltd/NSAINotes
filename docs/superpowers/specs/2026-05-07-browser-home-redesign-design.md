# Browser Home Page Redesign — Minimal Style

**Date**: 2026-05-07
**Status**: approved

## Problem

Current browser home page (264 lines) has clutter: 8-column shortcut grid with add/delete, bookmarks section, custom URL row always visible, and verbose SEO-style subtitle text. Visual style is dated.

## Design

Single HTML file, zero external dependencies. Centered layout with generous top spacing. Removed features: shortcut add/delete UI, bookmarks section (bookmarks now managed by Kotlin-side WebBrowser). Kept: search engine switcher with localStorage persistence, search history (last 5).

### Layout (top to bottom, centered)

1. Logo icon (44px, gradient blue-purple) + app name + tagline
2. Search input (wide, rounded, border glow on focus)
3. Engine chip row (Google / 百度 / 必应 / 自定义)
4. 5 fixed shortcut icons in a single row
5. Recent search history (shows when >0, max 5, with clear button)

### Key changes

| Before | After |
|--------|-------|
| 264 lines | ~150 lines |
| 8-column shortcut grid | Single row of 5 fixed icons |
| "SEO subtitle" description text | Minimal icon + name |
| Bookmarks section | Removed |
| Add/delete shortcut UI | Removed |
| Custom URL row always hidden | Shown only when "自定义" selected |
| 8 default shortcuts + localStorage CRUD | 5 fixed shortcuts, no edit |

### Tech constraints

- Pure HTML+CSS+JS, inline SVG icons
- `prefers-color-scheme` dark mode via CSS variables
- No external fonts, no icon libraries
- Search engine preference + history via localStorage

## Files

- Modify: `app/src/main/assets/browser_home.html`
