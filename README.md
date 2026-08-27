# Extra Dim

A simple Android app that dims the screen below the system's minimum brightness limit.

## Problem

Android only lets you dim the screen down to a fixed system minimum brightness. For reading in a dark room, or to reduce eye strain at night, that minimum can still be too bright. Extra Dim draws a full-screen translucent black overlay on top of everything, going darker than the OS allows.

## Features

- **Dim below the system minimum** with a brightness slider (full by default; drag down to dim further, all the way to pure black).
- **Quick Settings tile** to toggle dimming on/off with one tap.
- **Extra-darkness option** that also lowers the device's system brightness floor (requires the "modify system settings" permission).
- **Simple, single-purpose UI** — no bloat.

## Quick start

1. Grant **Display over other apps** when prompted (required for the overlay).
2. Use the slider / "Dimming" switch, or tap the Quick Settings tile.
3. First time: open the app and tap **Add to Quick Settings** to install the tile.

## Status

Implemented and building. See [AGENTS.md](AGENTS.md) for architecture, conventions and the development workflow.

## Built With

- Native Android (Kotlin, Jetpack Compose / Material 3)
- DataStore for settings persistence
- A `specialUse` foreground service for the always-on overlay
