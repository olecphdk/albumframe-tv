# Public release checklist

## Before any public beta

- [ ] Choose the permanent public app name and application ID.
- [ ] Publish the complete Git history in the owner's repository.
- [ ] Add a private security contact or enable GitHub private vulnerability reporting.
- [ ] Replace placeholder repository/contact wording in the privacy and security documents.
- [ ] Confirm in writing that Flickr permission covers the chosen API key model, distribution channels and any price.
- [ ] Publish the privacy policy at a stable public URL.
- [ ] Build, sign and archive the release; preserve the signing key separately.
- [ ] Upgrade-test the signed APK on the Google TV Streamer.
- [ ] Verify that no API keys, OAuth tokens, signing files or private photo URLs are tracked.

## Google Play

- [ ] Produce and validate an Android App Bundle (AAB), not only an APK.
- [ ] Complete developer account verification and the required testing track.
- [ ] Complete Data safety and app-access declarations.
- [ ] Supply TV screenshots, 320 x 180 banner and 160 x 160 icon.
- [ ] Provide reviewers with a reproducible way to exercise the app without personal credentials.
- [ ] Confirm the current target API and Android TV quality requirements immediately before submission.

## F-Droid

- [ ] Ensure the public repository builds entirely from source with no proprietary build dependency.
- [ ] Submit metadata, screenshots, license and reproducible version/tag information.
- [ ] Declare the Flickr dependency; expect the `NonFreeNet` anti-feature because Flickr is a non-free network service.
- [ ] Confirm that the application ID is acceptable and based on a domain or namespace controlled by the publisher.

## Every release

- [ ] Update version code/name, README and changelog.
- [ ] Run all tests and translation coverage checks.
- [ ] Inspect permissions, exported components and network-security settings.
- [ ] Verify signature continuity and upgrade behavior.
- [ ] Test both Danish and English UI on TV hardware.
