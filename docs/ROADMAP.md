# ROADMAP — Liftverse

Versi: 0.2 · Terakhir diperbarui: 2026-11-11

Ukuran relatif: **S** (sekitar satu sesi kerja), **M** (beberapa sesi), **L** (banyak sesi). Sengaja tanpa tanggal; pemilik proyek menyesuaikan dengan jadwalnya.

Setiap butir bisa langsung dijadikan GitHub Issue (lihat `GITHUB_SETUP.md`). Satu milestone = satu GitHub Milestone.

## Definition of Done (berlaku untuk semua issue)

- [ ] Build debug sukses & CI hijau
- [ ] Unit test untuk logika baru (mapper, perhitungan, ViewModel)
- [ ] Berjalan tanpa internet jika fiturnya termasuk fitur inti
- [ ] Tidak ada string UI hardcode; tidak ada secret ter-commit
- [ ] Dokumen terkait diperbarui bila ada perubahan keputusan
- [ ] PR kecil, deskripsi jelas, direview sendiri (self-review) sebelum merge

---

## M0 — Fondasi (M)

Status per 2026-11-11: kode fondasi selesai dan terverifikasi lokal. Sisa butir menunggu keputusan atau akun pemilik proyek.

- [x] Putuskan nama → `applicationId`: **Liftverse**, `com.nndiph.liftverse` (2026-11-11)
- [ ] Buat repo, ikuti `GITHUB_SETUP.md` (label, milestone, ruleset, CI). *Repo lokal + file `.github/` siap; menunggu `gh auth login` dan nama akun/repo.*
- [x] Proyek Android baru: Kotlin, Compose, Material 3, minSdk 26
- [x] Cek dan tetapkan versi terbaru (AGP, Kotlin, Compose BOM, Firebase BoM, Hilt) dari dokumentasi resmi → tabel di `ARCHITECTURE.md` §10
- [x] Hilt, Navigation Compose, tema (light/dark), kerangka bottom navigation (Dashboard · Catat · Profil). *Palet biru masih usulan sampai desain final ada.*
- [ ] Proyek Firebase: Auth, Firestore (mode produksi, region Asia), Crashlytics; `google-services.json` lokal + `.example`. *SDK + `.example` sudah terpasang; proyek di Firebase Console belum dibuat.*
- [x] Firebase Emulator Suite berjalan lokal; `firestore.rules` awal "tolak semua" (diuji: baca & tulis tanpa login → 403)
- [x] ktlint + CI (ktlint, lint, test, assembleDebug). *detekt ditunda: versi 2.x masih alpha per 2026-11-10.*
- [x] Isi bagian "Menjalankan proyek" di README

## M1 — Auth & profil dasar (M)

- [ ] Login Google (Credential Manager) + email/password
- [ ] Layar buat username (transaction `usernames/*`), validasi format
- [ ] Dokumen `users/{uid}` dibuat; sesi login bertahan
- [ ] Logout; tangani error jaringan dengan pesan jelas
- [ ] Rules + uji emulator untuk `users` dan `usernames`

## M2 — Library gerakan (M)

- [ ] Skrip `tools/seed` mengonversi dataset terpilih → `exercises.json` (setelah lisensi diverifikasi; simpan atribusi)
- [ ] Muat seed ke memori; pencarian (nama + alias) & filter (otot, alat) — offline
- [ ] Buat gerakan custom (form: nama, otot, alat, tipe pencatatan, "variasi dari", catatan)
- [ ] Ubah / arsipkan gerakan custom; larang ubah `trackingType` bila sudah dipakai
- [ ] Gabungkan seed + custom dalam satu picker
- [ ] Rules + uji untuk `customExercises`

## M3 — Catat latihan & riwayat (L) — inti produk

