# AlbumFrame TV 0.7.0

Open-source Android TV app for showing Flickr albums, including private photos authorized by the user's account. Package `dk.femto.albumframe`; GPL-3.0-or-later. No tracking, ads or third-party runtime libraries.

Version 0.7.0 adds photo ordering and improved album navigation. The permanent application ID remains `dk.femto.albumframe`; this version installs over 0.6.x without another Flickr login. Builds before 0.6.0 have a different application ID and install separately.

Canonical source: <https://github.com/olecphdk/albumframe-tv>

## Use

1. Install the APK on your Google TV Streamer and open **AlbumFrame TV**.
2. Open **Indstillinger → Forbind Flickr**, scan the QR code with a phone on the same local network, and complete setup. Verify the address matches the TV before continuing through the local certificate warning.
3. Enter your Flickr API key and secret on the phone, then authorize read access at Flickr. The temporary HTTPS server runs on the TV and closes after setup or 10 minutes. No permanent external service or embedded API credentials.
4. Choose **Vælg album** and an album. Photos display directly inside the app and the app keeps the screen awake during the slideshow.

Slideshows start automatically. Up opens interval selection: 1, 2, 4, 8, 16 or 32 seconds (default 4); this preference is saved. Left/right changes image, OK pauses/resumes, dedicated media Play/Pause keys are supported, and Back returns to albums with the previous album focused. The next photo is prefetched in memory, while the current photo stays visible. New photos dissolve with eased timing; the default transition is 2.5 seconds and can be changed to 0.7 or 1.4 seconds in Settings. The display interval starts after the transition completes. The information bar hides after four seconds and returns when a slideshow key is pressed. Album position is saved, so reopening an album resumes at its last photo. Album rows show Flickr cover thumbnails. Key repeats are consumed; one completed button press produces one action. Albums have 50 entries per page; photos in Flickr order are fetched 100 at a time. Other photo orders download the album metadata in pages of 500 before showing the first image, so a large album can take longer to start. Videos are excluded. System screensaver setup is not required. An optional DreamService is included, but its availability on Google TV Streamer has not been verified.

## Privacy and limits

Flickr credentials are encrypted using AndroidKeyStore. App backup and preference transfer are disabled. Photos are downloaded directly from Flickr over HTTPS, with no automatic photo disk cache. A photo explicitly selected as the menu background is saved as a JPEG in app-private no-backup storage (maximum dimension 1920 pixels). Images fit inside the screen without cropping. Downloads are limited to 20 MiB, decoded images to roughly 8.3 million pixels. Repeated download errors stop automatic advancement and show an error.

**Delete local login** in Settings removes stored credentials, selected album and saved album positions. Access revocation at Flickr is managed separately in your Flickr account.

See [PRIVACY.md](PRIVACY.md) for the user-facing privacy policy and [docs/SECURITY-DESIGN.md](docs/SECURITY-DESIGN.md) for the data flow, security boundaries and known limitations. Security reports are described in [SECURITY.md](SECURITY.md).

This product uses the Flickr API but is not endorsed or certified by SmugMug, Inc.

## Build

Use JDK 17, Android SDK platform 35 and build-tools 35.0.0.

- Gradle: `./gradlew assembleRelease` (unsigned APK).
- Direct build, once SDK and JDK are installed: `JAVA_HOME=/path/to/jdk ANDROID_HOME=/path/to/sdk ./build-direct.sh`.

The direct build script uses aapt2, javac, d8 and zipalign. Sign `build/direct/AlbumFrame-TV-unsigned.apk` using apksigner. The private signing backup is supplied separately to the owner and must be preserved for future updates. Do not publish it with the source.

## Tests and status

The `tests` folder includes desktop tests for OAuth signatures, QR generation, local TLS/CSRF/session behavior, authenticated owner lookup, album and photo pagination, local photo ordering, private-photo parameters, image URL validation, size fallback, empty albums and expired credentials. Run tests against Android 35 stubs; FlickrTest and PhotoOrderTest additionally need org.json 20240303 at runtime ahead of android.jar.

This version is compiled with the included direct build script. Gradle plugin download was unavailable, so Gradle lint did not run. APK signature and launcher manifest are verified after building. Version 0.6.1 was tested successfully on a Google TV Streamer; 0.7.0 still needs a device check before it replaces the public release.

LOGIN-TEST.md describes the historical 0.2.1 login-only release; this version adds album viewing.

SlideshowClockTest checks automatic advancement, pause during download, resume, interval changes, duplicate timer prevention and closure with a fake scheduler. Device-level animation and remote control behavior still need verification on the TV.


## Appearance and language

- High-contrast teal focus fill and a bright border for buttons and dialog choices. Selected radio options retain their checkmark.
- Settings contains Flickr connection and deletion, photo order, interval, transition speed, language, app background removal and clearing saved album positions. Photo order defaults to Flickr's album order; capture date and upload date can each be oldest or newest first. Changing the order resets saved photo positions so the next slideshow starts with the first photo in its new order. The language choices are follow the TV (default), Danish, or English. System languages other than Danish use English. Both TV UI and the temporary phone setup page follow the selection.
- All executable source identifiers and source messages are English; Danish translations live in `app/src/main/assets/da.json`. User-provided album titles are never translated.
- During photo viewing, hold OK for at least 650 ms and release to open the background confirmation. The displayed photo is captured when the press starts. A short OK press continues to toggle pause. The previous playback state is restored after dismissing the dialog.
- The chosen photo appears behind the home and album menus with a dark overlay. Choose Remove background in Settings to delete it. It does not change the Google TV home screen or other apps. Deleting Flickr login does not delete an independently selected background; remove it separately.
- Automated checks cover translation key completeness, system-language fallback, OAuth/local setup, Flickr fixtures and slideshow timing. Hardware rendering, long-press handling and background selection still require testing on Google TV.

## Version history

The repository contains reconstructed, tagged OpenFrame snapshots for 0.3.0, 0.3.1 and 0.4.0, followed by normal 0.5.x development commits and the AlbumFrame TV rename in 0.6.x.

See [CHANGELOG.md](CHANGELOG.md) for release notes and [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) for the remaining public-store work.
