## 1.4.3 · 12 November 2026

- Remove the Google consent rule that automatically clicked internal button identifiers within generic dialogs after page load. Google consent dialogs now require manual interaction.
- Add a regression test reproducing the unwanted automatic click. All 61 JavaScript tests pass with the fix.

This addresses a suspected contributor to the grey, unresponsive Google page reported after searches. Resolution of that symptom on the affected device is not yet confirmed.
