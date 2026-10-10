# Billing rules and open configuration

## Confirmed allowances
- Pickup/drop: 4 hours or 40 KM.
- Local: 8 hours or 80 KM.
- Outstation: 300 KM per day.
- Outstation extra distance: ₹12 per KM above allowance.

## Client-configurable rates
- Local base: ₹1,500, ₹1,600, ₹1,800 or ₹2,000, according to client agreement.
- Local extra hour: ₹100, ₹120, ₹200 or ₹300, according to client agreement.
- Local extra-KM rate: not confirmed; configure per client.
- Pickup/drop base and excess rates: not confirmed; configure per client.
- Outstation base daily rate: not confirmed; configure per client.

## Calculation requirements
- Distance is end odometer minus start odometer; reject negative values.
- Duration must support trips crossing midnight.
- Snapshot rate card and line items on finalized bills.
- Show toll and other expenses as separate line items.
- Do not finalize a bill when a required rate is missing.
- Test exact allowance boundaries and just-over-allowance cases.
