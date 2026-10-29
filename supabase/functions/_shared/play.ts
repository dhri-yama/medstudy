// Shared helpers for talking to the Google Play Developer API from Edge Functions (Deno).
// Secrets (set with `supabase secrets set`, never in the app):
//   GOOGLE_SERVICE_ACCOUNT_JSON  the Play Console API service account key (full JSON)
//   PLAY_PACKAGE_NAME            e.g. com.medstudy.app

export const PRODUCT_ID = "medstudy_monthly";
const API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications";

function b64url(data: ArrayBuffer | string): string {
  const bytes = typeof data === "string" ? new TextEncoder().encode(data) : new Uint8Array(data);
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** OAuth access token for the service account (RS256 JWT bearer grant). */
async function accessToken(): Promise<string> {
  const key = JSON.parse(Deno.env.get("GOOGLE_SERVICE_ACCOUNT_JSON")!);
  const now = Math.floor(Date.now() / 1000);
  const header = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = b64url(JSON.stringify({
    iss: key.client_email,
    scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const pem = (key.private_key as string).replace(/-----[A-Z ]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", cryptoKey, new TextEncoder().encode(`${header}.${claims}`));
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${header}.${claims}.${b64url(sig)}`,
    }),
  });
  if (!res.ok) throw new Error(`Google token request failed: ${res.status}`);
  return (await res.json()).access_token;
}

export type PlaySubscription = {
  state: string;                 // SUBSCRIPTION_STATE_*
  productId: string;
  expiresAt: Date;
  acknowledged: boolean;
  obfuscatedAccountId: string | null;
};

/** Asks Google what this purchase token is worth right now. */
export async function fetchSubscription(purchaseToken: string): Promise<PlaySubscription> {
  const pkg = Deno.env.get("PLAY_PACKAGE_NAME")!;
  const res = await fetch(`${API}/${pkg}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`, {
    headers: { Authorization: `Bearer ${await accessToken()}` },
  });
  if (!res.ok) throw new Error(`Play API ${res.status}: ${await res.text()}`);
  const body = await res.json();
  const line = body.lineItems?.[0];
  return {
    state: body.subscriptionState,
    productId: line?.productId,
    expiresAt: new Date(line?.expiryTime ?? 0),
    acknowledged: body.acknowledgementState === "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
    obfuscatedAccountId: body.externalAccountIdentifiers?.obfuscatedExternalAccountId ?? null,
  };
}

/** Acknowledges a new purchase; Google refunds unacknowledged purchases after 3 days. */
export async function acknowledge(purchaseToken: string): Promise<void> {
  const pkg = Deno.env.get("PLAY_PACKAGE_NAME")!;
  const res = await fetch(
    `${API}/${pkg}/purchases/subscriptions/${PRODUCT_ID}/tokens/${encodeURIComponent(purchaseToken)}:acknowledge`,
    { method: "POST", headers: { Authorization: `Bearer ${await accessToken()}`, "Content-Type": "application/json" }, body: "{}" },
  );
  if (!res.ok && res.status !== 400) throw new Error(`Acknowledge failed: ${res.status}`); // 400 = already acknowledged
}

/** A canceled subscription stays valid until it expires; on hold, expired and pending ones are not. */
export function isEntitled(sub: PlaySubscription): boolean {
  const ok = ["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD", "SUBSCRIPTION_STATE_CANCELED"];
  return ok.includes(sub.state) && sub.expiresAt.getTime() > Date.now();
}
