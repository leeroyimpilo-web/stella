# Lotto Intelligence — Retail white-label commerce (prototype)

**Not ready for taking live payments.** This project adds a sandbox-first commerce foundation next to the existing native app without replacing it. Never distribute the existing debug APK as a paid release.

## Functionality implemented

- Per-retailer branding, unique store links and commission percentages.
- PayFast hosted checkout, optional PayFast native percentage Split Payments when the secondary receiving merchant is approved.
- Fixed R50.00 purchase; never accept an amount from the browser.
- Server-side PayFast ITN: signature check, PayFast server validation, order/amount/merchant checks, idempotent licence issuance.
- Cryptographically random serial keys, HMAC hashes, AES-GCM encryption at rest.
- One-device activation with signed RSA offline entitlements; reinstalling on the SAME device requires the original serial again. A different device is refused, pending verified transfer or new purchase.
- Secure order receipt, paid-only APK delivery via server, and gross per-store reports.
- Checkout defaults to PayFast sandbox and refuses live payment unless explicitly enabled.
- Unit tests for key cryptography, licence device binding, signature generation and amount verification.

## What must happen before retail launch

1. Seek written confirmation from **PayFast** that a lottery statistics and number-generator software app sold at lotto retailers is approved for processing and Split Payments. This app does not sell tickets.
2. Obtain retailer agreement; verify each receiving PayFast merchant ID and that Split Payments is enabled.
3. Obtain permission before using any operator trademarks/branding or calling sales points authorised partner stores.
4. Set up a separate production Postgres DB (e.g. Neon), HTTPS hosting (e.g. Render), transactional email for receipts, monitoring, POPIA privacy notice, refunds process and proper consumer terms. Review CPA and ECTA obligations.
5. Generate a 2048/3072-bit RSA key pair for offline entitlements. Put private key in backend secure environment only and public key in the release Android build. Never commit private key.
6. Implement and test the Android activation lock screen against `POST /api/activate`, build a securely signed **release APK**, and upload that file as `APK_FILE_PATH`. Current debugging APK has no retail licence enforcement.
7. Test complete PayFast Sandbox ITNs, multi-store splitting, duplicate callbacks, cancelled payments, reinstalls, copied APKs and refund/chargeback scenarios before enabling live charges.
8. Set up customer support including transfer and key-recovery flows. Do not issue another paid charge simply for reinstalling the app on its original phone.

## Local setup

From `lotto-android/commerce`:

```bash
npm install
node --test tests/*.test.mjs
psql "$DATABASE_URL" -f schema.sql
# Provide all .env.example values through your shell or hosting secrets. Do not commit .env.
npm start
```

A dedicated HTTPS domain is necessary for real PayFast notifications. Localhost is allowed for development but can't receive payment notifications directly; use a secure testing tunnel. `/health` identifies sandbox vs live.

### Admin: register a participating retailer

```bash
curl -X POST https://your-host.example/api/admin/retailers \
 -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
 -d '{"slug":"southcoast-store","name":"South Coast Store","color":"#FFDA00","commissionPercent":30,"payfastReceiverId":"12345678","splitApproved":false}'
```

Record the returned retailer dashboard token in a private store onboarding record. **Never** set `splitApproved=true` without both merchants completing PayFast's required setup. Until approved, this example retailer's checkout is unavailable rather than silently taking a full payment without paying commission.

Store URL: `https://your-host.example/s/southcoast-store` . Retailer report: GET `/api/retailer/report` with `X-Retailer-Token` header.

## Secrets and endpoints

`.env.example` documents environment variables. All secrets, PayFast credentials, signing keys and database URLs stay on server. The website only receives short-lived payment form fields and an order-specific receipt token.

- POST `/api/checkout`
- POST `/api/payfast/itn`
- GET `/api/orders/:id` (receipt token required)
- GET `/api/orders/:id/apk` (paid receipt token required)
- POST `/api/activate` (requires serial and device/installation hashes)
- POST/GET `/api/admin/retailers` (admin bearer token)
- GET `/api/retailer/report` (retailer token)

### Offline licensing tradeoff

The public key validates a server-signed entitlement **on the activated device without internet**. Installation state is stored locally, so reinstalling removes the cached token. The customer's original serial can restore access on the same signed-APK device. Device fingerprints may change after factory reset or app-signing certificate changes, so human-reviewed transfers must be available. Offline licences cannot be instantly remotely revoked, and no APK is entirely piracy-proof.
