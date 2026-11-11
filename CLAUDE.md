# CLAUDE.md — Panduan untuk Claude Code

> Letakkan file ini di **root repo**. Baca seluruhnya sebelum mengerjakan apa pun, lalu baca dokumen di `docs/` yang relevan dengan tugasmu.

## Proyek

Aplikasi Android **gym tracker + media sosial** — "GitHub-nya gym": setiap sesi latihan menjadi riwayat yang bisa dilihat orang lain (heatmap konsistensi, progress per gerakan, routine sebagai "playlist" di profil).

- **Working title:** `Repset` (belum final — lihat Pertanyaan Terbuka)
- **Pemilik:** Proyek portofolio yang akan diseriusi sampai rilis di Play Store.
- **Platform:** Android dulu. iOS/web di luar scope.
- **Masalah utama yang diselesaikan:** app gym lain tidak menyediakan gerakan yang dipakai user. Library gerakan harus lengkap **dan** user bisa membuat gerakan custom sendiri.
- **Prioritas teknis nomor satu:** app tidak boleh sering "down". Pemilik proyek pernah berkali-kali mengalami server error di web buatannya sendiri, jadi **tidak ada server yang dikelola sendiri**, dan app harus tetap berguna saat offline.

## Dokumen (di folder `docs/`)

| File | Isi |
|---|---|
| `docs/PRD.md` | Visi, scope MVP, user story, acceptance criteria, non-goals |
| `docs/ARCHITECTURE.md` | Keputusan stack + alasannya, arsitektur app, offline-first, notifikasi, keamanan, biaya |
| `docs/DATA_MODEL.md` | Skema Firestore, strategi seed gerakan, draf security rules, pola agregasi |
| `docs/ROADMAP.md` | Milestone M0–M7 dengan checklist issue, Definition of Done, checklist Play Store |
| `docs/GITHUB_SETUP.md` | Langkah setup repo, label, milestone, CI, branch rules, Firebase project |

## Keputusan yang sudah diambil

| Area | Keputusan |
|---|---|
| Bahasa & UI | Kotlin + Jetpack Compose (Material 3), Android native |
| Arsitektur | MVVM + unidirectional data flow, Hilt untuk DI, Navigation Compose, Coroutines + Flow |
| Backend | Firebase: Auth, Cloud Firestore, FCM (nanti), Crashlytics, App Check |
| Offline | Cache offline bawaan Firestore + seed gerakan di-bundle sebagai JSON di assets |
| Notifikasi latihan harian | **Lokal** (WorkManager/AlarmManager), tidak bergantung server |
| Cloud Functions | **Tidak dipakai di MVP** (tetap bisa di paket Spark, hindari kerumitan) |
| Foto / Cloud Storage | Ditunda (Storage butuh paket Blaze) |
| minSdk | 26 (bisa dinaikkan jika ada alasan kuat) |
| Repo | Satu repo, satu modul `app` dulu. Pecah modul hanya jika ada alasan konkret |

Alasan memilih Firebase dibanding Supabase ada di `docs/ARCHITECTURE.md` (ringkas: Firestore punya SLA kontraktual dan tidak di-pause saat idle; Supabase Pro tidak punya SLA dan project gratisnya di-pause setelah 1 minggu tidak aktif).

## Pertanyaan terbuka — TANYAKAN ke pemilik proyek, jangan menebak

1. **Nama final app** dan `applicationId` (mis. `com.<namaakun>.<namaapp>`). `applicationId` **tidak bisa diganti** setelah upload pertama ke Play Console, jadi putuskan sebelum upload.
2. **Nama akun/organisasi GitHub** dan nama repo final.
3. **Lisensi dataset gerakan seed.** Kandidat: `free-exercise-db` dan `wger`. Verifikasi lisensi sebenarnya di repo/situs resminya sebelum mengimpor; jangan berasumsi.
4. **Region Firestore** (disarankan region Asia terdekat; tidak bisa diganti setelah dibuat).
5. Apakah kamu punya kartu untuk paket Blaze (dibutuhkan saat rilis). Jika belum, MVP tetap bisa jalan di Spark.

