---
title: Remove EclipseStore storage backend
status: accepted
date: 2026-06-17
---

# Context

The gateway previously supported EclipseStore-backed route, audit, changelog, and validation storage implementations. The code now has database and Azure Table Storage backends, and EclipseStore is no longer needed.

# Decision

Remove all EclipseStore implementation classes, support files, test fixtures, and configuration wiring from the application.

# Consequences

- The supported storage backends are now `database` and `azure-table`.
- EclipseStore-specific YAML files and configuration classes are no longer part of the build.
- The storage layer does not provide distributed concurrency control; concurrent updates must be coordinated outside the storage backend.

# Rationale

- Simplifies the codebase by removing a legacy storage path.
- Reduces maintenance and build complexity.
- Keeps Azure Table Storage as the cloud-native option without reintroducing EclipseStore-specific assumptions.

# Notes

The application should not rely on the storage backend to handle cross-node locking or conflict resolution.
