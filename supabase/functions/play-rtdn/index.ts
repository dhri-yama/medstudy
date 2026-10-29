// Google Play real-time developer notifications (Pub/Sub push): renewals, cancellations, expiry, holds.
// Keeps `current_period_end` correct without the user opening the app.
// Deploy with JWT verification OFF (Pub/Sub cannot send a Supabase JWT) and protect it with a shared secret:
//   supabase secrets set RTDN_SHARED_SECRET=<random>   and point the Pub/Sub push endpoint at  .../play-rtdn?secret=<random>
import { createClient } from "jsr:@supabase/supabase-js@2";
import { fetchSubscription } from "../_shared/play.ts";

Deno.serve(async (req) => {
  const secret = new URL(req.url).searchParams.get("secret");
  if (!secret || secret !== Deno.env.get("RTDN_SHARED_SECRET")) return new Response("forbidden", { status: 403 });

  const push = await req.json().catch(() => null);
  const encoded = push?.message?.data;
  if (!encoded) return new Response("ok"); // nothing to do, don't make Pub/Sub retry

  const note = JSON.parse(atob(encoded));
  const token: string | undefined = note.subscriptionNotification?.purchaseToken;
  if (!token) return new Response("ok"); // test or non-subscription notification

  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data: row } = await admin.from("subscriptions").select("user_id").eq("provider_ref", token).maybeSingle();
  if (!row) return new Response("ok"); // purchase not verified by the app yet; verify-purchase will record it

  try {
    const sub = await fetchSubscription(token);
    // Expired / on-hold subscriptions carry a past expiry, so access ends on its own; renewals move it forward.
    await admin.from("subscriptions").update({ current_period_end: sub.expiresAt.toISOString() }).eq("user_id", row.user_id);
    return new Response("ok");
  } catch (e) {
    console.error(e);
    return new Response("error", { status: 500 }); // Pub/Sub will retry
  }
});