## Aturan kerja

**Komunikasi**
- Balas pemilik proyek dalam **Bahasa Indonesia**. Kode, nama identifier, komentar kode, dan pesan commit dalam **bahasa Inggris**. String UI default Indonesia lewat `strings.xml` (tidak di-hardcode).
- Pemilik proyek belum tentu mengenal Kotlin/Firebase secara mendalam. Jelaskan keputusan penting dalam 2–3 kalimat; jangan hanya menjatuhkan kode.

**Proses**
- Kerjakan **per milestone** di `docs/ROADMAP.md`, satu issue/PR kecil dalam satu waktu. Jangan membangun beberapa fitur sekaligus.
- Jangan menambah fitur di luar scope MVP (`docs/PRD.md` → Non-goals). Jika ada ide bagus, catat sebagai issue, jangan langsung dibangun.
- Sebelum memakai API library (Firebase BoM, Compose, Hilt, WorkManager, Credential Manager), **cek dokumentasi resmi versi terbaru**. Jangan mengandalkan ingatan; API Android/Firebase sering berubah.
- Jika menemukan konflik antara dokumen dan kenyataan (mis. library berubah), perbarui dokumen di PR yang sama dan sebutkan di deskripsi PR.

**Keandalan (jangan dilanggar)**
- UI **tidak boleh memblokir** menunggu jaringan. Menulis ke Firestore saat offline tidak akan selesai sampai ada koneksi; jangan `await` hasil write di jalur UI. Update UI dari listener/cache lokal.
- Setiap fitur inti (catat latihan, lihat riwayat, lihat routine hari ini, notifikasi) harus bisa dipakai **tanpa internet** setelah login pertama.
- Hindari listener realtime pada koleksi besar; batasi query dengan `limit` dan rentang tanggal. Biaya Firestore dihitung per dokumen yang dibaca.
- Tambahkan Crashlytics sejak M0 agar crash pengguna terlihat.

**Keamanan**
- Jangan commit: `google-services.json`, keystore (`*.jks`, `*.keystore`), `keystore.properties`, `local.properties`, service account JSON. Commit `google-services.json.example` sebagai gantinya.
- Security rules Firestore wajib diuji dengan Firebase Emulator sebelum deploy.
- Jangan menyimpan data sensitif yang tidak perlu. Data pengguna minimal: email/UID, username, data latihan.

**Git**
- Branch: `feat/...`, `fix/...`, `docs/...`, `chore/...` dari `main`. `main` selalu bisa di-build.
- Commit mengikuti Conventional Commits (`feat: add exercise picker`). Satu PR = satu tujuan, idealnya < 400 baris diff.
- Jangan force-push ke `main`. Jangan merge jika CI merah.

## Struktur kode (target)

```
app/src/main/java/<package>/
  core/        # theme, util, common ui components
  data/        # firestore datasource, seed loader, repository impl
  domain/      # model, repository interface, use case
  feature/
    auth/
    exercise/  # library + custom exercise
    logger/    # catat sesi latihan
    routine/
    profile/   # heatmap, progress, follow
    dashboard/
  notification/ # scheduler + receiver
app/src/main/assets/seed/exercises.json
firestore.rules
firestore.indexes.json
docs/
.github/
```

## Perintah umum (setelah proyek dikonfigurasi)

```bash
./gradlew assembleDebug            # build debug
./gradlew testDebugUnitTest        # unit test
./gradlew lintDebug                # Android lint
firebase emulators:start           # Firestore + Auth emulator (untuk dev & uji rules)
```

## Langkah pertama untuk sesi pertama

1. Baca semua file di `docs/`.
2. Tanyakan Pertanyaan Terbuka no. 1–2 ke pemilik proyek bila belum terjawab.
3. Ikuti `docs/GITHUB_SETUP.md` (repo, label, milestone, CI) — bantu pemilik proyek menjalankan perintahnya; jangan melakukan aksi yang mengubah akun GitHub/Firebase tanpa konfirmasi.
4. Kerjakan **M0 — Fondasi** di `docs/ROADMAP.md`.
