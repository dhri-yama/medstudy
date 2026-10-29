# Edge Functions (Google Play subscriptions)

Source only. These are **not deployed yet** because they need Play Console credentials.

| Function | JWT verification | Purpose |
|---|---|---|
| `verify-purchase` | on (default) | The app sends the purchase token right after a purchase / "Restore purchase". Checks it with Google, records the paid period, acknowledges it. |
| `play-rtdn` | **off** + `?secret=` | Google's Pub/Sub push for renewals, cancellations, expiry. |

## One-time setup (cannot be done from code)
1. Play Console: create the app, a subscription with product id `medstudy_monthly` (a base plan, **no free-trial offer**: the 15-day trial is server-side), and link a Google Cloud project.
2. Play Console > API access: create a service account, grant it "View financial data" and "Manage orders and subscriptions", download its JSON key.
3. Cloud Pub/Sub: create a topic, give `google-play-developer-notifications@system.gserviceaccount.com` publish rights, add a push subscription to `https://<project>.supabase.co/functions/v1/play-rtdn?secret=<RTDN_SHARED_SECRET>`, then set it as the real-time notification topic in Play Console.
4. Secrets (never paste them in chat or commit them):
   `supabase secrets set GOOGLE_SERVICE_ACCOUNT_JSON="$(cat key.json)" PLAY_PACKAGE_NAME=com.medstudy.app RTDN_SHARED_SECRET=<random>`
5. Deploy: `supabase functions deploy verify-purchase` and `supabase functions deploy play-rtdn --no-verify-jwt`.
6. Test on an internal-testing track build with a license-tester account (Play Billing does not work on sideloaded debug builds).
