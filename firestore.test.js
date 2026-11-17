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
const ADMIN_EMAIL = "mdazinuddin16@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validOfferPayload(userId, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    title: "H > 150 জিবি 30 দিন 898৳ অল বিডি সবাই পাবে",
    price: "898",
    buyPrice: "748",
    discount: "discount: ৳150",
    category: "INTERNET",
    badge: "All BD",
    rating: 4.9,
    description: "150 GB 30 day drive offer",
    featured: true,
    bookmarked: false,
    bagQuantity: 0,
    sortOrder: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validOrderPayload(userId, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    offerId: "drive_150gb_898",
    offerTitle: "H > 150 জিবি 30 দিন 898৳ অল বিডি সবাই পাবে",
    price: "898",
    buyPrice: "748",
    discount: "discount: ৳150",
    quantity: 1,
    totalPaid: 748,
    paymentMethod: "bKash",
    adminPaymentNumber: "01711000000",
    transactionId: "9K7A2BX4M1",
    customerPhone: "01711111111",
    status: "PLACED",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated user: cannot read or write offers or orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("offers").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("offers").doc("offer_1").set(
      validOfferPayload(ALICE_UID)
    )
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
});

test("Owner: valid offer create, partial bookmark/bag/admin updates, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const offerRef = aliceDb.collection("users").doc(ALICE_UID).collection("offers").doc("offer_1");

  await assertSucceeds(offerRef.set(validOfferPayload(ALICE_UID)));

  await assertSucceeds(
    offerRef.update({
      bookmarked: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    offerRef.update({
      bagQuantity: 2,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  await assertSucceeds(
    offerRef.update({
      title: "H > 200 জিবি 30 দিন নতুন অফার",
      price: "998",
      buyPrice: "798",
      discount: "discount: ৳200",
      category: "INTERNET",
      badge: "Updated",
      description: "Updated offer details",
      featured: false,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("offers").orderBy("sortOrder").get()
  );

  await assertSucceeds(offerRef.delete());
});

test("Security bounds: reject ghost fields, @DocumentId id key, future timestamps, and immutable createdAt change", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const offersCol = aliceDb.collection("users").doc(ALICE_UID).collection("offers");

  await assertFails(
    offersCol.doc("ghost_1").set(validOfferPayload(ALICE_UID, { isVerified: true }))
  );

  await assertFails(
    offersCol.doc("with_id").set(validOfferPayload(ALICE_UID, { id: "with_id" }))
  );

  await assertFails(
    offersCol.doc("future_ts").set(
      validOfferPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) })
    )
  );

  await assertSucceeds(offersCol.doc("offer_immutable").set(validOfferPayload(ALICE_UID)));
  await assertFails(
    offersCol.doc("offer_immutable").update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice's offers or orders", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).collection("offers").doc("offer_1").set(
      validOfferPayload(ALICE_UID)
    );
    await db.collection("users").doc(ALICE_UID).collection("orders").doc("order_1").set(
      validOrderPayload(ALICE_UID)
    );
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("offers").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("offers").doc("offer_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("offers").doc("offer_1").update({
      bookmarked: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").get());
});

test("Email spoofing test: unverified admin email cannot read Alice's offers, verified admin can", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("offers").doc("offer_1").set(
      validOfferPayload(ALICE_UID)
    );
  });

  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofedAdminDb.collection("users").doc(ALICE_UID).collection("offers").get());

  const verifiedAdminDb = testEnv.authenticatedContext("real_admin_uid", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(verifiedAdminDb.collection("users").doc(ALICE_UID).collection("offers").get());
});

test("Orders: owner can place PLACED order and list orders; non-admin cannot mutate status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1");

  await assertSucceeds(orderRef.set(validOrderPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("orders").get());

  await assertFails(
    orderRef.update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("UserProfile: owner can register as USER, update allowed fields, but cannot escalate role or read other profiles", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceProfileRef = aliceDb.collection("users").doc(ALICE_UID);
  const now = new Date(Date.now() - 1000);

  await assertFails(
    aliceProfileRef.set({
      userId: ALICE_UID,
      fullName: "Alice",
      phone: "01711111111",
      division: "Dhaka",
      email: "alice@example.com",
      role: "ADMIN",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceProfileRef.set({
      userId: ALICE_UID,
      fullName: "Alice Rahman",
      phone: "01711111111",
      division: "Dhaka",
      email: "alice@example.com",
      role: "USER",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceProfileRef.get());
  await assertSucceeds(
    aliceProfileRef.update({
      fullName: "Alice R.",
      phone: "01722222222",
      division: "Barishal",
      email: "alice@example.com",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(
    aliceProfileRef.update({
      role: "ADMIN",
      updatedAt: new Date(Date.now() - 400),
    })
  );
});

test("PaymentSettings: verified admin can configure payment numbers; signed-in user can read; non-admin and unauth cannot write", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext("real_admin_uid", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const settingsPayload = {
    bkashNumber: "01711000000",
    nagadNumber: "01811000000",
    rocketNumber: "",
    instructions: "Send Money করে TrxID বসান",
    updatedBy: "real_admin_uid",
    updatedAt: new Date(Date.now() - 1000),
  };

  await assertFails(unauthDb.collection("app_config").doc("payment_settings").get());
  await assertFails(aliceDb.collection("app_config").doc("payment_settings").set({
    ...settingsPayload,
    updatedBy: ALICE_UID,
  }));

  await assertSucceeds(adminDb.collection("app_config").doc("payment_settings").set(settingsPayload));
  await assertSucceeds(aliceDb.collection("app_config").doc("payment_settings").get());
  await assertFails(aliceDb.collection("app_config").get());
});
