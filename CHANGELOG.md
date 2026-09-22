# Changelog

All notable user-visible and security-relevant changes are recorded here.

## 0.6.1 — 2026-09-21

- Replace the old OpenFrame Albums launcher banner with AlbumFrame TV branding.
- Keep the package name and signing identity unchanged for an in-place update from 0.6.0.

## 0.6.0 — 2026-09-21

- Rename the app from OpenFrame Albums to AlbumFrame TV.
- Change the permanent application ID and Java namespace to `dk.femto.albumframe`, based on the publisher-owned `femto.dk` domain.
- Publish the canonical source repository at `https://github.com/olecphdk/albumframe-tv`.
- This package installs separately from all 0.5.x test builds and requires a new Flickr login.

## 0.5.2 — 2026-09-21

- Explicitly disable Android backup and cleartext network traffic.
- Restrict local setup to TLS 1.2 or newer.
- Add browser isolation, cache prevention and permissions-policy headers to local setup.
- Compare the OAuth callback token in constant time.
- Add in-app privacy information and required Flickr API attribution.
- Add privacy, security-design and release documentation.

## 0.5.1

- Fix Flickr album cover fallback URLs.
- Keep the slideshow information bar hidden during automatic photo changes.

## 0.5.0

- Add album cover thumbnails, slideshow prefetching and smoother transitions.
- Add a high-contrast TV focus style and Danish/English language selection.
- Add long-press background selection and consolidated settings.

Earlier reconstructed releases are available as Git tags `v0.3.0`, `v0.3.1` and `v0.4.0`.
