const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID = "admin_uid_789";
const ADMIN_EMAIL = "creativegx4@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function adminContext(verified = true) {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: verified,
  });
}

function customerContext(uid, email) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: true,
  });
}

test("Unauthenticated users cannot read or write products, orders, customers, or invoices", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("customers").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("invoices").get());
  await assertFails(
    unauthDb.collection("products").doc("stretch_film").set({
      name: "Stretch Film",
      categoryId: "cat_films",
      categoryName: "Stretch Films",
      description: "High-quality stretch film",
      priceInr: 499,
      unit: "Roll",
      stockQty: 120,
      sku: "UW-SF-01",
      active: true,
      featured: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Customer profile, addresses, and cart enforce strict owner isolation", async () => {
  const aliceDb = customerContext(ALICE_UID, "alice@example.com").firestore();
  const bobDb = customerContext(BOB_UID, "bob@example.com").firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("customers").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Patel",
      email: "alice@example.com",
      mobileNumber: "9876543210",
      wishlistProductIds: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update succeeds for owner
  await assertSucceeds(
    aliceDb.collection("customers").doc(ALICE_UID).update({
      companyName: "Patel Logistics",
      wishlistProductIds: ["stretch_film"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot read, list, or update Alice's profile
  await assertFails(bobDb.collection("customers").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("customers").get());
  await assertFails(
    bobDb.collection("customers").doc(ALICE_UID).update({
      fullName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Address CRUD works for Alice and fails for Bob
  await assertSucceeds(
    aliceDb
      .collection("customers")
      .doc(ALICE_UID)
      .collection("addresses")
      .doc("addr_1")
      .set({
        userId: ALICE_UID,
        label: "Warehouse",
        recipientName: "Alice Patel",
        mobileNumber: "9876543210",
        addressLine: "Plot 14, GIDC Phase 2",
        city: "Ahmedabad",
        state: "Gujarat",
        pincode: "382445",
        defaultAddress: true,
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertSucceeds(
    aliceDb.collection("customers").doc(ALICE_UID).collection("addresses").get()
  );
  await assertFails(
    bobDb.collection("customers").doc(ALICE_UID).collection("addresses").get()
  );

  // Cart CRUD works for Alice and fails for Bob
  await assertSucceeds(
    aliceDb
      .collection("customers")
      .doc(ALICE_UID)
      .collection("cart")
      .doc("stretch_film")
      .set({
        userId: ALICE_UID,
        productId: "stretch_film",
        productName: "Stretch Film",
        unit: "Roll",
        unitPrice: 499,
        quantity: 2,
        drawableKey: "img_stretch_film",
        updatedAt: now,
      })
  );
  await assertFails(
    bobDb.collection("customers").doc(ALICE_UID).collection("cart").get()
  );
});

test("Products allow initial official seed creation once, then only Admins can modify or delete", async () => {
  const aliceDb = customerContext(ALICE_UID, "alice@example.com").firestore();
  const admDb = adminContext(true).firestore();
  const now = new Date(Date.now() - 5000);

  // Official seed product creation succeeds once
  await assertSucceeds(
    aliceDb.collection("products").doc("stretch_film").set({
      name: "Stretch Film",
      categoryId: "cat_films",
      categoryName: "Stretch Films",
      description: "High-quality stretch film for secure packaging and pallet wrapping.",
      priceInr: 499,
      unit: "Roll",
      stockQty: 150,
      sku: "UW-SF-01",
      drawableKey: "img_stretch_film",
      active: true,
      featured: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot update price or stock, nor create arbitrary products, nor delete
  await assertFails(
    aliceDb.collection("products").doc("stretch_film").update({
      priceInr: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("products").doc("hacked_item").set({
      name: "Fake",
      categoryId: "cat_films",
      categoryName: "Stretch Films",
      description: "Fake",
      priceInr: 1,
      unit: "Roll",
      stockQty: 1,
      sku: "FAKE",
      active: true,
      featured: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(aliceDb.collection("products").doc("stretch_film").delete());

  // Verified Admin can update product price and create custom products
  await assertSucceeds(
    admDb.collection("products").doc("stretch_film").update({
      priceInr: 549,
      stockQty: 200,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    admDb.collection("products").doc("custom_pallet_wrap").set({
      name: "Heavy Pallet Wrap",
      categoryId: "cat_films",
      categoryName: "Stretch Films",
      description: "Extra thick pallet wrap",
      priceInr: 799,
      unit: "Roll",
      stockQty: 80,
      sku: "UW-PW-02",
      active: true,
      featured: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Email spoofing and privilege escalation attacks are blocked", async () => {
  const unverifiedAdminDb = adminContext(false).firestore();
  const aliceDb = customerContext(ALICE_UID, "alice@example.com").firestore();
  const now = new Date(Date.now() - 5000);

  // Unverified email claiming to be admin cannot write admin collections
  await assertFails(
    unverifiedAdminDb.collection("books").doc("main_book").set({
      name: "Main Book",
      code: "MAIN",
      invoiceCount: 0,
      totalBilled: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot promote themselves into admin_users
  await assertFails(
    aliceDb.collection("admin_users").doc(ALICE_UID).set({
      email: "alice@example.com",
      displayName: "Alice",
      role: "admin",
      active: true,
      createdAt: now,
    })
  );

  // Customer cannot access books or invoices
  await assertFails(aliceDb.collection("books").get());
  await assertFails(aliceDb.collection("invoices").get());
});

test("Orders enforce scoped queries, terminal state locking, and admin status updates", async () => {
  const aliceDb = customerContext(ALICE_UID, "alice@example.com").firestore();
  const bobDb = customerContext(BOB_UID, "bob@example.com").firestore();
  const admDb = adminContext(true).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("orders").doc("order_1").set({
      orderNumber: "UW260001",
      userId: ALICE_UID,
      customerName: "Alice Patel",
      customerEmail: "alice@example.com",
      customerMobile: "9876543210",
      deliveryAddress: "Plot 14, GIDC",
      city: "Ahmedabad",
      state: "Gujarat",
      pincode: "382445",
      paymentMethod: "Cash on Delivery",
      items: [
        {
          productId: "stretch_film",
          productName: "Stretch Film",
          unit: "Roll",
          unitPrice: 499,
          quantity: 2,
          lineTotal: 998,
        },
      ],
      subtotal: 998,
      discount: 0,
      totalAmount: 998,
      status: "PROCESSING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Scoped query succeeds; unfiltered query fails for customer
  await assertSucceeds(
    aliceDb.collection("orders").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("orders").get());
  await assertFails(bobDb.collection("orders").doc("order_1").get());

  // Customer cannot confirm or mark their own order delivered
  await assertFails(
    aliceDb.collection("orders").doc("order_1").update({
      status: "DELIVERED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin can list all orders and update status
  await assertSucceeds(admDb.collection("orders").get());
  await assertSucceeds(
    admDb.collection("orders").doc("order_1").update({
      status: "SHIPPED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Once SHIPPED, customer cannot cancel
  await assertFails(
    aliceDb.collection("orders").doc("order_1").update({
      status: "CANCELLED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Admin Direct Book Billing creates books and invoices securely", async () => {
  const admDb = adminContext(true).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    admDb.collection("books").doc("main_book").set({
      name: "Main Book",
      code: "MAIN",
      description: "Primary company billing ledger",
      invoiceCount: 0,
      totalBilled: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    admDb.collection("invoices").doc("inv_1").set({
      invoiceNumber: "INV-2026-001",
      bookId: "main_book",
      bookName: "Main Book",
      customerName: "Rohan Sharma",
      customerMobile: "9898989898",
      items: [
        {
          productId: "stretch_film",
          productName: "Stretch Film",
          unit: "Roll",
          unitPrice: 499,
          quantity: 10,
          lineTotal: 4990,
        },
      ],
      subtotal: 4990,
      discount: 100,
      gstPercent: 18,
      gstAmount: 880.2,
      grandTotal: 5770.2,
      paymentStatus: "PAID",
      createdByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
});
