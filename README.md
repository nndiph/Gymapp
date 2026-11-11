# Repset *(working title)*

> Gym tracker + media sosial untuk Android. Catat latihan, buat gerakan sendiri, dan tunjukkan progress-mu — seperti GitHub, tapi untuk gym.

**Status:** 🚧 M0 (fondasi) — skeleton app sudah bisa di-build; belum ada fitur.

## Kenapa dibuat

Banyak app gym tracker tidak menyediakan gerakan yang benar-benar kita lakukan. Repset menyediakan library gerakan yang lengkap **dan** memungkinkan setiap user membuat gerakan custom sendiri. Di atasnya ada lapisan sosial ringan: profil dengan heatmap konsistensi dan progress per gerakan yang bisa dilihat orang lain.

## Fitur MVP

- **Catat latihan** — pilih gerakan (bawaan atau custom), catat set (beban, reps), simpan sebagai sesi. Bekerja offline.
- **Gerakan custom** — buat, ubah, dan pakai gerakanmu sendiri.
- **Profil** — heatmap latihan, statistik bulanan, rekor pribadi (PR), dan routine sebagai "playlist".
- **Dashboard** — latihan hari ini dan pengingat notifikasi.
- **Sosial minimal** — profil publik/privat, follow.

## Tech stack

Kotlin · Jetpack Compose · Hilt · Firebase (Auth, Firestore, Crashlytics, App Check) · WorkManager

Pertimbangan keandalan dan alasan pemilihan stack ada di [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Dokumentasi

- [PRD](docs/PRD.md)
- [Arsitektur](docs/ARCHITECTURE.md)
- [Model data](docs/DATA_MODEL.md)
- [Roadmap](docs/ROADMAP.md)
- [Setup GitHub & Firebase](docs/GITHUB_SETUP.md)

## Menjalankan proyek

**Prasyarat:** JDK 17, Android Studio (stabil terbaru) dengan SDK Platform 37.0 dan Build-Tools 36.0.0, Node.js + `firebase-tools` dan **Java 21** (untuk emulator; JBR bawaan Android Studio sudah 21), GitHub CLI `gh` (untuk alur kerja repo).

1. Clone repo, lalu buat `local.properties` berisi `sdk.dir=<path Android SDK>` (Android Studio membuatnya otomatis saat proyek dibuka).
2. Siapkan `app/google-services.json`:
   - **Belum punya proyek Firebase?** Salin saja placeholder: `cp app/google-services.json.example app/google-services.json`. App bisa di-build dan dijalankan, tetapi fitur Firebase tidak aktif.
   - **Sudah punya?** Unduh `google-services.json` dari Firebase Console ke `app/`. File ini tidak di-commit.
3. Build & uji:

   ```bash
   ./gradlew assembleDebug            # build debug
   ./gradlew testDebugUnitTest        # unit test
   ./gradlew lintDebug                # Android lint
   ./gradlew ktlintCheck              # gaya kode (ktlintFormat untuk merapikan otomatis)
   ```

4. Emulator Firebase (Auth + Firestore, rules dari `firestore.rules`):

   ```bash
   firebase emulators:start           # UI di http://localhost:4000
   ```

   Tanpa login pun bisa: `.firebaserc` menunjuk ke proyek demo `demo-repset` yang hanya hidup di emulator. `firebase-tools` versi 15 menolak Java di bawah 21, sedangkan Gradle memakai JDK 17. Di PowerShell, arahkan `JAVA_HOME` ke JBR Android Studio hanya untuk sesi emulator:

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
   firebase emulators:start
   ```

> `applicationId` saat ini `com.repset.app` adalah **placeholder** sampai nama final diputuskan (lihat `CLAUDE.md` → Pertanyaan terbuka). Ganti di `app/build.gradle.kts`, nama paket di `app/src/main/java/`, dan `app/google-services.json.example` sebelum mendaftarkan app di Firebase.

## Lisensi

Kode: [MIT](LICENSE). Dataset gerakan seed memiliki lisensi sendiri — lihat `docs/DATA_MODEL.md`.
