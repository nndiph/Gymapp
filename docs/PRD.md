# PRD — Repset (working title)

Versi: 0.1 (draf perencanaan) · Terakhir diperbarui: 2026-11-10

## 1. Visi

Gym tracker yang **tidak membatasi gerakan** dan membuat progress latihan **bisa dilihat dan dibanggakan** — kombinasi pencatat latihan dan profil sosial ala GitHub (heatmap kontribusi) + Instagram (profil yang bisa dilihat orang lain).

## 2. Masalah

1. App gym di Play Store sering tidak menyediakan gerakan yang dilakukan user (variasi, mesin lokal, gerakan unik).
2. Progress latihan terkunci di dalam app; sulit dibagikan atau dipamerkan secara konsisten.
3. Banyak app terlalu bergantung pada koneksi, padahal sinyal di gym sering buruk.

## 3. Target pengguna

- **Utama:** orang yang rutin ke gym dan mencatat latihan (pemula–menengah), pengguna Android, nyaman berbahasa Indonesia.
- **Sekunder:** teman satu gym yang ingin saling melihat progress.

## 4. Prinsip produk

1. **Cepat dicatat.** Mencatat set berikutnya harus ≤ 3 ketukan (nilai terakhir otomatis terisi).
2. **Selalu bisa dipakai.** Fitur inti jalan tanpa internet.
3. **Gerakan milik user.** Gerakan custom adalah warga kelas satu, bukan fitur tersembunyi.
4. **Sosial ringan.** Sosial menambah motivasi, bukan menambah beban. Privasi di tangan user.

## 5. Scope MVP

Tiga area utama + fondasi pendukung.

### 5.1 Catat gerakan & latihan (inti)

| ID | User story | Acceptance criteria |
|---|---|---|
| US-01 | Sebagai user, aku bisa mencari gerakan dari library bawaan | Pencarian berdasarkan nama; filter otot utama & alat; hasil muncul tanpa internet |
| US-02 | Aku bisa membuat gerakan custom | Isi nama, otot utama, alat, tipe pencatatan (beban+reps / bodyweight reps / durasi / jarak+durasi). Opsional: "variasi dari" gerakan lain. Tersimpan dan langsung muncul di pencarian |
| US-03 | Aku bisa mengubah/menghapus gerakan custom | Menghapus tidak merusak riwayat lama (riwayat menyimpan snapshot nama gerakan) |
| US-04 | Aku bisa memulai sesi latihan (kosong atau dari routine) | Sesi punya tanggal, nama, daftar gerakan, dan set |
| US-05 | Aku bisa mencatat set | Input beban & reps (atau durasi/jarak sesuai tipe); nilai sesi terakhir untuk gerakan itu terisi otomatis; tandai warm-up; hapus/ubah set |
| US-06 | Aku bisa menyelesaikan sesi | Tersimpan lokal seketika (tanpa menunggu jaringan), tersinkron otomatis saat online |
| US-07 | Aku bisa melihat, mengubah, dan menghapus riwayat sesi | Daftar sesi terbaru → detail; perubahan memperbarui statistik |
| US-08 | Aku bisa membuat routine | Nama, daftar gerakan dengan target set/reps, hari-hari pelaksanaan, jam pengingat |

### 5.2 Profil (progress yang bisa dilihat)

| ID | User story | Acceptance criteria |
|---|---|---|
| US-10 | Aku punya profil | Username unik, nama tampilan, bio singkat |
| US-11 | Profilku menampilkan heatmap 12 bulan | Warna per hari menurut jumlah/volume latihan; ketuk hari → ringkasan sesi |
| US-12 | Profilku menampilkan progress | Statistik bulan ini (jumlah sesi, total volume, streak); PR per gerakan; grafik progress per gerakan |
| US-13 | Routine-ku tampil sebagai "playlist" | Daftar routine di profil; orang lain bisa melihat isi routine |
| US-14 | Aku mengatur privasi | Profil publik atau privat. Privat = hanya aku yang bisa melihat detail |
| US-15 | Aku bisa follow/unfollow user lain | Cari username → buka profil publik → follow; jumlah follower/following tampil |

### 5.3 Dashboard

| ID | User story | Acceptance criteria |
|---|---|---|
| US-20 | Dashboard menunjukkan latihan hari ini | Berdasarkan routine & hari; tombol "Mulai"; jika tidak ada jadwal, tawarkan sesi kosong |
| US-21 | Aku menerima pengingat latihan | Notifikasi lokal pada jam yang diatur, hanya di hari routine; tidak muncul jika hari itu sudah ada sesi tercatat |
| US-22 | Dashboard menunjukkan ringkasan | Streak, sesi minggu ini, shortcut ke profil/riwayat/gerakan |
| US-23 | App meminta izin notifikasi dengan baik | Menjelaskan alasan sebelum meminta izin; tetap berguna jika izin ditolak |

### 5.4 Fondasi

- Login (Google + email/password), buat username.
- Hapus akun (kebutuhan kebijakan Play Store).
- Crash reporting.

## 6. Non-goals (MVP)

Feed berisi postingan, like/komentar, DM, foto/video progress, library gerakan publik/komunitas, iOS, web, pembayaran/langganan, rekomendasi AI, integrasi wearable. Ide-ide ini dicatat di backlog, tidak dibangun sebelum MVP stabil.

## 7. Metrik keberhasilan (usulan target)

- Crash-free sessions ≥ 99% pada closed test.
- Mencatat set berulang ≤ 3 ketukan.
- Fitur inti 100% berfungsi dalam mode pesawat (setelah login pertama).
- Closed test Play Store: ≥ 12 tester aktif selama 14 hari berturut-turut (target aman: rekrut 15–20 orang karena ada yang gugur).
- Waktu buka app sampai bisa mencatat set < 3 detik pada perangkat menengah.

## 8. Risiko & mitigasi

| Risiko | Mitigasi |
|---|---|
| Layanan backend down | Offline-first; tidak ada server buatan sendiri; Crashlytics + budget alert |
| Biaya Firestore membengkak | Seed gerakan di-bundle (bukan dibaca dari Firestore); agregasi bulanan; limit query; budget alert |
| Scope membengkak | Non-goals tegas; milestone kecil; ide baru masuk backlog |
| Gerakan custom jadi berantakan/duplikat | Custom bersifat privat per user di MVP; berbagi ke library publik ditunda dengan moderasi |
| Penolakan Play Store (akun personal baru) | Mulai closed test lebih awal; siapkan privacy policy, hapus akun, pelaporan/blokir konten sosial |
| Notifikasi tidak muncul di sebagian HP (penghemat baterai vendor) | Uji di beberapa merek; beri panduan pengecualian baterai di pengaturan |
