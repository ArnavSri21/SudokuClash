# Obsidian Sudoku Security Specification

## 1. Data Invariants
1. **Authentication Mandatory**: Every read and write operation requires an authenticated user (`request.auth != null`).
2. **Strict Tenant Isolation**: Every user document and subcollection under `/users/{userId}/...` (`sessions`, `daily_challenges`, `stickers`, `game_stats`) is strictly accessible only when `request.auth.uid == userId` and `request.resource.data.userId == request.auth.uid`.
3. **Path Variable Hardening**: All document IDs (`userId`, `sessionId`, `dateString`, `stickerId`, `difficulty`) must match `^[a-zA-Z0-9_\-]+$` and have length `<= 128`.
4. **Immutable Creation Metadata**: `userId` and `createdAt` cannot be modified on `update` (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
5. **Temporal Integrity**: `createdAt` and `updatedAt` must be server timestamps (`is timestamp && <= request.time`).
6. **Strict Schema & Bounds**: Every string field enforces `.size() <= MAX`, every numeric counter enforces `is int && >= 0`, and `keys().hasOnly(...)` blocks ghost fields.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `auth = null` attempting `get` or `list` on `/users/alice/sessions` -> REJECTED.
2. **Cross-User Read**: `auth.uid = "bob"` attempting `get` or `list` on `/users/alice/sessions` -> REJECTED.
3. **Cross-User Write**: `auth.uid = "bob"` attempting `create` on `/users/alice/sessions/active` -> REJECTED.
4. **Identity Spoofing in Payload**: `auth.uid = "alice"` writing `{"userId": "bob", ...}` to `/users/alice/sessions/active` -> REJECTED.
5. **Shadow Update (Ghost Field)**: `auth.uid = "alice"` updating `/users/alice/game_stats/EASY` with `{"isAdmin": true}` -> REJECTED by `hasOnly()`.
6. **Immortal Field Mutation**: `auth.uid = "alice"` updating `createdAt` or `userId` on an existing session -> REJECTED.
7. **Future Timestamp Injection**: `auth.uid = "alice"` submitting `createdAt` in the future (`> request.time`) -> REJECTED.
8. **Type Poisoning**: `auth.uid = "alice"` setting `timerSeconds: "fast"` (string instead of int) -> REJECTED by `isValidGameSession()`.
9. **Negative Counter Injection**: `auth.uid = "alice"` setting `mistakes: -5` or `gamesWon: -1` -> REJECTED.
10. **Oversized String DoS**: `auth.uid = "alice"` submitting a 10,000-char `boardData` string -> REJECTED by `.size() <= 2000`.
11. **Missing Required Field on Create**: `auth.uid = "alice"` creating a `Sticker` without `title` or `createdAt` -> REJECTED by `hasAll()`.
12. **Required Field Deletion on Update**: `auth.uid = "alice"` deleting `difficulty` via `FieldValue.delete()` -> REJECTED by `isValidGameSession(incoming())`.
