# Billing rules and configuration

## Confirmed allowances
- Pickup/drop: 4 hours or 40 KM.
- Local: 8 hours or 80 KM.
- Outstation: 300 KM per day.
- Outstation extra distance: ₹12/km above the 300 KM allowance.

## Client-configurable rates
- Local base: choose the rate agreed with the client (known examples: ₹1,500, ₹1,600, ₹1,800 or ₹2,000).
- Local extra hour: choose the rate agreed with the client (known examples: ₹100, ₹120, ₹200 or ₹300).
- Local extra-KM rate: not confirmed; configure per client.
- Pickup/drop base and excess rates: not confirmed; configure per client.
- Outstation base daily rate: not confirmed; configure per client.
- Outstation extra-KM rate defaults to the known ₹12/km and can be explicitly changed on a client's rate card.

## Implemented calculation behavior
- Distance is end odometer minus start odometer; negative distance is rejected.
- Duty duration supports trips crossing midnight.
- The configured base rate is charged once per recorded trip.
- For pickup/drop and local trips, time beyond the allowance is prorated per minute from the configured hourly rate. Excess distance is charged independently if a rate is configured.
- Outstation billing is supported for a single recorded duty of up to 24 hours. Longer trips are blocked until day-wise distance entry is implemented.
- Missing rates block invoice creation rather than silently guessing.
- Toll and other expenses are shown as separate invoice line items.
- Invoice snapshots store line items and total so later rate-card edits do not alter old invoices.

## Important product decision still needed
The original package wording says “4 hours or 40 KM” and “8 hours or 80 KM”. This implementation charges excess time and excess distance independently if both allowances are exceeded. Confirm if the commercial agreement instead means whichever limit is reached first, with a different excess policy.
