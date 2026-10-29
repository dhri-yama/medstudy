// Called by the app right after a Google Play purchase (and by "Restore purchase").
// Checks the token with Google, then records the paid period. The app never writes it itself.
// Deploy with JWT verification ON (the default): only signed-in users can call it.
import { createClient } from "jsr:@supabase/supabase-js@2";
import { acknowledge, fetchSubscription, isEntitled, PRODUCT_ID } from "../_shared/play.ts";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method not allowed" }, 405);

  const url = Deno.env.get("SUPABASE_URL")!;
  const asUser = createClient(url, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: req.headers.get("Authorization") ?? "" } },
  });
  const { data: { user } } = await asUser.auth.getUser();
  if (!user) return json({ error: "not signed in" }, 401);

  const { productId, purchaseToken } = await req.json().catch(() => ({}));
  if (productId !== PRODUCT_ID || typeof purchaseToken !== "string" || !purchaseToken) {
    return json({ error: "bad request" }, 400);
  }

  const sub = await fetchSubscription(purchaseToken);
  if (sub.productId !== PRODUCT_ID) return json({ error: "wrong product" }, 400);
  // Set by the app at purchase time; stops one account replaying another account's token.
  if (sub.obfuscatedAccountId && sub.obfuscatedAccountId !== user.id) return json({ error: "token belongs to another account" }, 403);
  if (!isEntitled(sub)) return json({ error: "subscription is not active", state: sub.state }, 402);

  const admin = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data: owner } = await admin.from("subscriptions").select("user_id").eq("provider_ref", purchaseToken).maybeSingle();
  if (owner && owner.user_id !== user.id) return json({ error: "token already used by another account" }, 409);

  const { error } = await admin.from("subscriptions").update({
    plan_id: "monthly",
    current_period_end: sub.expiresAt.toISOString(),
    provider: "google_play",
    provider_ref: purchaseToken,
  }).eq("user_id", user.id);
  if (error) return json({ error: error.message }, 500);

  if (!sub.acknowledged) await acknowledge(purchaseToken);
  return json({ ok: true, expiresAt: sub.expiresAt.toISOString() });
});