- [ ] Model domain `Session`, `SetEntry`, `Routine`; mapper ↔ Firestore
- [ ] Mulai sesi kosong; tambah gerakan lewat picker
- [ ] Input set per `trackingType`; auto-isi dari sesi terakhir; tandai warm-up; hapus set
- [ ] Draf sesi aktif tersimpan lokal (anti-hilang jika app dimatikan)
- [ ] Selesai sesi → tulis dokumen sesi + `recompute-day` ke `monthlyStats` dalam satu batch
- [ ] Riwayat: daftar (paginasi) → detail → ubah/hapus (statistik ikut dihitung ulang)
- [ ] Routine: buat/ubah, hari pelaksanaan, jam pengingat; mulai sesi dari routine
- [ ] Indikator status sinkron
- [ ] **Uji mode pesawat penuh** (alur lengkap) dan catat hasilnya di PR
- [ ] Rules + uji untuk `sessions`, `routines`, `monthlyStats`

## M4 — Profil & progress (L)

- [ ] Header profil (nama, username, bio, edit)
- [ ] Heatmap 12 bulan dari `monthlyStats`; ketuk hari → ringkasan
- [ ] Statistik bulan ini & streak
- [ ] `exerciseStats` (PR, e1RM, titik terbaru) dan layar progress per gerakan + grafik
- [ ] Routine tampil sebagai "playlist" di profil
- [ ] Pengaturan privasi (publik/privat)
- [ ] Rules + uji untuk `exerciseStats` & visibilitas profil

## M5 — Dashboard & notifikasi (M)

- [ ] Kartu "Latihan hari ini" dari routine + hari; tombol Mulai; fallback sesi kosong
- [ ] Ringkasan: streak, sesi minggu ini
- [ ] Notification channel + permintaan izin kontekstual (Android 13+)
- [ ] Penjadwal pengingat (WorkManager), terjadwal ulang setelah reboot/ubah routine
- [ ] Lewati pengingat jika hari ini sudah ada sesi
- [ ] Layar pengaturan notifikasi + petunjuk pengecualian baterai
- [ ] Uji di beberapa perangkat/merek; catat temuan

## M6 — Sosial minimal (M)

- [ ] Cari user berdasarkan username
- [ ] Profil user lain (hanya jika publik): heatmap, progress ringkas, routine
- [ ] Follow/unfollow (batch dua dokumen); jumlah via `count()`
- [ ] Daftar followers/following
- [ ] Rules + uji lintas-user (baca profil privat harus ditolak)

## M7 — Hardening & rilis Play Store (L)

**Teknis**
- [ ] Review menyeluruh security rules; uji penyalahgunaan; validasi bentuk data
- [ ] Aktifkan Firebase App Check (Play Integrity)
- [ ] Pindah ke Blaze + **budget alert**; tinjau pola read Firestore
- [ ] Strategi backup/ekspor Firestore diputuskan & dicatat
- [ ] Performa: startup, scroll riwayat, ukuran APK/AAB
- [ ] Aksesibilitas dasar (ukuran sentuh, kontras, content description)

**Kebijakan & Play Console** *(verifikasi persyaratan terbaru di dokumentasi Play sebelum submit)*
- [ ] Kebijakan privasi (URL publik) + formulir Data Safety
- [ ] Hapus akun di dalam app + tautan penghapusan di web
- [ ] Fitur laporkan & blokir (karena ada konten buatan pengguna)
- [ ] Play App Signing, build AAB, ikon & aset toko, deskripsi (ID + EN), content rating
- [ ] Akun developer personal baru: jalankan **closed test ≥ 12 tester opt-in selama 14 hari berturut-turut** sebelum mengajukan akses produksi; rekrut 15–20 tester sebagai cadangan
- [ ] Ajukan akses produksi → rilis bertahap (staged rollout)

## Backlog pasca-MVP (jangan dikerjakan sebelum M7)

Feed & postingan, like/komentar, follow request untuk akun privat, library gerakan komunitas dengan moderasi, foto progress (Storage/Blaze), unit lb, rest timer lanjutan, ekspor data (CSV), widget layar utama, notifikasi sosial (FCM), terjemahan nama gerakan, Wear OS, iOS.
