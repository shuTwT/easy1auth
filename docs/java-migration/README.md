# Java migration baseline

This specification was captured from `docs/goal`, the Vue API modules, Express routes, and the Prisma schema. Code is the compatibility baseline when it conflicts with the old README.

- [API inventory](api-inventory.md)
- [Data mapping](data-mapping.md)
- [Security and permission matrix](security-matrix.md)
- [Acceptance criteria](acceptance.md)
- [Phase 7 deployment and cutover runbook](phase-7-runbook.md)
- [Phase 7 implementation status](phase-7-status.md)

No SQLite business-data importer will be built. The old database and configuration are archive-only, and all old admin/OAuth tokens expire at cutover.
