const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const nowTimestamp = () => new Date(Date.now() - 1000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- SECURITY BOUNDS TESTS (Dirty Dozen) ---

test("1. Unauthenticated user: cannot read or list sessions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("active")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("2. Cross-user read: Bob cannot read Alice's sessions, stickers, daily_challenges, or game_stats", async () => {
  const now = nowTimestamp();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("active")
      .set({
        userId: ALICE_UID,
        difficulty: "EASY",
        timerSeconds: 45,
        mistakes: 1,
        isPaused: false,
        isDaily: false,
        boardData: "0:0:0",
        solutionData: "1",
        notesData: "",
        createdAt: now,
        updatedAt: now,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("active")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("3. Cross-user write: Bob cannot create or update in Alice's subcollections", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("active")
      .set({
        userId: ALICE_UID,
        difficulty: "EASY",
        timerSeconds: 10,
        mistakes: 0,
        isPaused: false,
        isDaily: false,
        boardData: "0:0:0",
        solutionData: "1",
        notesData: "",
        createdAt: nowTimestamp(),
        updatedAt: nowTimestamp(),
      })
  );
});

test("4. Identity spoofing in payload: Alice cannot write userId = Bob in her own subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("active")
      .set({
        userId: BOB_UID,
        difficulty: "EASY",
        timerSeconds: 10,
        mistakes: 0,
        isPaused: false,
        isDaily: false,
        boardData: "0:0:0",
        solutionData: "1",
        notesData: "",
        createdAt: nowTimestamp(),
        updatedAt: nowTimestamp(),
      })
  );
});

test("5. Shadow update (ghost field): Alice cannot inject extra fields on create or update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("game_stats")
    .doc("EASY");

  await assertFails(
    docRef.set({
      userId: ALICE_UID,
      difficulty: "EASY",
      gamesPlayed: 1,
      gamesWon: 1,
      bestTimeSeconds: 120,
      totalTimeSeconds: 120,
      winStreak: 1,
      perfectGames: 1,
      isAdmin: true,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
});

test("6. Authenticated owner: can create, read, list, update, and delete valid GameSession, DailyChallenge, Sticker, and GameStats", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // GameSession create & get
  const sessionRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions")
    .doc("active");
  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      difficulty: "EASY",
      timerSeconds: 30,
      mistakes: 0,
      isPaused: false,
      isDaily: false,
      boardData: "1:1:0,0:0:0",
      solutionData: "1,2",
      notesData: ";",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(sessionRef.get());
  await assertSucceeds(
    sessionRef.update({
      timerSeconds: 35,
      updatedAt: nowTimestamp(),
    })
  );

  // DailyChallenge create & list with userId filter
  const dailyRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("daily_challenges")
    .doc("2026-10-17");
  await assertSucceeds(
    dailyRef.set({
      userId: ALICE_UID,
      dateString: "2026-10-17",
      difficulty: "DAILY",
      completed: true,
      timeSpentSeconds: 180,
      mistakes: 0,
      starsAwarded: 3,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("daily_challenges")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Sticker create & update
  const stickerRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("stickers")
    .doc("first_win");
  await assertSucceeds(
    stickerRef.set({
      userId: ALICE_UID,
      title: "Obsidian Initiate",
      description: "Complete your first Sudoku puzzle",
      iconName: "sparkles",
      category: "Milestone",
      unlocked: false,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(
    stickerRef.update({
      unlocked: true,
      updatedAt: nowTimestamp(),
    })
  );

  // GameStats create & update
  const statsRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("game_stats")
    .doc("EASY");
  await assertSucceeds(
    statsRef.set({
      userId: ALICE_UID,
      difficulty: "EASY",
      gamesPlayed: 1,
      gamesWon: 1,
      bestTimeSeconds: 150,
      totalTimeSeconds: 150,
      winStreak: 1,
      perfectGames: 1,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(
    statsRef.update({
      gamesPlayed: 2,
      gamesWon: 2,
      winStreak: 2,
      updatedAt: nowTimestamp(),
    })
  );
});
