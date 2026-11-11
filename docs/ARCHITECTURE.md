# ARCHITECTURE — Repset (working title)

Versi: 0.1 · Terakhir diperbarui: 2026-11-10

## 1. Tujuan arsitektur

1. **Jarang down** — tidak ada server yang dikelola sendiri; app tetap berguna saat backend bermasalah.
2. **Offline-first** — mencatat latihan tidak boleh gagal karena sinyal gym.
3. **Murah dan terprediksi** — MVP muat di paket gratis; biaya saat rilis bisa dikontrol.
4. **Mudah dikerjakan bertahap** — satu modul, lapisan jelas, tanpa abstraksi berlebihan.

## 2. Keputusan stack: Firebase (bukan Supabase)

Riset per November 2026. Angka dari pelacak pihak ketiga tidak seragam, jadi baca sebagai gambaran umum, bukan skor pasti.

| Aspek | Firebase (Firestore) | Supabase |
|---|---|---|
| Jaminan uptime | SLA kontraktual: Firestore regional ≥ 99,99%, multi-region ≥ 99,999% (praktisnya berlaku di akun berbayar/Blaze) | SLA 99,9% **hanya Enterprise**; Pro/Team tanpa SLA; database single-node secara default, tanpa add-on HA yang dipublikasikan |
| Perilaku paket gratis | Tidak di-pause saat idle; saat kuota habis, layanan berhenti merespons sampai reset | Project gratis **di-pause setelah 1 minggu tidak aktif** dan harus dipulihkan manual |
| Insiden tercatat | Mayoritas di produk pinggiran (Test Lab, log Hosting). Ada insiden serius: Realtime Database 6 Jan 2026 (±6,5 jam, termasuk asia-southeast1) | Banyak insiden (±77 sampai ±187 tergantung pelacak), kebanyakan minor/dashboard; ada yang Major, mis. "Unresponsive Projects" 10 Sep 2026 |
| Offline | Cache offline + sinkronisasi otomatis bawaan SDK Android | Harus dibangun sendiri (SQLite + sync) |
| Model data | NoSQL (perlu desain per-dokumen) | Postgres relasional (lebih nyaman untuk query kompleks) |
| Biaya | Spark gratis; Blaze bayar-per-pakai, tanpa biaya dasar | Gratis → Pro mulai $25/bulan |

**Keterbatasan riset ini:** insiden Firestore dilaporkan di *Cloud Service Health* (bukan halaman status Firebase), sehingga data insiden Firestore spesifik tidak lengkap. Jangan menganggap Firebase "tidak pernah down" — itulah alasan desain offline-first di bawah.

**Kapan meninjau ulang keputusan ini:** jika query sosial menjadi kompleks (feed, ranking, pencarian), biaya read Firestore tidak terkendali, atau SQL relasional jadi kebutuhan nyata. Catat sebagai ADR baru di `docs/` jika berubah.

## 3. Gambaran sistem

```
┌────────────────────── Android App ──────────────────────┐
│ UI (Compose)  ←→  ViewModel  ←→  Repository             │
│                                      │                  │
│                  ┌───────────────────┼──────────────┐   │
│                  │                   │              │   │
│        Seed exercises JSON    Firestore SDK     WorkManager│
│        (assets, read-only)   (cache offline)   (pengingat) │
└──────────────────────────────┬──────────────────────────┘
                               │ sinkron otomatis saat online
                     Firebase: Auth · Firestore · Crashlytics · App Check
```

Tidak ada backend buatan sendiri. Tidak ada Cloud Functions di MVP.

## 4. Lapisan aplikasi

