# MedStudy 🩺

> A modern Android application for medical students and NEET PG / AIIMS aspirants featuring active recall, curated high-yield question banks, progress analytics, and background spontaneous question overlays.

---

## 🌟 Overview

**MedStudy** is an offline-first Android learning platform designed specifically for medical students preparing for high-stakes examinations like NEET PG, AIIMS, and INI-CET. Combining a modular Jetpack Compose interface with structured question banks and background micro-learning habits, MedStudy helps students study consistently throughout their day.

### ⚡ Highlight Feature: Spontaneous Question Overlay
Break doom-scrolling habits and reinforce spaced repetition! MedStudy includes a dedicated foreground overlay service (`QuestionOverlayService`) that spontaneously presents high-yield MCQs over other apps at customizable time intervals, prompting instant active recall without requiring the user to open the app.

---

## 🚀 Key Features

- **🎯 Spontaneous Question Overlays**:
  - Timed overlay popups appearing on top of any active app using Android WindowManager.
  - Interactive multiple-choice answers, immediate feedback, and explanations on submit.
  - Configurable intervals and snooze options.

- **📚 Curated Medical Question Bank (1,600+ MCQs)**:
  - High-yield questions derived from NEET PG & AIIMS exams (MedMCQA dataset).
  - Covers **21 medical subjects** (Anatomy, Physiology, Pathology, Pharmacology, Forensic Medicine, Surgery, Pediatrics, etc.) and **141 specialized topics**.
  - Detailed rationales and answer explanations for deep conceptual understanding.

- **⚡ Interactive Self-Paced Practice**:
  - Subject and topic-wise practice sessions.
  - Instant answer validation with comprehensive explanations.
  - Review mode for tracking correct and incorrect attempts.

- **🏷️ Subject & Category Explorer**:
  - Filter by exam type (NEET PG, AIIMS) or subject area.
  - View subject-level question counts and completion rates.

- **📊 Progress Tracking & Weekly Study Plans**:
  - Interactive activity calendar and study streaks.
  - Performance analytics with subject-wise accuracy insights.
  - Personalized weekly study targets and plan generator.

- **💾 Offline-First Architecture**:
  - Powered by Room Database for fast local caching, attempt logging, and full offline usability.

---

## 🏗️ Architecture & Project Structure

MedStudy follows **Clean Architecture** principles and a modern **Multi-Module** Android structure:

```
medstudy/
├── app/                  # Main entry point, navigation host & foreground overlay service
├── core/
│   ├── model/            # Pure Kotlin domain models (Question, Subject, StudyPlan, etc.)
│   ├── database/         # Room database, entities, and DAOs for offline persistence
│   ├── data/             # Repositories for question catalogs, learner activity & plans
│   └── designsystem/     # Material 3 theme tokens, typography, and reusable UI components
├── feature/
│   ├── practice/         # Question practice flow, quiz UI, and review screens
│   ├── categories/       # Subject explorer, topic browsing, and exam filters
│   └── progress/         # Activity calendar, study insights, and weekly plan generator
└── repo/                 # Organized MedMCQA JSON dataset (subjects, topics, questions)
```

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose & Material 3 (Material Design You)
- **Architecture**: Multi-Module Clean Architecture with MVVM / MVI patterns and unidirectional data flow (UDF)
- **Local Persistence**: Android Jetpack Room (SQLite)
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow`
- **System Integration**: Android WindowManager (`TYPE_APPLICATION_OVERLAY`) & Foreground Services

---

## 📲 Getting Started

### Prerequisites
- **Android Studio Ladybug** (or later)
- **JDK 17**
- **Android SDK** (API Level 26+ minimum, target SDK 34/35)

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/dhri-yama/medstudy.git
cd medstudy

# Build debug APK
./gradlew assembleDebug

# Install on a connected device or emulator
./gradlew :app:installDebug
```

### Launch via ADB

```bash
adb shell am start -n com.medstudy.app/com.medstudy.app.MainActivity
```

### Enabling Question Overlays
To enable spontaneous pop-up questions over other apps:
1. Open the app settings or navigate to your device's **Settings > Apps > MedStudy**.
2. Tap **Display over other apps** (or "Appear on top") and toggle **Allow permission**.
3. Toggle on **Sudden Questions** in the app and set your desired interval!

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) (or applicable open source license). Dataset contents belong to their respective creators under the MedMCQA research release.
