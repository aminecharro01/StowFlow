# StowFlow (frontend)

Frontend **Next.js** (App Router, Tailwind) pour la gestion de stock multi-tenant.

L’API REST est dans **`../stowflow-api`** (Spring Boot, port **8000**).

## Démarrage

```bash
npm install
npm run dev
```

Ouvrir `http://localhost:3000` (ou le port indiqué par Next).

En développement local, **ne pas** définir `NEXT_PUBLIC_API_URL` : le proxy Next.js (`next.config.ts`) redirige `/api/*` vers `http://127.0.0.1:8000`.

## Configuration optionnelle

Copier `.env.local.example` vers `.env.local` uniquement si l’API n’est pas sur la machine locale :

```bash
cp .env.local.example .env.local
```

Voir le [README racine](../README.md) pour les comptes démo, Gemini et `run-dev.ps1`.
