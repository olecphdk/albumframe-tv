# Version 0.2.1 — local Flickr login test

Fixes the form POST rejection in 0.2.0: local pages now use Referrer-Policy:
same-origin, which preserves Origin on same-origin native form submissions.
The Flickr redirect still uses no-referrer. Exact Origin checking is retained;
foreign, null and missing origins are still rejected. Error codes distinguish
Origin mismatch (F01), repeated submission (F02), and content type (F03).

Regression tests check the returned form policy, valid-origin form submission
reaching field validation without a Flickr call, and rejection of null/missing/
foreign origins. Live Android browser and Flickr authorization still need a
device test. Install over the previous app and scan a newly generated QR.

## Scope

The APK now implements local QR setup, an ephemeral HTTPS listener, Flickr
OAuth 1.0a read-only login and encrypted credential storage. It has no embedded
API credentials and needs no external pairing service. Buttons now size to content.

**Album selection and Flickr photo display are not implemented yet. The slideshow
and DreamService still show demo art. This is a development login test, not a
production or F-Droid release.**

## Instructions

1. Install over 0.1.0 (same package and signing certificate).
2. Open Forbind Flickr. Phone and TV must use the same trusted home network.
3. Scan QR, or manually open the displayed HTTPS URL in the phone browser.
4. Verify the URL matches the TV before proceeding past the local self-signed
   certificate warning. Never bypass a certificate warning for flickr.com.
5. Enter your Flickr API Key and Secret, NOT your Flickr password.
6. If Flickr accepts the local callback, the browser opens Flickr for read-only
   authorization. Approve and the browser returns to the TV's local page.
7. TV exchanges the verifier, tests `flickr.test.login` and stores credentials
   encrypted. It should show Forbundet som ... and close the local server.
8. Return home and restart the app to check persistence. Slideshow is still demo.

The listener also closes after ten minutes, Back, Activity stop or destruction.
No port forwarding, domain, hosting account or permanent service is needed.
VPN or guest-network isolation may block phone-to-TV traffic. Managed browsers
may prohibit bypassing the local certificate warning. Restart setup if interrupted.

## Security and limitations

Callback acceptance by Flickr, actual phone/TV flow and AndroidKeyStore persistence
have NOT been tested with a real account/device. Desktop tests do not prove these.
Bad keys, clock skew or connectivity can look like callback rejection.

The local TLS certificate/key is freshly generated and held only in memory.
TLS encrypts transport; bypassing a self-signed certificate warning does not
establish public-CA identity. This prototype assumes a trusted home network and
is not hardened against active LAN attackers.

The QR is generated on-device and contains a random 128-bit session URL, never
API secrets. Do not share live QR or callback URLs. The listener binds only the
active private IPv4 address. Host, Origin, callback token and duplicate parameters
are checked. Request sizes and socket timeouts are bounded. API calls to Flickr
use platform HTTPS validation; there is no trust-all code in the app.

API Key/Secret and user token/secret are encrypted with AndroidKeyStore AES-GCM
in private preferences, excluded from cloud backup and device transfer. They are
never returned to the browser or logged. Java strings cannot reliably be zeroized.
Encrypted storage cannot protect a compromised OS. Deleting local login does not
revoke the app's access in Flickr account settings.

## Development tests

JDK 17, Android SDK 35, Gradle 8.10.2 / AGP 8.7.3:

```sh
./gradlew assembleDebug lintDebug
mkdir -p tests/build
javac -d tests/build app/src/main/java/dk/femto/albumframe/{OAuth,LocalTls,SmallQr,PairingServer}.java tests/CoreTest.java
java -cp tests/build dk.femto.albumframe.CoreTest
```

CoreTest uses only loopback TLS and synthetic OAuth data. Checks: RFC signature
vector, encoding, duplicate rejection, QR capacity, certificate validity and
self-signature, local form, headers, CSRF, callback-token binding, HTTP methods
and listener shutdown. QR encoder is fixed version 4-L/byte-mode/mask-0, max 78
bytes with a four-module quiet zone; it needs no runtime dependency.

On-device testing is still needed for layout, scanning, browser certificate UI,
AndroidKeyStore, and actual Flickr OAuth. No user API credentials are included.

Official OAuth reference: https://www.flickr.com/services/api/auth.oauth.html
