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
  await assertFails(unauthDb.collection("discoveryPosts").where("published", "==", true).get());
  await assertFails(unauthDb.collection("products").where("active", "==", true).get());
});

test("User profile & preferences: owner create/partial update succeeds, cross-user & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Style",
      username: "alice_style",
      email: "alice@example.com",
      avatarStyle: "Streetwear",
      preferredStyles: ["Minimalist", "Streetwear", "Y2K"],
      preferredColors: ["Charcoal", "Cream", "Lime"],
      avoidColors: ["Orange"],
      preferredCategories: ["JACKETS", "TROUSERS"],
      preferredWeatherStyles: ["Cool", "Rainy"],
      preferredOccasions: ["Casual day", "Weekend"],
      preferredFootwear: ["Low-tops", "Sneakers"],
      preferredAccessories: ["Cap", "Watch"],
      budgetPreference: "Balanced everyday",
      inspirationFocus: "Unisex",
      preferredCurrency: "USD",
      preferredRegion: "United States",
      locationLabel: "New York, NY",
      themeMode: "LIGHT",
      privateProfile: true,
      minorAccount: false,
      notificationsEnabled: false,
      locationAccessPreference: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      themeMode: "DARK",
      budgetPreference: "Budget-friendly",
      hasPersonalPhoto: true,
      guideCompleted: true,
      avatarConfig: {
        presentation: "Feminine",
        hairStyle: "Box Braids",
        topStyle: "Oversized Tee",
        topColor: "Cream",
        aestheticPreset: "Y2K",
      },
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: pastDate(500) }));

  // Shadow update with ghost field rejected
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

test("Username uniqueness registry enforces unique handles, denies listing, and restricts deletion to owner", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  // Alice claims @alice_fit
  await assertSucceeds(
    aliceDb.collection("usernames").doc("alice_fit").set({
      userId: ALICE_UID,
      createdAt: now,
    })
  );

  // Unauthenticated duplicate check get is permitted (no PII stored)
  await assertSucceeds(unauthDb.collection("usernames").doc("alice_fit").get());
  // Listing all usernames is strictly blocked
  await assertFails(unauthDb.collection("usernames").get());
  await assertFails(bobDb.collection("usernames").get());

  // Bob cannot hijack or overwrite Alice's existing username
  await assertFails(
    bobDb.collection("usernames").doc("alice_fit").set({
      userId: BOB_UID,
      createdAt: now,
    })
  );
  await assertFails(bobDb.collection("usernames").doc("alice_fit").delete());

  // Invalid username formatting rejected
  await assertFails(
    bobDb.collection("usernames").doc("ab").set({
      userId: BOB_UID,
      createdAt: now,
    })
  );
});

test("Legal acceptances & wardrobe items: Kotlin create, partial update, @DocumentId guard, cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("legalAcceptances").doc("v1_consent").set({
      userId: ALICE_UID,
      privacyPolicyVersion: "1.0",
      termsVersion: "1.0",
      communityGuidelinesVersion: "1.0",
      cameraConsent: false,
      photoConsent: true,
      locationConsent: false,
      notificationsConsent: false,
      acceptedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wardrobeItems").doc("item_1").set({
      userId: ALICE_UID,
      wardrobeId: "default",
      name: "Structured Wool Blazer",
      category: "JACKETS",
      subType: "Blazer",
      color: "Charcoal",
      pattern: "Solid",
      material: "Wool",
      texture: "Brushed knit",
      finish: "Relaxed drape",
      style: "Minimalist",
      garmentType: "Blazer",
      notes: "Tailored everyday layer",
      favorite: true,
      privateItem: true,
      collectionTags: ["Work", "Minimalist"],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wardrobeItems").doc("item_1").update({
      name: "Oversized Charcoal Wool Blazer",
      favorite: false,
      updatedAt: pastDate(1000),
    })
  );

  // Reject @DocumentId 'id' inside payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("wardrobeItems").doc("item_2").set({
      id: "item_2",
      userId: ALICE_UID,
      wardrobeId: "default",
      name: "Invalid ID Field",
      category: "TOPS",
      color: "Black",
      favorite: false,
      privateItem: true,
      collectionTags: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject invalid category
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("wardrobeItems").doc("item_3").set({
      userId: ALICE_UID,
      wardrobeId: "default",
      name: "Invalid Cat",
      category: "UNKNOWN_CAT",
      color: "Black",
      favorite: false,
      privateItem: true,
      collectionTags: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user wardrobe access fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("wardrobeItems").get());
});

test("Outfits, AI conversations/messages, generated previews, and saved items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("outfits").doc("outfit_1").set({
      userId: ALICE_UID,
      name: "Gallery Opening Look",
      occasion: "Dinner",
      vibe: "Clean fit",
      notes: "Layer blazer over crisp white tee",
      topItemId: "item_top",
      bottomItemId: "item_bottom",
      shoesItemId: "item_shoes",
      jacketItemId: "item_1",
      accessoryIds: ["acc_1"],
      itemNames: ["White Tee", "Slate Trousers", "Charcoal Blazer"],
      colorPalette: ["Cream", "Grey", "Charcoal"],
      itemColorOverrides: { TOP: "Cream", BOTTOM: "Black" },
      feedbackStatus: "LIKED",
      favorite: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("aiConversations").doc("conv_1").set({
      userId: ALICE_UID,
      title: "Birthday Dinner Styling",
      occasion: "Birthday",
      vibe: "Smart casual",
      lastMessagePreview: "Here is a clean layered look...",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("aiConversations").doc("conv_1").collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "ASSISTANT",
      text: "Pair your charcoal wool blazer with tailored trousers.",
      recommendedItemNames: ["Charcoal Blazer", "Slate Trousers"],
      stylingReason: "Balanced monochrome silhouette for cool evening weather.",
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savedItems").doc("save_1").set({
      userId: ALICE_UID,
      targetType: "DISCOVERY_POST",
      targetId: "post_1",
      title: "Monochrome Tailoring",
      category: "Minimalist",
      liked: true,
      createdAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("outfits").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("aiConversations").doc("conv_1").collection("messages").get());
});

test("Shared discoveryPosts, products, and reports enforce query filters and authorship", async () => {
  const now = pastDate(10000);
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
    await adminDb.collection("discoveryPosts").doc("post_draft").set({
      authorId: BOB_UID,
      authorName: "Bob",
      title: "Draft",
      description: "Unpublished draft",
      category: "Casual",
      sourceType: "COMMUNITY",
      published: false,
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

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Filtered queries matching rules succeed
  await assertSucceeds(aliceDb.collection("discoveryPosts").where("published", "==", true).get());
  await assertSucceeds(aliceDb.collection("products").where("active", "==", true).get());

  // Unfiltered list queries fail
  await assertFails(aliceDb.collection("discoveryPosts").get());
  await assertFails(aliceDb.collection("products").get());
  await assertFails(aliceDb.collection("discoveryPosts").doc("post_draft").get());

  // Reports: Alice can create & list her own, cannot read Bob's
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetType: "DISCOVERY_POST",
      targetId: "post_pub",
      reason: "Copyright inquiry",
      status: "SUBMITTED",
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(aliceDb.collection("reports").where("reporterId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("reports").get());
});
