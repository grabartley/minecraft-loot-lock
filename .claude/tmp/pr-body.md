Implements rejected-item delete enforcement behind server policy.

**What's included:**
- Adds server policy loading for `allowDeleteRejectedItems` from `lootlock/server-policy.json`
- Wires policy into `PickupGuard` so rejected delete decisions downgrade to leave-on-ground when disabled
- Blocks `/lootlock profile set action delete` when policy disables delete mode
- Adds unit tests for policy loading, pickup decision downgrade, and action normalization

Closes #36
