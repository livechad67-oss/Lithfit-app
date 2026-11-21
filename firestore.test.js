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
const OWNER_UID = "owner_clastad";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated users cannot read or write any LITHFIT collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("wardrobeItems").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("calendarEntries").get());
  await assertFails(unauthDb.collection("ownerNotifications").get());
  await assertFails(unauthDb.collection("discoveryPosts").where("published", "==", true).get());
  await assertFails(unauthDb.collection("products").where("active", "==", true).get());
});

test("User profile & preferences: expanded fields succeed, cross-user & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Style",
      username: "alice_style",
      email: "alice@example.com",
      phone: "+15551234567",
      profilePhotoPrivate: true,
      avatarStyle: "Streetwear",
      preferredStyles: ["Minimalist", "Streetwear", "Y2K"],
      personalStyleTags: ["Clean editorial"],
      preferredColors: ["Charcoal", "Cream"],
      frequentColors: ["Black", "Cream"],
      neutralCombos: ["Warm Cream & Charcoal"],
      experimentColors: ["Olive"],
      avoidColors: ["Orange"],
      preferredCategories: ["JACKETS", "TROUSERS"],
      preferredSilhouettes: ["Baggy / Oversized"],
      enjoyedCategories: ["JACKETS"],
      rarelyWornCategories: ["SKIRTS"],
      preferredWeatherStyles: ["Cool", "Rainy"],
      preferredOccasions: ["Casual day", "Workwear", "Weekend"],
      preferredFootwear: ["Low-tops", "Sneakers"],
      preferredAccessories: ["Cap", "Watch"],
      lifestyleTags: ["Creative Studio"],
      budgetPreference: "Balanced everyday",
      inspirationFocus: "Unisex",
      preferredCurrency: "USD",
      preferredRegion: "United States",
      locationLabel: "New York, NY",
      themeMode: "LIGHT",
      language: "English",
      reducedMotion: false,
      largeReadableText: false,
      privateProfile: true,
      minorAccount: false,
      notificationsEnabled: true,
      notifOutfitRecs: true,
      notifDailyReminders: true,
      notifWardrobeReminders: true,
      notifSecurityAlerts: true,
      notifAppUpdates: false,
      notifProductUpdates: false,
      locationAccessPreference: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      themeMode: "DARK",
      language: "French",
      reducedMotion: true,
      largeReadableText: true,
      notifAppUpdates: true,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: pastDate(500) }));

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Tampered future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future",
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});

test("Owner-only registration notifications are readable ONLY by verified livechad67@gmail.com", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const unverifiedOwnerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: "livechad67@gmail.com",
    email_verified: false,
  }).firestore();
  const verifiedOwnerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: "livechad67@gmail.com",
    email_verified: true,
  }).firestore();

  const now = pastDate(3000);
  await assertSucceeds(
    aliceDb.collection("ownerNotifications").doc("owner_reg_alice_123").set({
      newUserId: ALICE_UID,
      username: "alice_style",
      displayName: "Alice Style",
      authMethod: "GOOGLE",
      recipientOwnerEmail: "livechad67@gmail.com",
      message: "New user joined LITHFIT! @alice_style",
      read: false,
      createdAt: now,
    })
  );

  // Normal users & unverified owner claims cannot read or list registration notifications
  await assertFails(aliceDb.collection("ownerNotifications").get());
  await assertFails(aliceDb.collection("ownerNotifications").doc("owner_reg_alice_123").get());
  await assertFails(unverifiedOwnerDb.collection("ownerNotifications").get());

  // Verified owner can read, list, and mark read
  await assertSucceeds(verifiedOwnerDb.collection("ownerNotifications").get());
  await assertSucceeds(verifiedOwnerDb.collection("ownerNotifications").doc("owner_reg_alice_123").update({ read: true }));
});

test("Calendar entries, wear history, and notifications enforce owner isolation & schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("calendarEntries").doc("2026_11_21").set({
      userId: ALICE_UID,
      dateKey: "2026-11-21",
      outfitId: "outfit_1",
      outfitName: "Studio Cream Blazer",
      occasion: "Workwear",
      vibe: "Minimalist",
      notes: "Morning meeting",
      worn: false,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("outfitHistory").doc("hist_1").set({
      userId: ALICE_UID,
      outfitId: "outfit_1",
      outfitName: "Studio Cream Blazer",
      occasion: "Workwear",
      vibe: "Minimalist",
      itemIds: ["item_1", "item_2"],
      itemNames: ["Cream Tee", "Charcoal Blazer"],
      wornDateKey: "2026-11-21",
      wornAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      category: "DAILY_REMINDER",
      title: "Today's Look Ready",
      body: "Balanced warm cream and charcoal pairing ready.",
      read: false,
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").update({ read: true })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("calendarEntries").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("outfitHistory").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notifications").get());
});

test("Username registry enforces unique handles, denies listing, and restricts deletion", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("usernames").doc("alice_fit").set({
      userId: ALICE_UID,
      createdAt: now,
    })
  );
  await assertSucceeds(unauthDb.collection("usernames").doc("alice_fit").get());
  await assertFails(unauthDb.collection("usernames").get());
  await assertFails(bobDb.collection("usernames").get());
  await assertFails(bobDb.collection("usernames").doc("alice_fit").delete());
});

test("Wardrobe items, outfits, discoveryPosts, products, and reports enforce isolation & query filters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wardrobeItems").doc("item_1").set({
      userId: ALICE_UID,
      wardrobeId: "default",
      name: "Structured Wool Blazer",
      category: "JACKETS",
      color: "Charcoal",
      favorite: true,
      privateItem: true,
      collectionTags: ["Workwear", "Minimalist"],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("outfits").doc("outfit_1").set({
      userId: ALICE_UID,
      name: "Gallery Opening Look",
      occasion: "Dinner",
      vibe: "Minimalist",
      accessoryIds: ["acc_1"],
      itemNames: ["White Tee", "Slate Trousers", "Charcoal Blazer"],
      colorPalette: ["Cream", "Grey", "Charcoal"],
      favorite: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("wardrobeItems").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("outfits").get());

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("discoveryPosts").doc("post_pub").set({
      authorId: BOB_UID,
      authorName: "LITHFIT Editorial",
      title: "Architectural Tailoring",
      description: "Clean monochrome lines for cool days.",
      category: "Minimalist",
      sourceType: "EDITORIAL",
      published: true,
      likesCount: 0,
      createdAt: now,
      updatedAt: now,
    });
    await adminDb.collection("products").doc("prod_1").set({
      sellerOwnerId: BOB_UID,
      sellerId: "seller_1",
      sellerName: "Atelier Nord",
      name: "Boxy Merino Hoodie",
      category: "HOODIES",
      color: "Black",
      style: "Streetwear",
      price: 110,
      currency: "USD",
      sizes: ["S", "M", "L"],
      availabilityStatus: "IN_STOCK",
      active: true,
      createdAt: now,
    });
  });

  await assertSucceeds(aliceDb.collection("discoveryPosts").where("published", "==", true).get());
  await assertSucceeds(aliceDb.collection("products").where("active", "==", true).get());
  await assertFails(aliceDb.collection("discoveryPosts").get());
  await assertFails(aliceDb.collection("products").get());
});
