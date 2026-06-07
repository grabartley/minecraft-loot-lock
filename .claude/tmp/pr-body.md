Fix Item Search multi-select so all selected rules are persisted in one save request.

**What's included:**
- add `RuleListController.withRulesAdded(...)` to batch-append rule ids while preserving existing dedupe behavior and selection order
- add `RuleListScreen.addRules(...)` so Item Search can save all selected items through one draft/save flow
- update `ItemSearchScreen.addSelected()` to submit the full selection at once and show the actual number of newly added items
- add unit coverage for batched rule adds in `RuleListControllerTest`

Closes #101
