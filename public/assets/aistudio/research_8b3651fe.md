# Baon Tracker — Research Notes

> Collected 2026-11-16. Background context for why the app exists and which features it borrows. Read with `architecture.md`.

## Why a baon tracker
- Filipino students live on small, irregular allowances (baon) handed out daily, weekly or as a monthly padala. Commonly cited daily baon is ₱100–₱150 (blog estimate, not a survey).
- Fare, load/data, printing (~₱1.50–₱3 per B&W page, ₱5–₱10 color) and food eat the budget fast.
- U-Belt bedspaces cost roughly ₱3,500–₱4,500 a month, so dorm students budget even tighter.
- Students are phone-first. In a UE study, ~98% owned smartphones, and they used them far more than laptops. Mobile data is cheaper now but still not free, so the app must work offline and use little data.
- Students already use AI a lot (83% of PH students have used generative AI for learning, according to the ASEAN Foundation in 2026), so AI quick entry fits how they already work.
- Generic budget apps don't understand student categories or the allowance cycle.

## Target users
- First: the developer (UE BSIT 3rd year) + a few classmates
- Later: college and SHS students in Metro Manila, including working students

## Competitor analysis
| App | Strength | Weakness | What we borrow |
|---|---|---|---|
| Monefy | One-tap logging, color-coded pie chart | Generic categories, ads | One-tap quick add |
| Money Manager (Realbyte) | Ledger, calendar view, receipt photos | Ads; paid sync; too complex for students | Calendar view |
| Goodbudget / envelope apps | Money split into envelopes (baon, transport, savings) | Manual setup every period | Envelopes that reset each baon cycle |
| Fleur | Aesthetic UI; PH working students praise it | ₱299 lifetime premium | Cute, polished design |
| Pocket Clear | PHP support, offline, Cash/GCash/Maya, no bank login | No AI, no forecast | Offline-first, wallets as accounts |
| GCash Money Manager | Real-time spend view inside GCash | Only sees GCash, not cash | Track cash + GCash + Maya together |
| AI trackers (Papirer, moneasy, EnExpense) | Voice/text/receipt AI entry, share screenshots from payment apps | Built for USD and salaried adults; paid | Taglish text entry + receipt/GCash screenshot parsing |
| CashMate, Fortune City | Ipon challenges, gamified budgeting | Niche | Streaks and ipon goals (later) |

## Our gap / unique value
1. **"Safe to spend today"**, a forecast tied to the baon cycle ("Will my baon last until the next one?"). No competitor has this.
2. Student categories: Pagkain, Pamasahe, Load, Print/Xerox, School, Gala.
3. Taglish AI quick entry: `jeep 13, xerox 20, lunch 85 gcash`.
4. Free, offline-first, no bank or e-wallet login.

## Decisions made with the user
| Question | Answer |
|---|---|
| Who uses it first? | Me + some classmates (so accounts + cloud backup are needed) |
| Monthly budget | ₱0: free tiers only |
| Offline? | Yes: logging must work with no internet |
| Platform | Android first |
| Tech stack | Expo + TypeScript, Supabase, Gemini API (see architecture.md) |

## Caveats
- Baon and dorm prices come from blogs and listings, not official statistics.
- Free-tier limits and terms for Supabase and Gemini change. Verify them before inviting testers.
