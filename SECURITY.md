# Security policy

## Supported version

Only the latest published version receives security fixes. Before reporting an issue, verify that it is still present in that version.

## Reporting a vulnerability

Do not post API keys, OAuth tokens, private photo links or an exploitable vulnerability in a public issue. After the repository is published on GitHub, use GitHub private vulnerability reporting if it is enabled. Until a private reporting channel is listed in the public repository, contact the distributor without including secrets and ask for a secure channel.

Include the app version, Android/Google TV version, steps to reproduce and the security impact. Use test credentials and non-private photos whenever possible.

## Scope

The security design and known limitations are documented in [docs/SECURITY-DESIGN.md](docs/SECURITY-DESIGN.md). A self-signed certificate is expected during local phone setup and is not itself a vulnerability. An unexpected address, a setup page reached without scanning the current TV QR code, or credentials leaving the local network may be a security issue.
