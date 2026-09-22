# Security design

## Data flow and trust boundaries

1. The user scans a one-time QR address displayed by the TV.
2. The phone connects directly to a temporary HTTPS server bound to the TV's local IPv4 address.
3. The phone submits a user-supplied Flickr API key and secret to the TV.
4. The TV performs Flickr OAuth 1.0a requests and redirects the phone to Flickr for read-only authorization.
5. Flickr redirects the phone to the TV, which exchanges the verifier for an access token.
6. The TV encrypts the API and OAuth credentials with an Android Keystore key.
7. Album metadata and photos subsequently travel directly between the TV and Flickr over HTTPS.

The developer operates no login, proxy, analytics or image service.

## Local setup controls

- The server binds only to the selected local IPv4 interface and an ephemeral port.
- The URL contains a random 128-bit path and expires after ten minutes.
- The server accepts bounded headers and request bodies, rejects duplicate headers, transfer encoding, unexpected methods, incorrect Host and cross-origin form submissions.
- The OAuth callback token is bound to the active session.
- TLS 1.2 or newer is required. A new RSA key and self-signed certificate are generated in memory for every session.
- Responses prevent caching, framing and resource embedding and use a restrictive content security policy.
- Credentials and tokens are cleared when the server closes. Sensitive values are not logged.

## Stored data

Credential values are encrypted with AES-GCM using a non-exportable Android Keystore key. Android backup and data extraction are disabled. Preferences and a user-selected app background are private to the application. Ordinary slideshow photos are not written to disk by the app.

Deleting local login removes encrypted credentials, selected album and resume positions. Removing the background deletes its local file. Revocation at Flickr must be performed separately by the user.

## Network and media controls

The application disallows cleartext traffic. Flickr API and image URLs must use HTTPS and expected Flickr hosts. Image downloads and decoded dimensions are bounded to reduce memory exhaustion. Automatic playback stops after repeated download failures.

## Known limitations

- The local server certificate is self-signed because no public certificate authority can issue a certificate for an ephemeral private-network address. Encryption prevents passive observation, but accepting the warning does not authenticate the TV against an active attacker on the same network. The user must compare the phone address with the TV and use a trusted home network.
- A hostile or rooted TV, malicious accessibility service, compromised Flickr account, compromised Flickr infrastructure or compromised device firmware is outside the application's protection boundary.
- Android applications cannot keep an embedded API secret confidential from a determined reverse engineer. This design therefore asks each user to supply a Flickr API key rather than placing the owner's secret in the APK.
- A photo explicitly saved as the app background remains locally available until the user removes it or uninstalls the app, even if its Flickr visibility later changes.

## Release verification

Release builds are expected to pass the deterministic desktop tests, translation coverage check, APK signature verification and manifest inspection. A real-device smoke test must cover upgrade, login preservation, album thumbnails, automatic slideshow, pause/resume, interval selection, transitions, language switching, background selection/removal and login deletion.
