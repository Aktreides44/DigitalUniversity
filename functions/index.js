const admin = require("firebase-admin");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");


admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

exports.assignmentCreatedNotification = onDocumentCreated(
    "assignments/{assignmentId}",
    async (event) => {

        const assignment = event.data.data();

        if (!assignment) {
            console.log("No assignment data found.");
            return null;
        }

        const courseId = assignment.courseId;
        const title = assignment.title;
        const dueDate = assignment.dueDate;

        console.log("FUNCTION TRIGGERED");
        console.log(`New assignment created for course ${courseId}`);

        const enrollmentSnap = await db
            .collection("enrollments")
            .where("courseId", "==", courseId)
            .get();

        console.log(
            `Enrollments found: ${enrollmentSnap.size}`
        );

        if (enrollmentSnap.empty) {
            console.log("No students enrolled.");
            return null;
        }

        const promises = enrollmentSnap.docs.map(async (enrollment) => {

            const studentEmail =
                enrollment.get("studentEmail");

            console.log(
                `Processing student: ${studentEmail}`
            );

            const userSnap = await db
                .collection("users")
                .where("email", "==", studentEmail)
                .limit(1)
                .get();

            if (userSnap.empty) {
                console.log(
                    `User not found: ${studentEmail}`
                );
                return;
            }

            const userDoc = userSnap.docs[0];

            const token =
                userDoc.get("fcmToken");

            if (!token) {
                console.log(
                    `No FCM token for ${studentEmail}`
                );
                return;
            }

            try {

                await messaging.send({
                    token: token,
                    notification: {
                        title: "New Assignment",
                        body: `${title} - Due ${dueDate}`
                    }
                });

                console.log(
                    `Notification sent to ${studentEmail}`
                );

            } catch (e) {

                console.error(
                    `Failed to send to ${studentEmail}`,
                    e
                );
            }
        });

        await Promise.all(promises);

        console.log("Function completed.");

        return null;
    }
);

const { onDocumentUpdated } =
    require("firebase-functions/v2/firestore");

exports.gradeReleasedNotification =
    onDocumentUpdated(
        "submissions/{submissionId}",
        async (event) => {

            const before = event.data.before.data();
            const after = event.data.after.data();

            if (!before || !after) return null;

            // only trigger when grade changes
            if (before.grade === after.grade) {
                return null;
            }

            const studentId = after.studentId;
            const grade = after.grade;
            const comment = after.comment || "";

            console.log(
                `Grade updated for student ${studentId}`
            );

            const userDoc =
                await db.collection("users")
                    .doc(studentId)
                    .get();

            if (!userDoc.exists) {
                console.log("Student not found");
                return null;
            }

            const token =
                userDoc.get("fcmToken");

            if (!token) {
                console.log("No token found");
                return null;
            }

            try {

                await messaging.send({
                    token: token,
                    notification: {
                        title: "Assignment Graded",
                        body: `Grade: ${grade}`
                    }
                });

                console.log(
                    `Grade notification sent to ${studentId}`
                );

            } catch (e) {

                console.error(e);
            }

            return null;
        }
    );

exports.dmMessageNotification =
    onDocumentCreated(
        "chats/{chatId}/messages/{messageId}",
        async (event) => {

            const message = event.data.data();

            if (!message) return null;

            const chatId = message.chatId;
            const senderId = message.senderId;
            const senderName = message.senderName;
            const text = message.text;

            // Only DM chats
            if (!chatId.startsWith("dm_")) {
                return null;
            }

            const metaDoc =
                await db.collection("chatMeta")
                    .doc(chatId)
                    .get();

            if (!metaDoc.exists) {
                console.log("chatMeta not found");
                return null;
            }

            const meta = metaDoc.data();

            const studentId = meta.studentId;
            const lecturerId = meta.lecturerId;

            let recipientId;

            if (senderId === studentId) {
                recipientId = lecturerId;
            } else {
                recipientId = studentId;
            }

            const userDoc =
                await db.collection("users")
                    .doc(recipientId)
                    .get();

            if (!userDoc.exists) {
                console.log("Recipient not found");
                return null;
            }

            const token =
                userDoc.get("fcmToken");

            if (!token) {
                console.log("No token found");
                return null;
            }

            try {

                await messaging.send({
                    token: token,
                    notification: {
                        title: `New message from ${senderName}`,
                        body: text
                    }
                });

                console.log(
                    `DM notification sent to ${recipientId}`
                );

            } catch (e) {

                console.error(e);
            }

            return null;
        }
    );

