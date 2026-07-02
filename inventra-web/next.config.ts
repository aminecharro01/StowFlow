import type { NextConfig } from "next";
import path from "path";
import { fileURLToPath } from "url";

/** Dossier de cette app (évite que Turbopack prenne `C:\Users\user` à cause d’un package-lock parent). */
const appRoot = path.dirname(fileURLToPath(import.meta.url));

/** Backend Spring — le front appelle `/api/...` (même origine), Next proxifie vers ce port. */
const apiTarget = process.env.API_PROXY_TARGET ?? "http://127.0.0.1:8000";

const nextConfig: NextConfig = {
  output: "standalone",
  turbopack: {
    root: appRoot,
  },
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${apiTarget}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
