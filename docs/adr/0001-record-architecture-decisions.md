# 1. Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-11-18

## Context

The project has grown over more than a year. Many decisions are only visible in the code and in commit messages.
New contributors (and the owner, later) need to know why things are the way they are.

## Decision

We record architecture decisions as ADRs in `docs/adr`, using the format by Michael Nygard
(Context, Decision, Consequences). ADRs 0002–0015 were written afterwards from the code and git history. Their
dates are the dates the decision first appeared in the code.

## Consequences

- New significant decisions get a new ADR with the next number.
- A decision that is replaced is not deleted. Its status changes to "Superseded by ADR-XXXX".
- Where a reason could not be found in the code or history, the ADR says so.