exports.groupMessageNotification =
    onDocumentCreated(
        "chats/{chatId}/messages/{messageId}",
        async (event) => {

            const message = event.data.data();

            if (!message) return null;

            const chatId = message.chatId;
            const senderId = message.senderId;
            const senderName = message.senderName;
            const text = message.text;

            if (!chatId.startsWith("group_")) {
                return null;
            }

            const courseId =
                chatId.replace("group_", "");

            console.log(
                `Group message in ${courseId}`
            );

            const courseDoc =
                await db.collection("courses")
                    .doc(courseId)
                    .get();

            if (!courseDoc.exists) {
                console.log("Course not found");
                return null;
            }

            const lecturerId =
                courseDoc.get("lectureId");

            const recipientIds = new Set();

            // Add lecturer if not sender
            if (
                lecturerId &&
                lecturerId !== senderId
            ) {
                recipientIds.add(lecturerId);
            }

            const enrollmentSnap =
                await db.collection("enrollments")
                    .where("courseId", "==", courseId)
                    .get();

            for (const enrollment of enrollmentSnap.docs) {

                const studentEmail =
                    enrollment.get("studentEmail");

                const userSnap =
                    await db.collection("users")
                        .where("email", "==", studentEmail)
                        .limit(1)
                        .get();

                if (userSnap.empty) continue;

                const userId =
                    userSnap.docs[0].id;

                if (userId !== senderId) {
                    recipientIds.add(userId);
                }
            }

            const sendPromises = [];

            for (const recipientId of recipientIds) {

                const userDoc =
                    await db.collection("users")
                        .doc(recipientId)
                        .get();

                if (!userDoc.exists) continue;

                const token =
                    userDoc.get("fcmToken");

                if (!token) continue;

                sendPromises.push(

                    messaging.send({
                        token: token,
                        notification: {
                            title:
                                `Group message from ${senderName}`,
                            body: text
                        }
                    })

                );
            }

            await Promise.all(sendPromises);

            console.log(
                `Group notifications sent to ${recipientIds.size} users`
            );

            return null;
        }
    );
exports.assignmentSubmissionNotification =
    onDocumentCreated(
        "submissions/{submissionId}",
        async (event) => {

            const submission = event.data.data();

            if (!submission) return null;

            const courseId = submission.courseId;
            const studentName = submission.studentName;

            const courseDoc =
                await db.collection("courses")
                    .doc(courseId)
                    .get();

            if (!courseDoc.exists) {
                console.log("Course not found");
                return null;
            }

            const lecturerId =
                courseDoc.get("lectureId");

            if (!lecturerId) {
                console.log("No lecturer found");
                return null;
            }

            const lecturerDoc =
                await db.collection("users")
                    .doc(lecturerId)
                    .get();

            if (!lecturerDoc.exists) {
                console.log("Lecturer not found");
                return null;
            }

            const token =
                lecturerDoc.get("fcmToken");

            if (!token) {
                console.log("No lecturer token");
                return null;
            }

            await messaging.send({
                token: token,
                notification: {
                    title: "Assignment Submitted",
                    body: `${studentName} submitted an assignment`
                }
            });

            console.log(
                `Submission notification sent to lecturer`
            );

            return null;
        }
    );
