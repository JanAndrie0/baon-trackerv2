# First prompt for Claude Code

Paste this into Claude Code after putting `CLAUDE.md` in the project root and `architecture.md` + `research.md` in `docs/`:

---

Read CLAUDE.md, docs/architecture.md and docs/research.md first.

We're building Baon Tracker, Phase 1 only (offline core, no login, no sync, no AI yet):

1. Create a new Expo app with TypeScript and Expo Router in this folder. Set up NativeWind, expo-sqlite + Drizzle ORM, and Zustand.
2. Create the local schema from the Data Model section: allowance_cycles, wallets, categories, transactions, outbox. Money is integer centavos. Seed default categories (Pagkain, Pamasahe, Load, Print/Xerox, School, Gala, Others) and wallets (Cash, GCash, Maya).
3. Build the onboarding: how I get baon (daily/weekly/monthly), amount, next baon date.
4. Build the forecast engine as pure TypeScript functions (remaining, days_left, safe_to_spend_today, status color) with unit tests.
5. Build the Home screen (Baon Meter + today's list + Add button), the Add expense sheet (number pad, category chips, wallet), and History (grouped by day, edit/delete). Use bottom tabs: Home, History, Insights (placeholder), Settings.
6. Add a daily local reminder to log expenses.

Make a plan first and show it to me before writing code. Explain each step simply because I'm still learning React Native.