- **UI (Compose):** layar stateless; menerima `UiState`, mengirim `UiEvent`.
- **ViewModel:** menyimpan `StateFlow<UiState>`; memanggil use case/repository; tidak tahu Firebase.
- **Domain:** model murni Kotlin (`Exercise`, `Session`, `SetEntry`, `Routine`, `UserProfile`) + interface repository. Use case hanya jika ada logika yang dipakai lebih dari satu tempat (mis. menghitung PR, volume).
- **Data:** implementasi repository, `FirestoreDataSource`, `SeedExerciseDataSource`, mapper DTO ↔ domain.
- **DI:** Hilt. **Navigasi:** Navigation Compose, bottom bar: Dashboard · Catat · Profil.

## 5. Offline-first (aturan inti)

1. **Seed gerakan** (±ratusan item) dibundel sebagai `assets/seed/exercises.json` dan dimuat ke memori saat start. Tidak dibaca dari Firestore → hemat kuota, instan, offline penuh.
2. **Gerakan custom, sesi, routine, statistik** disimpan di Firestore; cache offline SDK aktif secara default di Android.
3. **Jangan `await` write di jalur UI.** Saat offline, `Task` write Firestore baru selesai ketika server mengonfirmasi. UI memakai snapshot listener/cache sebagai sumber kebenaran; write dipicu "fire-and-observe".
4. **Sesi aktif** (yang sedang berjalan) sebaiknya punya draf lokal (mis. disimpan di DataStore/Room ringan) agar tidak hilang jika app dimatikan sistem sebelum "Selesai". Putuskan implementasi di M3 dan catat di PR.
5. **Indikator sinkron** kecil di UI (mis. ikon awan) agar user tahu data belum terkirim — mengurangi kecemasan data hilang.
6. **Uji wajib:** mode pesawat penuh untuk alur catat → selesai → lihat riwayat → nyalakan jaringan → data muncul di Firestore.

## 6. Notifikasi latihan (lokal)

- Jadwal diturunkan dari routine (`weekdays` + `reminderTime`). Tidak butuh server.
- Gunakan WorkManager untuk menjadwal ulang pengingat berikutnya setelah setiap kemunculan dan setelah reboot. Toleransi beberapa menit dapat diterima; **hindari exact alarm** kecuali benar-benar perlu (butuh izin khusus dan dibatasi kebijakan Play — verifikasi aturan terbaru sebelum memutuskan).
- Android 13+: minta izin `POST_NOTIFICATIONS` secara kontekstual (jelaskan manfaat dulu). App harus tetap berguna jika ditolak.
- Saat pengingat akan tampil, cek apakah hari ini sudah ada sesi tercatat; jika sudah, lewati.
- Buat notification channel khusus ("Pengingat latihan").
- Sebagian vendor (mis. penghemat baterai agresif) dapat menunda/mematikan pekerjaan latar. Uji di beberapa merek dan sediakan petunjuk di Pengaturan.
- FCM (push dari server) baru dipertimbangkan saat ada notifikasi sosial (Fase pasca-MVP).

## 7. Keamanan

- **Security rules Firestore** menjadi garis pertahanan utama (lihat `DATA_MODEL.md`). Default tolak semua; buka seperlunya. Uji dengan Emulator.
- **Firebase App Check** (Play Integrity) diaktifkan sebelum rilis publik untuk menekan penyalahgunaan kuota.
- **Auth:** Google Sign-In (Credential Manager) + email/password. Verifikasi email dianjurkan sebelum fitur sosial.
- **Secrets:** `google-services.json` tidak di-commit (sediakan `.example`); keystore tidak pernah di-commit. Detail di `GITHUB_SETUP.md`.
- **Konten sosial:** sediakan pelaporan & blokir sebelum rilis (kebijakan Play untuk konten buatan pengguna — verifikasi aturan terbaru).
- **Hapus akun:** hapus data user di Firestore + akun Auth, dari dalam app dan lewat tautan web (kebutuhan Play).

## 8. Observabilitas & keandalan