exports.courseEnrollmentNotification =
    onDocumentCreated(
        "enrollments/{enrollmentId}",
        async (event) => {

            const enrollment = event.data.data();

            if (!enrollment) return null;

            const courseId =
                enrollment.courseId;

            const studentEmail =
                enrollment.studentEmail;

            const courseDoc =
                await db.collection("courses")
                    .doc(courseId)
                    .get();

            if (!courseDoc.exists) {
                console.log("Course not found");
                return null;
            }

            const lecturerId =
                courseDoc.get("lectureId");

            if (!lecturerId) {
                console.log("No lecturer assigned");
                return null;
            }

            const lecturerDoc =
                await db.collection("users")
                    .doc(lecturerId)
                    .get();

            if (!lecturerDoc.exists) {
                console.log("Lecturer not found");
                return null;
            }

            const token =
                lecturerDoc.get("fcmToken");

            if (!token) {
                console.log("No lecturer token");
                return null;
            }

            let studentName = studentEmail;

            const userSnap =
                await db.collection("users")
                    .where("email", "==", studentEmail)
                    .limit(1)
                    .get();

            if (!userSnap.empty) {
                studentName =
                    userSnap.docs[0].get("name") ||
                    studentEmail;
            }

            await messaging.send({
                token: token,
                notification: {
                    title: "New Course Enrollment",
                    body: `${studentName} enrolled in ${courseId}`
                }
            });

            console.log(
                `Enrollment notification sent to lecturer`
            );

            return null;
        }
    );

    // Add these to your existing index.js Cloud Functions file

    const { onDocumentDeleted } = require("firebase-functions/v2/firestore");

    // ── When a COURSE is deleted ──
    // Deletes: enrollments, assignments, submissions, chatMeta, DM messages
    exports.onCourseDeleted = onDocumentDeleted(
        "courses/{courseId}",
        async (event) => {
            const courseId = event.params.courseId;
            console.log(`Course deleted: ${courseId}, cascading...`);

            const promises = [];

            // Delete enrollments
            const enrollSnap = await db.collection("enrollments")
                .where("courseId", "==", courseId).get();
            enrollSnap.docs.forEach(d => promises.push(d.ref.delete()));

            // Delete assignments + their submissions
            const assignSnap = await db.collection("assignments")
                .where("courseId", "==", courseId).get();
            for (const aDoc of assignSnap.docs) {
                promises.push(aDoc.ref.delete());
                const subSnap = await db.collection("submissions")
                    .where("assignmentId", "==", aDoc.id).get();
                subSnap.docs.forEach(s => promises.push(s.ref.delete()));
            }

            // Delete notifications related to course
            const notifSnap = await db.collection("notifications")
                .where("courseId", "==", courseId).get();
            notifSnap.docs.forEach(d => promises.push(d.ref.delete()));

            // Delete DM chatMeta + their messages subcollection
            const chatMetaSnap = await db.collection("chatMeta")
                .where("courseId", "==", courseId).get();
            for (const chatDoc of chatMetaSnap.docs) {
                const msgSnap = await db.collection("chats")
                    .doc(chatDoc.id).collection("messages").get();
                msgSnap.docs.forEach(m => promises.push(m.ref.delete()));
                promises.push(chatDoc.ref.delete());
            }

            // Delete group chat messages (NOT the concept, just the messages)
            const groupChatId = `group_${courseId}`;
            const groupMsgSnap = await db.collection("chats")
                .doc(groupChatId).collection("messages").get();
            groupMsgSnap.docs.forEach(m => promises.push(m.ref.delete()));

            await Promise.all(promises);
            console.log(`Cascade delete complete for course ${courseId}`);
            return null;
        }
    );

    // ── When a USER is deleted ──
    // Deletes: Auth account, enrollments, submissions, DM chatMeta + messages
    // Does NOT delete group chat messages (their name stays in history)
    exports.onUserDeleted = onDocumentDeleted(
        "users/{uid}",
        async (event) => {
            const uid = event.params.uid;
            const userData = event.data?.data();
            const email = userData?.email || "";

            console.log(`User deleted: ${uid}, cascading...`);

            const promises = [];

            // Delete Firebase Auth account
            try {
                await admin.auth().deleteUser(uid);
                console.log(`Auth user ${uid} deleted`);
            } catch (e) {
                console.error(`Auth delete failed for ${uid}:`, e.message);
            }

            // Delete enrollments by email
            if (email) {
                const enrollSnap = await db.collection("enrollments")
                    .where("studentEmail", "==", email).get();
                enrollSnap.docs.forEach(d => promises.push(d.ref.delete()));
            }

            // Delete submissions by studentId
            const subSnap = await db.collection("submissions")
                .where("studentId", "==", uid).get();
            subSnap.docs.forEach(d => promises.push(d.ref.delete()));

            // Delete notifications for this user
            const notifSnap = await db.collection("notifications")
                .where("userId", "==", uid).get();
            notifSnap.docs.forEach(d => promises.push(d.ref.delete()));

            // Delete DM chatMeta where this user is student + their messages
            const dmSnap = await db.collection("chatMeta")
                .where("studentId", "==", uid).get();
            for (const chatDoc of dmSnap.docs) {
                const msgSnap = await db.collection("chats")
                    .doc(chatDoc.id).collection("messages").get();
                msgSnap.docs.forEach(m => promises.push(m.ref.delete()));
                promises.push(chatDoc.ref.delete());
            }

            await Promise.all(promises);
            console.log(`Cascade delete complete for user ${uid}`);
            return null;
        }
    );

    // ── When an ASSIGNMENT is deleted (future use) ──
    // Deletes: all submissions for that assignment
    exports.onAssignmentDeleted = onDocumentDeleted(
        "assignments/{assignmentId}",
        async (event) => {
            const assignmentId = event.params.assignmentId;
            console.log(`Assignment deleted: ${assignmentId}, cascading...`);

            const subSnap = await db.collection("submissions")
                .where("assignmentId", "==", assignmentId).get();

            await Promise.all(subSnap.docs.map(d => d.ref.delete()));
            console.log(`Deleted ${subSnap.size} submissions`);
            return null;
        }
    );