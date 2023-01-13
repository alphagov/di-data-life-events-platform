# Dependency Management

As a general aim, we should be keeping dependencies up to date, and where possible automating this to reduce toil.

Dependabot is set up on GitHub to raise out of date dependencies automatically as PRs. On a regular basis, we should review this PRs and assess
- for minor/patch changes with a reasonably safe looking changelog and a set of passing tests, merge them
- for large changes, or those with a less safe looking changelog, discuss within the team
if after folding changes in the builds fail/there's an issue our tests didn't flag, revert the change in Git and raise for discussion in the team.
The best outcome here is that we extend our test coverage and have a more robust system going forward.
