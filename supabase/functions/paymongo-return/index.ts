Deno.serve((request) => {
  const incoming = new URL(request.url);
  const result = incoming.searchParams.get("result") === "success" ? "success" : "cancel";
  const reference = incoming.searchParams.get("reference") ?? "";
  const deepLink = "transitph://payment-return?result=" + result + "&reference=" + encodeURIComponent(reference);
  const heading = result === "success" ? "Checking your TransitPH payment" : "Returning to TransitPH";
  const detail = result === "success"
    ? "The app will check PayMongo’s payment confirmation and activate Premium after it is verified."
    : "Your payment was not completed. You can return to the app and try again.";
  const page = "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>TransitPH payment</title><style>body{font-family:Arial,sans-serif;margin:0;background:#f4f8f7;color:#14231e;display:grid;min-height:100vh;place-items:center}.card{max-width:420px;margin:24px;padding:28px;background:white;border-radius:18px;box-shadow:0 8px 30px #16382a18;text-align:center}a{display:inline-block;background:#00966e;color:white;text-decoration:none;padding:14px 22px;border-radius:10px;font-weight:bold}</style></head><body><main class=\"card\"><h1>" + heading + "</h1><p>" + detail + "</p><a href=\"" + deepLink + "\">Open TransitPH</a></main><script>setTimeout(function(){window.location.href=" + JSON.stringify(deepLink) + "},700)</script></body></html>";
  return new Response(page, { headers: { "Content-Type": "text/html; charset=utf-8" } });
});