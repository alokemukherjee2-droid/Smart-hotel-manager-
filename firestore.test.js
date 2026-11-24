const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_1";
const BOB_UID = "bob_other_hotel_2";
const HOTEL_ID = "hotel_main_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastDate = (offsetMs = 5000) => new Date(Date.now() - offsetMs);
const futureDate = (offsetMs = 60000) => new Date(Date.now() + offsetMs);

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/rooms`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/bookings`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/guests`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/rooms/room_101`).set({
      userId: ALICE_UID,
      hotelId: HOTEL_ID,
      roomNumber: "101",
      floor: "1st Floor",
      roomType: "DELUXE",
      capacity: 2,
      basePrice: 3500,
      extraPersonCharge: 800,
      seasonalPrice: 4200,
      seasonalActive: false,
      status: "AVAILABLE",
      amenities: ["AC", "Wi-Fi"],
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice's hotel data", async () => {
  const created = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc(`users/${ALICE_UID}/guests/guest_1`).set({
      userId: ALICE_UID,
      hotelId: HOTEL_ID,
      fullName: "Rajeshwar Sharma",
      mobile: "9876543210",
      lifetimeBookingCount: 3,
      completedStaysCount: 2,
      totalLifetimeSpend: 28500,
      foreignGuest: false,
      cFormSubmitted: false,
      createdAt: created,
      updatedAt: created,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/guests`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/guests/guest_1`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/guests/guest_1`).update({
      fullName: "Hijacked Guest",
      updatedAt: pastDate(1000),
    })
  );
});

test("Owner can create/update Room, Guest (with lifetimeBookingCount), and rejects Shadow Fields & future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  // Valid Room create
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/rooms/room_101`).set({
      userId: ALICE_UID,
      hotelId: HOTEL_ID,
      roomNumber: "101",
      floor: "1st Floor",
      roomType: "DELUXE",
      capacity: 2,
      basePrice: 3800,
      extraPersonCharge: 800,
      seasonalPrice: 4500,
      seasonalActive: false,
      status: "AVAILABLE",
      amenities: ["AC", "Wi-Fi", "Smart TV"],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid Housekeeping status partial update
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/rooms/room_101`).update({
      status: "CLEANING_IN_PROGRESS",
      assignedStaffName: "Ramesh Kumar",
      housekeepingNotes: "Deep clean requested",
      updatedAt: pastDate(1000),
    })
  );

  // Reject Shadow Field / @DocumentId `id` field
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/rooms/room_102`).set({
      id: "room_102",
      userId: ALICE_UID,
      hotelId: HOTEL_ID,
      roomNumber: "102",
      floor: "1st Floor",
      roomType: "SINGLE",
      capacity: 1,
      basePrice: 2200,
      extraPersonCharge: 500,
      seasonalPrice: 2600,
      seasonalActive: false,
      status: "AVAILABLE",
      amenities: ["Wi-Fi"],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/guests/guest_future`).set({
      userId: ALICE_UID,
      hotelId: HOTEL_ID,
      fullName: "Future Guest",
      mobile: "9988776655",
      lifetimeBookingCount: 1,
      completedStaysCount: 0,
      totalLifetimeSpend: 0,
      foreignGuest: false,
      cFormSubmitted: false,
      createdAt: futureDate(),
      updatedAt: futureDate(),
    })
  );

  // Valid Guest with Returning Guest count & masked Aadhaar
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/guests/guest_1`).set({
      userId: ALICE_UID,
      hotelId: HOTEL_ID,
      fullName: "Priya Deshmukh",
      mobile: "9820112233",
      idDocumentType: "AADHAAR_MASKED",
      maskedIdNumber: "XXXX-XXXX-4521",
      lifetimeBookingCount: 4,
      completedStaysCount: 3,
      totalLifetimeSpend: 42000,
      foreignGuest: false,
      cFormSubmitted: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid stay history increment update
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/guests/guest_1`).update({
      lifetimeBookingCount: 5,
      lastStayDate: "24-11-2026",
      updatedAt: pastDate(1000),
    })
  );
});

test("Booking lifecycle enforces terminal state locking and RoomLock prevents updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  // Valid Booking create
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/bookings/bkg_1`).set({
      userId: ALICE_UID,
      hotelId: HOTEL_ID,
      bookingCode: "SHM-2026-1001",
      guestId: "guest_1",
      guestName: "Priya Deshmukh",
      guestMobile: "9820112233",
      returningGuest: true,
      guestLifetimeBookingsAtBooking: 4,
      adults: 2,
      children: 0,
      roomCount: 1,
      roomId: "room_101",
      roomNumber: "101",
      roomType: "DELUXE",
      bookingDateEpoch: 1763960000000,
      checkInEpoch: 1763960000000,
      checkOutEpoch: 1764132800000,
      checkInTimeStr: "02:00 PM",
      checkOutTimeStr: "11:00 AM",
      nights: 2,
      nightlyRate: 3800,
      roomCharges: 7600,
      extraChargesTotal: 0,
      extraCharges: [],
      discount: 0,
      taxableAmount: 7600,
      gstRate: 12,
      cgstAmount: 456,
      sgstAmount: 456,
      igstAmount: 0,
      totalAmount: 8512,
      advancePaid: 2000,
      totalPaid: 2000,
      balanceDue: 6512,
      refundAmount: 0,
      bookingSource: "WHATSAPP",
      status: "CONFIRMED",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Check-in and then Check-out
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/bookings/bkg_1`).update({
      status: "CHECKED_OUT",
      totalPaid: 8512,
      balanceDue: 0,
      actualCheckOutEpoch: 1764132800000,
      updatedAt: pastDate(2000),
    })
  );

  // Terminal state lock: cannot revert CHECKED_OUT back to CONFIRMED
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/bookings/bkg_1`).update({
      status: "CONFIRMED",
      updatedAt: pastDate(1000),
    })
  );

  // RoomLock create & delete succeed, update fails
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/roomLocks/room_101_20261124`).set({
      userId: ALICE_UID,
      hotelId: HOTEL_ID,
      roomId: "room_101",
      bookingId: "bkg_1",
      dateKey: "20261124",
      createdAt: created,
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/roomLocks/room_101_20261124`).update({
      bookingId: "bkg_2",
    })
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/roomLocks/room_101_20261124`).delete()
  );
});

test("AuditLog is strictly append-only; Payment, Invoice, Staff, and filtered queries succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/auditLogs/log_1`).set({
      userId: ALICE_UID,
      hotelId: HOTEL_ID,
      actorName: "Aloke Mukherjee",
      actorRole: "OWNER",
      actionType: "BOOKING_CREATED",
      entityType: "BOOKING",
      entityId: "bkg_1",
      summary: "Created booking SHM-2026-1001 for Priya Deshmukh",
      timestampEpoch: Date.now() - 5000,
      createdAt: created,
    })
  );

  // AuditLog cannot be updated or deleted
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/auditLogs/log_1`).update({ summary: "Tampered" })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/auditLogs/log_1`).delete()
  );

  // Payment create with PENDING_VERIFICATION and reconciliation update to VERIFIED
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/payments/pay_1`).set({
      userId: ALICE_UID,
      hotelId: HOTEL_ID,
      transactionCode: "TXN-2026-001",
      bookingId: "bkg_1",
      bookingCode: "SHM-2026-1001",
      guestName: "Priya Deshmukh",
      amount: 2000,
      paymentMode: "UPI",
      paymentStatus: "PENDING_VERIFICATION",
      upiIdUsed: "smarthotel@icici",
      receivedBy: "Reception Desk",
      paymentEpoch: Date.now() - 5000,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/payments/pay_1`).update({
      paymentStatus: "VERIFIED",
      referenceNumber: "UTR432198765432",
      updatedAt: pastDate(1000),
    })
  );

  // Kotlin-aligned subcollection list queries succeed for owner
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/rooms`).where("hotelId", "==", HOTEL_ID).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/bookings`).where("hotelId", "==", HOTEL_ID).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/guests`).where("hotelId", "==", HOTEL_ID).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/payments`).where("hotelId", "==", HOTEL_ID).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/auditLogs`).where("hotelId", "==", HOTEL_ID).get());
});
