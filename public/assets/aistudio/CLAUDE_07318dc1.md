# Baon Tracker

Offline-first Android app for Filipino students that tracks spending against their baon (allowance) cycle and shows "safe to spend today".

<!-- planfirst:start -->
## Architecture Reference
Full spec: `./docs/architecture.md` — read before adding any infrastructure.

### Stack (momento: validate)
- App: Expo (React Native) + TypeScript + Expo Router
- Local DB (source of truth): expo-sqlite + Drizzle ORM
- Backend: Supabase (Auth with Google, Postgres + RLS, Edge Functions)
- AI: Gemini API (free tier) called only from the `ai-parse` Edge Function
- Styling: NativeWind
- Charts: react-native-gifted-charts
- State: Zustand (UI state only)
- Notifications: expo-notifications (local only)
- Builds: Expo Go for dev, EAS Build (APK) for testers

### Agent Rules
- ALWAYS store money as integer centavos (`amount_centavos`). NEVER use floats for money.
- ALWAYS read and write app data through Drizzle + expo-sqlite. NEVER call Supabase tables directly from screens.
- ALWAYS write changes to the local `outbox` so the sync service can push them. Sync is last-write-wins on `updated_at`.
- ALWAYS soft-delete (`deleted_at`) synced rows. NEVER hard-delete them.
- NEVER put the Gemini API key or any secret in app code or `app.json`. AI calls go ONLY through the `ai-parse` Supabase Edge Function.
- ALWAYS treat AI output as a draft the user confirms before saving. Validate AI JSON with zod.
- ALWAYS compress images (max 1024px, JPEG ~0.7) before uploading to `ai-parse`.
- ALWAYS enable Row Level Security on every Supabase table with policies on `user_id = auth.uid()`.
- The forecast (safe-to-spend, days-left) MUST be pure TypeScript functions with unit tests. DO NOT use AI for it.
- DO NOT add Redux, Firebase, LangChain, a vector DB, a custom Node backend, PowerSync or WatermelonDB.
- DO NOT integrate GCash/Maya/bank logins.
- Use NativeWind for styling and Expo Router for navigation. UI copy in English with Taglish labels (Pagkain, Pamasahe, Ipon).
- Build in phase order: Phase 1 (offline core) must work without an account before adding auth, sync or AI.
- Target Android first. Test with Expo Go; ship test builds as APKs via EAS.
<!-- planfirst:end -->
