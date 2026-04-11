const {onDocumentWritten} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

exports.notifyOnTaskChange = onDocumentWritten(
    "Tasks/{taskId}",
    async (event) => {
      const change = event.data;
      if (!change) return null;

      const after = change.after.data();
      const before = change.before ? change.before.data() : null;

      if (!after) return null;

      // SCENARIO 1: ADMIN ASSIGNS A TASK
      const isNowAssigned = after.status === "ASSIGNED";
      const wasAssigned = before && before.status === "ASSIGNED";

      if (isNowAssigned && !wasAssigned) {
        const userId = after.assignedToUserId;
        const userRef = admin.firestore().collection("Users").doc(userId);
        const userDoc = await userRef.get();

        if (!userDoc.exists) return null;

        const token = userDoc.data().fcmToken;
        if (token) {
          const payload = {
            token: token,
            notification: {
              title: "New Task Assigned",
              body: `${after.companyName} - ${after.workToBeDone}`,
            },
          };
          await admin.messaging().send(payload);
        }
      }

      // SCENARIO 2: EMPLOYEE COMPLETES TASK
      const isNowPending = after.status === "PENDING_BILLING";
      const wasPending = before && before.status === "PENDING_BILLING";

      if (isNowPending && !wasPending) {
        const accSnap = await admin.firestore().collection("Users")
            .where("role", "==", "ACCOUNTANT").get();

        const tokens = [];
        accSnap.forEach((doc) => {
          if (doc.data().fcmToken) tokens.push(doc.data().fcmToken);
        });

        if (tokens.length > 0) {
          const payload = {
            tokens: tokens,
            notification: {
              title: "Task Ready for Billing",
              body: `${after.employeeName} finished at ${after.companyName}`,
            },
          };
          await admin.messaging().sendEachForMulticast(payload);
        }
      }

      return null;
    },
);
