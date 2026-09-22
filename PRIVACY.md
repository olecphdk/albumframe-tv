# Privacy policy for AlbumFrame TV

Last updated: 2026-09-21

AlbumFrame TV is an open-source Android TV application that displays albums from a Flickr account. The app contains no advertising, analytics, telemetry or crash-reporting service. The developer does not receive or store your credentials, photos, album information or usage data.

## Data handled by the app

- Flickr API credentials and OAuth access credentials are stored only on the Android TV device, encrypted with Android Keystore.
- Album and photo metadata is requested directly from Flickr over HTTPS.
- Photos are requested directly from Flickr over HTTPS and normally remain only in memory.
- If you explicitly choose a photo as the app background, a resized JPEG copy is stored in the app's private, no-backup storage on the TV.
- Slideshow preferences, selected album and resume positions are stored locally on the TV.

AlbumFrame TV does not request your Flickr password. Authorization takes place on Flickr's website. Flickr processes data under its own privacy policy and terms.

## Local phone setup

During setup, the TV temporarily runs an HTTPS server on the local network. The phone sends the Flickr API key and secret directly to the TV. The server uses a random session address, accepts a single setup attempt and closes when setup finishes, when you leave the setup screen, or after ten minutes.

The server uses a temporary self-signed certificate, so the phone browser displays a certificate warning. Confirm that the address shown by the browser is identical to the address shown on the TV, and perform setup only on a trusted home network. No permanent internet service operated by the developer is involved.

## Delete or revoke data

Use **Delete local login** in the app to remove stored Flickr API and OAuth credentials, the selected album and saved positions. Use **Remove background** to delete a background photo saved by the app. Uninstalling the app also removes its private local data.

Revoking AlbumFrame TV access at Flickr is a separate action managed in your Flickr account.

## Contact and changes

Security issues should be reported as described in [SECURITY.md](SECURITY.md). Other questions can be raised through the [source repository's issue tracker](https://github.com/olecphdk/albumframe-tv/issues). Material changes to this policy will be recorded in the repository history and included with a new app release.

This product uses the Flickr API but is not endorsed or certified by SmugMug, Inc.
