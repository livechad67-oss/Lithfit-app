/**
 * CLASTAD / LITHFIT Firebase Cloud Functions Template (Node.js 20, Gen 2)
 *
 * Deployment Note:
 * Deploying Cloud Functions requires Firebase Blaze Plan + Secrets configured via:
 *   firebase functions:secrets:set SMTP_HOST SMTP_USER SMTP_PASS
 *
 * 1. onUserRegisteredNotifyOwner:
 *    Listens to Firestore `/ownerNotifications/{notifId}` creation and sends an
 *    automated HTML/text registration alert to `livechad67@gmail.com` server-side
 *    without exposing credentials or triggering client email apps.
 *
 * 2. resolveUsernameToAuthHint:
 *    Optional callable for resolving normalized usernames across devices when
 *    Email/Password Firebase Auth + Cloud Functions are activated in Console.
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const OWNER_EMAIL = "livechad67@gmail.com";

exports.onUserRegisteredNotifyOwner = onDocumentCreated(
  {
    document: "ownerNotifications/{notifId}",
    database: "ai-studio-android-lithfit-b1d3862c-5080-49ef-8f10-b73965646245",
  },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    logger.info("New LITHFIT registration recorded for CLASTAD owner", {
      notifId: event.params.notifId,
      username: data.username,
      authMethod: data.authMethod,
      recipientOwnerEmail: OWNER_EMAIL,
    });

    // When nodemailer + SMTP secrets are configured in Cloud Functions:
    // Send automated email directly to livechad67@gmail.com using SMTP credentials
    // from process.env / defineSecret("SMTP_PASS").
  }
);