- **Crashlytics** sejak M0. **Firebase Performance** opsional.
- **Budget alert** di Google Cloud Billing saat pindah ke Blaze (mis. alert di $1, $5, $10).
- Pantau halaman status: Firebase dan *Google Cloud Service Health* (Firestore).
- **Backup:** pelajari opsi backup/PITR/ekspor terjadwal Firestore (umumnya butuh Blaze + bucket) sebelum rilis publik; catat keputusan di PR M7.
- **Degradasi anggun:** jika Firestore tidak terjangkau, app tetap menampilkan data cache dan mengantre perubahan; layar sosial menampilkan status "tidak bisa memuat" tanpa crash.

## 9. Kontrol biaya (Firestore dihitung per dokumen dibaca/ditulis)

- Seed gerakan dibundel (nol read).
- Satu sesi = **satu dokumen** (set tertanam di dalamnya) → membuka riwayat murah.
- Heatmap membaca **12 dokumen** `monthlyStats`, bukan ratusan sesi.
- Gunakan `count()` aggregation untuk jumlah follower/following (tanpa Cloud Functions).
- Pagination (`limit`) untuk daftar riwayat; hindari listener pada koleksi tanpa batas.
- Aturan keamanan yang memakai `get()` ikut dihitung sebagai read — gunakan seperlunya.
- Kuota Spark (saat ini ±50 ribu read, 20 ribu write per hari — verifikasi di halaman harga) cukup untuk MVP skala kecil; desain di atas sengaja hemat.

## 10. Teknologi & versi

Versi ditetapkan di M0 (dicek ke rilis resmi pada 2026-11-10) dan dicatat di `gradle/libs.versions.toml`, sumber kebenaran satu-satunya. Saat menaikkan versi, cek release notes resmi; jangan menyalin dari ingatan.

| Komponen | Versi | Catatan |
|---|---|---|
| Android Gradle Plugin | 9.5.1 | Kotlin built-in (plugin `org.jetbrains.kotlin.android` tidak dipakai), DSL baru. Max API 37 |
| Gradle | 9.9.1 | Minimum AGP 9.5 adalah 9.6.0; dinaikkan ke stabil terbaru atas saran Android lint |
| Kotlin | 2.4.21 | Dipasang lewat `buildscript` di root (AGP 9 bawa KGP sendiri) |
| KSP | 2.3.12 | 2.3.13 punya regresi yang menghilangkan binding Hilt (google/ksp#3286) |
| Compose BOM | 2026.09.00 | Compose 1.12.x, Material 3 1.4.0; butuh `compileSdk 37` |
| Firebase BoM | 35.1.0 | minSdk 24+ |
| Hilt / androidx.hilt | 2.60.1 / 1.4.0 | Hilt butuh AGP 9 |
| Navigation Compose | 2.10.2 | Rute string dulu; type-safe routes dipertimbangkan nanti |
| WorkManager | 2.12.0 | Dipakai mulai M5 |
| Credential Manager | 1.6.0 | Dipakai mulai M1 |
| ktlint-gradle | 14.2.0 | Gaya `android_studio`, lihat `.editorconfig` |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 | |
| Ikon UI | Material Symbols (vector XML di `res/drawable`) | Artefak `material-icons-*` tidak lagi dianjurkan Google (lambat di-build, tampilan lama). Unduh XML dari fonts.google.com/icons; lisensi Apache 2.0 |

Pustaka yang direncanakan: Compose Material 3, Navigation Compose, Hilt, Coroutines/Flow, Firebase BoM (Auth, Firestore, Crashlytics, App Check), Credential Manager, WorkManager, DataStore, Coil (jika nanti ada gambar), JUnit + Turbine + MockK (test), ktlint (kualitas kode; detekt menyusul saat 2.x stabil).

## 11. Referensi riset

- Firestore SLA: https://cloud.google.com/firestore/sla
- Supabase pricing & pause: https://supabase.com/pricing
- Ringkasan SLA/operasi Supabase: https://comparedge.com/tools/supabase/operations
- Status Firebase: https://status.firebase.google.com
- Syarat closed test Play (akun personal baru): https://support.google.com/googleplay/android-developer/answer/14151465
