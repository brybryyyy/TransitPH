import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (request) => {
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const rawBody = await request.text();
  const signature = request.headers.get("paymongo-signature") ?? "";
  let payload: any;
  try {
    payload = JSON.parse(rawBody);
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

    const envelope = payload?.data ?? {};
  const eventAttributes = envelope?.attributes ?? {};
  const eventType = eventAttributes?.type ?? envelope?.type ?? "";
  const session = eventAttributes?.data ?? envelope?.data ?? {};
  const isLive =
    eventAttributes?.livemode ??
    envelope?.livemode ??
    session?.attributes?.livemode ??
    false;
  const secret = Deno.env.get(isLive ? "PAYMONGO_WEBHOOK_SECRET_LIVE" : "PAYMONGO_WEBHOOK_SECRET_TEST")
    ?? Deno.env.get("PAYMONGO_WEBHOOK_SECRET") ?? "";
  if (!secret || !await verifySignature(rawBody, signature, secret, Boolean(isLive))) {
    return json({ error: "Invalid webhook signature" }, 401);
  }

  if (eventType !== "checkout_session.payment.paid") return json({ received: true }, 200);

  const sessionId = String(session?.id ?? "");
  const attributes = session?.attributes ?? {};
  const referenceNumber = String(attributes?.reference_number ?? "");
  const paidPayment = Array.isArray(attributes?.payments)
    ? attributes.payments.find((payment: any) => payment?.attributes?.status === "paid")
    : null;
  if (!sessionId || !referenceNumber || !paidPayment) return json({ error: "Paid checkout data is incomplete" }, 400);

  const paymentAttributes = paidPayment.attributes ?? {};
  const paidAmount = Number(paymentAttributes.amount);
  const currency = String(paymentAttributes.currency ?? "").toUpperCase();
  const lineItems = Array.isArray(attributes.line_items) ? attributes.line_items : [];
  const lineTotal = lineItems.reduce((sum: number, line: any) =>
    sum + Number(line?.amount ?? 0) * Number(line?.quantity ?? 0), 0);
  if (paidAmount !== 4900 || currency !== "PHP" || lineTotal !== 4900) {
    console.error("Paid checkout does not match TransitPH Premium price", referenceNumber);
    return json({ error: "Paid amount did not match the plan" }, 400);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  if (!supabaseUrl || !serviceRoleKey) return json({ error: "Webhook storage is not configured" }, 503);
  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });

  const { data: order, error: orderError } = await admin.from("premium_payment_orders")
    .select("checkout_session_id, status, amount_minor, currency")
    .eq("reference_number", referenceNumber)
    .maybeSingle();
  if (orderError || !order) return json({ error: "Payment order was not found" }, 404);
  if (order.status === "paid") return json({ received: true, alreadyProcessed: true }, 200);
  if (order.checkout_session_id !== sessionId || order.amount_minor !== 4900 || order.currency !== "PHP") {
    return json({ error: "Checkout session does not match the pending order" }, 400);
  }

  const { data: activated, error: activationError } = await admin.rpc("activate_premium_from_paymongo", {
    p_reference_number: referenceNumber,
    p_checkout_session_id: sessionId,
    p_payment_id: String(paidPayment.id ?? ""),
    p_paid_amount: paidAmount,
    p_currency: currency,
  });
  if (activationError || activated !== true) {
    console.error("Premium activation failed", activationError?.message ?? "order validation failed");
    return json({ error: "Premium activation could not be recorded" }, 500);
  }

  return json({ received: true }, 200);
});

async function verifySignature(rawBody: string, header: string, secret: string, live: boolean): Promise<boolean> {
  const parts: Record<string, string> = {};
  for (const item of header.split(",")) {
    const [key, ...value] = item.trim().split("=");
    if (key) parts[key] = value.join("=");
  }
  const timestamp = parts.t;
  const provided = live ? parts.li : parts.te;
  if (!timestamp || !provided || !/^\d+$/.test(timestamp)) return false;
const encoder = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw", encoder.encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"],
  );
  const signatureBytes = new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(timestamp + "." + rawBody)));
  const expected = Array.from(signatureBytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
  if (provided.length !== expected.length) return false;
  let difference = 0;
  for (let i = 0; i < expected.length; i++) difference |= expected.charCodeAt(i) ^ provided.charCodeAt(i);
  return difference === 0;
}

function json(body: Record<string, unknown>, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}