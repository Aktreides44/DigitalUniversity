# Digital University 🎓

A native Android mobile learning platform built with Kotlin, Jetpack Compose, and Firebase. Provides role-based access for Admins, Lecturers, and Students to manage courses, assignments, grading, real-time chat, and push notifications — all in one app.

---

## Features

- **Role-based access** — Admin, Lecturer, and Student each get a tailored dashboard and permissions
- **Course management** — create, edit, enroll, and cascade-delete courses with scheduled sessions
- **Assignments & submissions** — lecturers attach files, students upload submissions via Firebase Storage
- **Grading system** — numeric grades (2–5) with written feedback, visible to students in a dedicated Grades tab
- **Real-time chat** — group chat per course plus private lecturer ↔ student direct messages, powered by Firestore snapshot listeners
- **Push notifications** — Firebase Cloud Functions + FCM trigger notifications for new assignments, submissions, grades, enrollments, and chat messages
- **Analytics dashboards** — admin sees platform-wide stats and course popularity; lecturers see per-course grade averages and submission rates
- **Cascade delete** — deleting a course or user automatically removes all related enrollments, assignments, submissions, and chat data

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| Jetpack Compose | Declarative UI |
| MVVM + LiveData | Architecture pattern |
| Navigation Component | Screen routing |
| Firebase Auth | Authentication |
| Cloud Firestore | Real-time database |
| Firebase Storage | File uploads (assignments & submissions) |
| Cloud Functions | Server-side push notification triggers |
| Firebase Cloud Messaging (FCM) | Push notifications |

---

## Architecture

The app follows the **MVVM** pattern:

```
UI Layer (Compose screens)
        ↓ observes
ViewModel Layer (business logic, LiveData)
        ↓ calls
Repository Layer (Firestore/Storage abstraction)
        ↓
Firebase Backend (Auth, Firestore, Storage, Functions, FCM)
```

### Project Structure

```
com.example.digitaluniversity/
├── auth/              AuthViewModel, UserViewModel
├── chat/              ChatMessage, ChatViewModel, ChatScreen (group + DM)
├── courses/           Course, CourseSchedule, CourseRepository, CourseViewModel
├── dashboard/         Student, Lecturer, Admin dashboards, CourseDetailScreen
│   └── assignment/    Assignment, AssignmentSubmission, AssignmentViewModel, SubmissionViewModel
├── navigation/        AppNavigation (single NavHost)
└── MainActivity
```

---

## User Roles

| Role | Capabilities |
|---|---|
| **Admin** 👑 | Create/edit/delete courses (cascade), manage users, view platform analytics. View-only inside courses — no assignments or chat. |
| **Lecturer** 👨‍🏫 | Manage assigned courses, create/edit assignments with file attachments, grade submissions, chat with students. |
| **Student** 🎓 | Browse and enroll in courses, submit assignments, view grades and feedback, chat with lecturers. |

---

## Firestore Collections

| Collection | Key Fields |
|---|---|
| `users` | uid, name, email, role, fcmToken |
| `courses` | courseId, courseCode, courseName, description, credits, lecturerName, lectureId, schedules[] |
| `enrollments` | courseId, studentEmail, enrolledAt |
| `assignments` | assignmentId, courseId, title, description, dueDate, createdBy, attachmentUrl, attachmentName |
| `submissions` | submissionId, assignmentId, courseId, studentId, fileUrl, fileName, submittedAt, grade, comment |
| `chats/{id}/messages` | messageId, senderId, senderName, senderRole, text, timestamp |
| `chatMeta` | chatId, courseId, type (dm/group), studentId, lecturerId |

**Firebase Storage paths:**
- `assignment_files/{assignmentId}/{fileName}` — lecturer-uploaded briefs
- `submissions/{assignmentId}/{studentUID}/{fileName}` — student submissions

---

## Push Notification Triggers

| Event | Notifies |
|---|---|
| Assignment created | All enrolled students |
| Assignment submitted | Course lecturer |
| Grade released | The submitting student |
| Student enrolled | Course lecturer |
| Direct message sent | The other person in the chat |
| Group chat message | All course members except sender |

Implemented via Cloud Functions (`onDocumentCreated` / `onDocumentUpdated` triggers) so FCM server keys never touch client code.

---

## Setup

### Prerequisites
- Android Studio (latest stable)
- A Firebase project on the **Blaze** plan (required for Cloud Functions)
- Node.js v18+ (for deploying Cloud Functions)

### 1. Clone the repo
```bash
git clone https://github.com/<your-username>/digital-university.git
cd digital-university
```

### 2. Connect Firebase
- Create a Firebase project in the [Firebase Console](https://console.firebase.google.com)
- Add an Android app with package name `com.example.digitaluniversity`
- Download `google-services.json` and place it in `app/`
- Enable **Authentication** (Email/Password), **Firestore**, **Storage**, and **Cloud Messaging**

### 3. Deploy Firestore & Storage rules
Paste the rules from `/firestore.rules` and `/storage.rules` into the respective tabs in the Firebase Console, or deploy via CLI:
```bash
firebase deploy --only firestore:rules,storage:rules
```

### 4. Deploy Cloud Functions
```bash
cd functions
npm install
firebase deploy --only functions
```

### 5. Run the app
Open the project in Android Studio and run on an emulator or device.

---

## Screenshots

| Login | Student Dashboard | Course Detail |
|---|---|---|
| _add screenshot_ | _add screenshot_ | _add screenshot_ |

| Assignments | Chat | Grades |
|---|---|---|
| _add screenshot_ | _add screenshot_ | _add screenshot_ |

---

## License

This project was built as a university assignment for educational purposes.
