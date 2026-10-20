# 📱 Complete Guide: Android Development in Termux
This documentation covers how to build, compile, and deploy a fully functional Android app (StudySpark) using only an Android phone, Termux, and GitHub Actions.

## 🛠️ Prerequisites
1. **Termux** installed (from F-Droid).
2. A **GitHub Account** (free).
3. A **Google AI Studio Account** (for the Gemini API keys).

## Step 1: Termux & Ubuntu Setup
**1. Update Termux and install Ubuntu:**
pkg update && pkg upgrade -y
pkg install proot-distro -y
proot-distro install ubuntu

**2. Log into Ubuntu:**
proot-distro login ubuntu

**3. Install development tools:**
apt update && apt upgrade -y
apt install openjdk-17-jdk git wget curl unzip nano -y

## Step 2: Project Initialization
**1. Create the directory structure:**
mkdir -p ~/StudySpark/app/src/main/java/com/studyspark/app
mkdir -p ~/StudySpark/app/src/main/res/values
mkdir -p ~/StudySpark/.github/workflows
cd ~/StudySpark

**2. Create the Gradle Wrapper:**
mkdir -p gradle/wrapper
curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar
curl -L -o gradlew https://github.com/gradle/gradle/raw/v8.5.0/gradlew
chmod +x gradlew

## Step 3: Core Plugins & Dependencies
**1. Project Level build.gradle.kts:**
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.16" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}

**2. App Level Dependencies:**
- Compose: androidx.compose:compose-bom:2024.02.00
- Room DB: androidx.room:room-runtime, room-ktx, ksp room-compiler
- Navigation: androidx.navigation:navigation-compose:2.7.7
- PDF Extraction: com.tom-roush:pdfbox-android:2.0.27.0
- OCR: com.google.android.gms:play-services-mlkit-text-recognition
- AI: com.google.ai.client.generativeai:generativeai:0.9.0

## Step 4: CI/CD Setup (GitHub Actions)
- Create .github/workflows/android-build.yml
- Add GitHub Secrets: GEMINI_API_KEY and GEMINI_API_KEY_2
- Workflow uses JDK 17 to build APK and upload artifacts.

## Step 5: Advanced Configurations
- **FileProvider:** Added to AndroidManifest for saving/sharing PDFs.
- **Security (FLAG_SECURE):** Prevents screenshots during exams.
- **Anti-Cheat:** Uses SharedPreferences to detect if app was minimized and auto-submits quiz.
- **OCR:** Uses TakePicture() for high-res photos and PdfRenderer for image PDFs.

## 🚀 How to Deploy on a New Phone
1. Setup Termux, Ubuntu, and Java.
2. git clone https://github.com/Prudent66/StudySpark.git
3. Create GitHub Secrets for API keys.
4. Edit code with nano, git push, and download APK from GitHub Actions.
