"use client";
import { useEffect } from "react";
// Remove workers installed by previous demo builds, so they cannot intercept real requests.
export function RetireMockWorker() {
  useEffect(() => {
    if (!("serviceWorker" in navigator)) return;
    void navigator.serviceWorker.getRegistrations().then(async (registrations) => {
      let removed = false;
      for (const registration of registrations) {
        const worker = registration.active ?? registration.waiting ?? registration.installing;
        if (worker && new URL(worker.scriptURL).pathname.endsWith("/mockServiceWorker.js")) {
          removed = (await registration.unregister()) || removed;
        }
      }
      if (removed && navigator.serviceWorker.controller?.scriptURL.includes("mockServiceWorker.js")) window.location.reload();
    }).catch(() => undefined);
  }, []);
  return null;
}
