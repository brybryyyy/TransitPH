import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const PREMIUM_PRICE_MINOR = 4900;
const PREMIUM_PLAN = "PREMIUM_MONTHLY";

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  const paymongoSecret = Deno.env.get("PAYMONGO_SECRET_KEY") ?? "";
  if (!supabaseUrl || !serviceRoleKey || !paymongoSecret) {
    return json({ error: "Payment service is not configured on the server." }, 503);
  }

  const bearer = request.headers.get("Authorization") ?? "";
  const accessToken = bearer.startsWith("Bearer ") ? bearer.slice(7) : "";
  if (!accessToken) return json({ error: "Sign in is required." }, 401);

  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
  const { data: authData, error: authError } = await admin.auth.getUser(accessToken);
  const user = authData.user;
  if (authError || !user) return json({ error: "Your session is invalid. Please sign in again." }, 401);

  const { data: profile } = await admin.from("profiles").select("premium_until").eq("id", user.id).maybeSingle();
  if (profile?.premium_until && new Date(profile.premium_until).getTime() > Date.now()) {
    return json({ error: "Premium is already active.", premium: true }, 409);
  }

  const referenceNumber = "TPH-" + crypto.randomUUID();
  const { error: orderError } = await admin.from("premium_payment_orders").insert({
    user_id: user.id,
    reference_number: referenceNumber,
    amount_minor: PREMIUM_PRICE_MINOR,
    currency: "PHP",
    plan_type: PREMIUM_PLAN,
    status: "pending",
  });
  if (orderError) return json({ error: "Could not prepare the payment record." }, 500);

  const publicUrl = supabaseUrl.replace(/\/$/, "") + "/functions/v1/paymongo-return";
  const returnBase = publicUrl + "?reference=" + encodeURIComponent(referenceNumber);
  const credentials = btoa(paymongoSecret + ":");
  let paymongoResponse: Response;
  try {
    paymongoResponse = await fetch("https://api.paymongo.com/v2/checkout_sessions", {
      method: "POST",
      headers: {
        Authorization: "Basic " + credentials,
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({
        data: {
          attributes: {
            line_items: [{
              name: "TransitPH Premium — 1 month",
              amount: PREMIUM_PRICE_MINOR,
              currency: "PHP",
              quantity: 1,
            }],
            payment_method_types: ["card", "gcash", "qrph"],
            success_url: returnBase + "&result=success",
            cancel_url: returnBase + "&result=cancel",
            reference_number: referenceNumber,
            send_email_receipt: true,
            metadata: {
              plan_type: PREMIUM_PLAN,
              reference_number: referenceNumber,
              user_id: user.id,
            },
          },
        },
      }),
    });
  } catch {
    return json({ error: "Could not connect to PayMongo. Please try again." }, 502);
  }

  const paymongoBody = await paymongoResponse.json().catch(() => ({}));
  if (!paymongoResponse.ok) {
    console.error("PayMongo checkout error", paymongoBody?.errors?.[0]?.code ?? paymongoResponse.status);
    return json({ error: "PayMongo could not create checkout. Please try again later." }, 502);
  }

  const session = paymongoBody?.data;
  const checkoutUrl = session?.attributes?.checkout_url;
  const checkoutSessionId = session?.id;
  if (!checkoutUrl || !checkoutSessionId) {
    return json({ error: "PayMongo returned an incomplete checkout session." }, 502);
  }

  const { error: updateError } = await admin.from("premium_payment_orders")
    .update({ checkout_session_id: checkoutSessionId })
    .eq("reference_number", referenceNumber);
  if (updateError) return json({ error: "Could not save the checkout session." }, 500);

  return json({ checkoutUrl, referenceNumber, planType: PREMIUM_PLAN }, 200);
});

function json(body: Record<string, unknown>, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}