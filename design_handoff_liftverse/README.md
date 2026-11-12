# Handoff: Liftverse (MVP Android)

## Overview
Liftverse adalah gym tracker Android dengan profil sosial ringan ("GitHub-nya gym"). Setiap sesi latihan menjadi riwayat publik: heatmap konsistensi, progress per gerakan, routine, dan musik latihan di profil. Pengguna: orang Indonesia pemula–menengah, mencatat dengan satu tangan di sela set, sering tanpa sinyal.

Empat prinsip yang harus dijaga saat implementasi:
1. **Cepat dicatat.** Konfirmasi set = 1 ketukan, ubah lalu konfirmasi ≤ 3 ketukan. Nilai sesi lalu terisi otomatis.
2. **Selalu bisa dipakai.** Fitur inti offline-first (Room sebagai sumber kebenaran). Tidak ada layar yang macet menunggu jaringan.
3. **Gerakan milik pengguna.** Gerakan custom setara dengan bawaan.
4. **Sosial ringan.** Hanya profil, follow, laporkan, blokir. Tidak ada feed, like, komentar, DM.

## About the Design Files
File di `design/` adalah **referensi desain berbentuk HTML** (prototype yang menunjukkan tampilan dan perilaku), **bukan kode produksi**. Tugasnya adalah **membangun ulang desain ini di Android dengan Jetpack Compose + Material 3**, memakai pola dan library proyek. Jangan menyalin HTML-nya.

Cara membuka: buka file `design/*.dc.html` langsung di browser (Chrome). `support.js` harus ada di folder yang sama. `LiftverseApp.dc.html` berisi **semua layar dalam satu komponen**; lembar 1–9 menyusunnya per topik. Prototype yang bisa diklik: `Liftverse 6 Prototype.dc.html`.

## Fidelity
**High-fidelity.** Warna, tipografi, radius, spacing, copy, dan interaksi sudah final untuk MVP. Bangun ulang sepresisi mungkin. Kode tema siap pakai ada di `theme/` (belum pernah dikompilasi; sesuaikan nama paket, `R.font`, dan import).

Satuan di HTML: 1 px = 1 dp (artboard 412 × 915 dp). Ukuran font px = sp.

## Urutan implementasi yang disarankan
1. **Tema**: salin `theme/Color.kt`, `Type.kt`, `Theme.kt`. Tambahkan font Archivo SemiExpanded (Bold, ExtraBold) dan Plus Jakarta Sans (Regular, Medium, SemiBold, Bold). Ikon: Material Symbols Rounded. Dynamic color **mati**.
2. **Data lokal**: Room untuk Exercise (bawaan + custom), Routine (+ RoutineExercise, hari, jam pengingat), Session, SessionExercise, Set, ScheduleConfig (jadwal streak), plus antrean sinkron. WorkManager untuk sinkron.
3. **Kerangka navigasi**: 3 tab (Dashboard, Catat, Profil), status bar 36, top app bar 64, navigation bar 80, gesture bar 24.
4. **Sesi aktif** (inti produk): baris set alternatif B, numpad, sheet plat, timer istirahat, pencapaian langsung, Selesaikan sesi.
5. **Selesai sesi** → **Dashboard** → **Profil + heatmap**.
6. **Pemilih gerakan** + **form gerakan custom**.
7. **Tab Catat** (mulai + kelola routine), **form routine**, **jadwal & streak**.
8. **Riwayat + detail**, **Progress** (minggu/bulan) dan **progress per gerakan**.
9. **Masuk, username, izin notifikasi**.
10. **Sosial** (cari, profil lain, privat, pengikut, laporkan/blokir) dan **musik latihan**.
11. **Pengaturan, Tentang**, lalu semua **kondisi khusus** (kosong, error, offline, pengguna baru, sangat aktif).

## Screens / Views
Semua layar ada di `design/LiftverseApp.dc.html`, dipilih lewat prop `screen` + `variant` (lihat tabel di bawah). Lembar referensi: 3 (layar kunci light/dark berdampingan), 4 (layar lain), 5 (kondisi khusus).

Kerangka umum: margin layar 16 dp; kartu radius 16, isi `surface`, garis 1 dp `outlineVariant`, tanpa bayangan; tombol utama 56 dp (radius 12, `primaryFill`, label `labelLarge`), tombol sekunder 48 dp; aksi utama selalu di separuh bawah layar (biasanya bar bawah sticky).

| screen | variant | Layar | Isi utama |
|---|---|---|---|
| `dashboard` | –, libur, baru, offline | Dashboard | Wordmark + chip sinkron; "Halo, Raka"; ringkasan streak + 7 lingkaran hari minggu ini; kartu **Latihan hari ini** (routine sesuai hari, 3 gerakan pertama + target, tombol Mulai 56 dp); progress ringkas vs sesi lalu / bulan lalu; pintasan Riwayat & Gerakan. `libur`: "Tidak ada jadwal" + Mulai sesi kosong + jadwal berikutnya. |
| `catat` | –, baru | Tab Catat tanpa sesi aktif | Kartu "Hari ini · Kamis" (routine terjadwal, Ubah, Mulai), "Routine lainnya" (baris dengan pratinjau gerakan + tombol teks Mulai 48 dp), baris "Buat routine". Bar bawah: Mulai Push Day + Mulai sesi kosong. Bila ada sesi aktif, tab langsung membuka Sesi. |
| `sesi` | –, numpad, naik, calon-pr, selesai, selesai-ubah, tipe, padat, offline, kosong, istirahat-habis | Sesi aktif | App bar: nama sesi, durasi berjalan + "x dari y set", chip sinkron. Kartu per gerakan (hanya yang berjalan terbuka). Baris set B. Bar bawah: timer istirahat (volt) di atas tombol Tambah gerakan + Selesai. |
| `selesai` | –, tanpa-pr | Selesai sesi | Judul "Sesi selesai"; grid durasi / set / volume / gerakan; waktu aktif vs istirahat; set naik dari sesi lalu; PR baru (kuningan); potongan heatmap dengan hari ini terisi; Lihat profil / Kembali ke dashboard. |
| `profil` | –, baru, aktif, musik-atur | Profil sendiri | Avatar inisial 72 dp, nama, @username, bio, pengikut/mengikuti (48 dp), chip privasi; **heatmap 6 bulan** (pita 26–27 minggu, chevron 6 bulan sebelumnya/berikutnya, sel bisa diketuk → ringkasan + navigasi sesi sebelumnya/berikutnya); statistik bulan ini; Rekor pribadi; Routine (gaya playlist); Musik latihan. |
| `profil-lain` | –, diikuti, menu, laporkan, blokir, offline | Profil publik orang lain | Sama dengan profil + tombol Ikuti/Mengikuti, menu ⋮ (Laporkan, Blokir). |
| `profil-privat` | –, offline | Profil privat | Identitas + jumlah saja, kartu "Profil ini privat", tanpa tombol Ikuti. |
| `masuk` | –, daftar | Masuk / Daftar | Tanda + wordmark besar, tagline; segmented Masuk/Daftar; Lanjutkan dengan Google; email + kata sandi; tombol utama; teks legal. |
| `username` | –, dipakai, format | Buat username | Field `@`, validasi live (^[a-z0-9_]{3,20}$), counter /20, 3 aturan dengan ikon, saran chip bila dipakai; Lanjutkan nonaktif sampai valid. |
| `notif` | –, sistem, ditolak | Izin notifikasi | Penjelasan + contoh notifikasi, lalu dialog sistem Android 13+; state ditolak tetap bisa lanjut. |
| `jadwal` | – | Jadwal & streak | Pilih mode: Hari tetap / Target per minggu / Split; kalender minggu yang bisa diketuk; opsi "Boleh tukar hari"; penjelasan cara streak dihitung. |
| `pemilih` | –, browse, dada, kosong | Pemilih gerakan | Search bar, chip filter Otot dan Alat, satu daftar bawaan + custom (tag Custom), tombol sticky "Buat gerakan custom" (jadi tombol utama berisi kata pencarian saat tidak ada hasil). |
| `gerakan-form` | –, dari-cari, ubah | Form gerakan custom | Nama, chip otot utama, chip alat, 4 tipe pencatatan (radio card), Variasi dari (opsional), Catatan; Arsipkan pada mode ubah. |
| `routine-form` | –, baru | Form routine | Nama; gerakan dengan stepper set & reps 48 dp, pegangan seret, tombol hapus (dengan Urungkan); Tambah gerakan; 7 tombol hari 48 dp; switch pengingat + jam; Hapus routine (mode ubah). |
| `riwayat` | –, kosong, error | Riwayat | Dikelompokkan per bulan; baris dengan tanggal besar, nama, tag PR, meta. |
| `riwayat-detail` | –, ubah, hapus | Detail sesi | Ringkasan, waktu aktif/istirahat, daftar set; Ubah (angka dapat diketuk) dan Hapus (dialog). |
| `progres` | –, bulan | Progress | Toggle Minggu/Bulan; batang volume per hari (minggu ini vs minggu lalu, terjadwal putus-putus); otot yang dilatih; daftar gerakan dengan delta. |
| `progress` | – | Progress per gerakan | Estimasi 1RM besar berlabel "estimasi" + rumus; grafik 12 bulan dengan titik PR; daftar rekor; sesi terakhir. |
| `musik` | –, preview, error, share | Tambah musik latihan | Tempel link (clipboard ditawarkan), pratinjau kartu, error playlist privat, mode "Bagikan ke Liftverse". |
| `cari` | –, kosong, offline | Cari pengguna | Search `@`, hasil dengan Ikuti 48 dp, akun privat bertanda gembok. |
| `koneksi` | –, mengikuti, kosong, offline | Pengikut / mengikuti | Dua tab, baris dengan Ikuti/Ikuti balik/Mengikuti. |
| `pengaturan` | –, ditolak, baterai, keluar, hapus | Pengaturan | Jadwal & streak, pengingat + panduan penghemat baterai, privasi Publik/Privat, akun (keluar, hapus akun dengan mengetik username), Tentang. |
| `tentang` | – | Tentang | Wordmark, versi, tautan legal. |

### Baris set (komponen terpenting)
- **Satu set aktif** per sesi, otomatis set pertama yang belum selesai. Panel: margin 4/6, padding 14/12/12, radius 16, `surfaceContainer`.
- Header panel: "Set N" (15, bold) + "Sesi lalu 80 kg × 8" (12, `onSurfaceVariant`, satu baris, ellipsis), chip pencapaian (lihat di bawah), tombol "Plat" 48 dp untuk gerakan barbell.
- Stepper: grid 2 kolom, gap 8, tinggi 76, radius 12, `surface`; tombol −/+ 46×56; nilai Archivo 800 28/32; keterangan 11 ("kg · ±2,5", "reps"). Ketuk angka → numpad (bottom sheet) dengan ±langkah, koma, hapus, Simpan.
- Baris bawah panel: hapus set, Warm-up toggle, **Selesai set** (56, `primaryFill`).
- Baris ringkas: tinggi 56; badge nomor 32 (radius 8; warm-up "W" dengan garis putus 1,5 dp); nilai Archivo 700 20; centang 32 di area 52 (selesai: isi `primaryFill` + ikon `onPrimaryFill`; belum: cincin 2 dp `outline`).
- Langkah: barbell 2,5 kg, dumbbell 2 kg, kabel 2,5 kg, reps 1, durasi 15 dtk, jarak 0,1 km. Mengubah beban set aktif ikut mengubah set berikutnya yang belum selesai dengan nilai sama.
- 4 tipe pencatatan: beban + reps, bodyweight reps (+ beban tambahan opsional), durasi, jarak + durasi. Contoh di lembar 2.

### Pencapaian langsung
Dihitung ulang tiap kali nilai set aktif berubah, sebelum Selesai set. Hanya chip kecil di header panel (tanpa panel teks):
- **Naik dari sesi lalu**: chip volt (`accent` / `onAccent`, tinggi 28, radius 8) "↑ +2,5 kg" bila beban naik dengan reps tidak turun > 1, atau reps naik di beban sama (durasi lebih lama, jarak lebih jauh).
- **Calon PR**: chip kuningan (`brass` / `onBrass`, ikon trophy) bila estimasi 1RM Epley melewati rekor. Menggantikan chip naik.
- contentDescription chip memuat detail ("Calon PR, estimasi 1RM 42 kg, rekor 35 kg"); umumkan via LiveRegion polite + getar pendek saat pertama muncul. Warm-up dan gerakan tanpa sesi lalu tidak dihitung.
- Setelah Selesai set, baris ringkas memakai tag "↑ +2,5 kg" (tinggi 22, radius 6) atau tag PR.

### Timer istirahat
- Mulai otomatis saat set dicentang. Default 2:30 (bench, squat, deadlift, OHP, row), 1:30 lainnya, 1:00 durasi/kardio.
- Panel volt radius 16 di atas tombol bawah: label 12 bold, sisa waktu Archivo 800 28, −15 / +15 / Lewati (48 dp), bilah progres linear 5 dp. Habis: "Waktunya set berikutnya" + hitung lebih (+0:12).
- Durasi sesi tetap berjalan selama istirahat. Waktu istirahat dicatat per set → Selesai sesi dan detail riwayat menampilkan waktu aktif vs istirahat dan rata-rata per set.
- Hitung dari timestamp (bukan countdown di memori). Notifikasi/getar saat aplikasi di latar.

## Interactions & Behavior
- **Navigasi bawah**: tepat 3 tab. Titik 8 dp pada ikon Catat = ada sesi aktif ("Catat, sesi sedang berjalan").
- **Alur utama** (prototype lembar 6): Dashboard → Mulai → centang 2 set → Selesai → Selesaikan sesi → Selesai sesi → Lihat profil → heatmap hari ini terisi + ringkasan terbuka.
- **Selesaikan sesi** (bottom sheet): menyebut jumlah set belum dicentang; bila struktur sesi berbeda dari routine, checkbox "Perbarui routine X" + ringkasan perubahan (default tidak dicentang); sesi kosong → "Simpan sebagai routine".
- **Hapus** (set, gerakan di sesi, gerakan di routine): langsung, dengan snackbar Urungkan 5 dtk. Menu ⋮ hanya bila ada ≥ 3 aksi; bila satu aksi, tampilkan ikon langsung.
- **Indikator sinkron**: chip 28 dp, ikon + teks: "Tersinkron" (cloud_done) / "Tersimpan di perangkat" (smartphone) / "Menyinkronkan". Tidak pernah memblokir.
- **Offline**: fitur inti tetap jalan. Layar sosial: batas waktu 8 dtk → "Tidak bisa memuat" + Coba lagi (48 dp), tanpa spinner tanpa batas.
- **Kosong / error**: ikon dalam kotak 64 dp (radius 20), judul 18 bold, satu kalimat 14, satu aksi. Error jaringan netral (cloud_off); error data lokal `errorContainer`.
- **Validasi username**: debounce 400 ms; state tersedia (`success`), dipakai (+ 3 saran), format salah (`error`, pesan menyebut aturan).
- **Heatmap**: seluruh sel bisa diketuk; terpilih = cincin 2 dp `onSurface`; hari ini = cincin `primary` (putus-putus bila kosong); masa depan = garis putus `outlineVariant`.

## State Management
- **Sesi aktif**: sessionId, startAt, daftar exercise (routine asal + struktur awal untuk diff), set (w, r, t, d, warm, done, pr, nilai sesi lalu), set aktif (otomatis/terpilih), rest (startAt, target), restLog per set.
- **Selesai sesi**: durasi, waktu aktif/istirahat, set, volume, PR baru, set naik per gerakan, flag perbarui routine.
- **Jadwal**: mode (hari tetap / target per minggu / split), hari latihan, target, boleh tukar hari.
- **Profil**: heatmap per hari (volume → level), halaman 6 bulan, hari terpilih, privasi, follow state, musik (≤ 3 tautan + metadata cache).
- **Server**: profil publik, follow, laporan/blokir, metadata musik (oEmbed/Open Graph dibaca di server). Semua data latihan dibaca dari lokal.

## Rumus
- Volume set = kg × reps (bodyweight: beban tambahan saja). Warm-up ikut volume, tidak ikut PR.
- Estimasi 1RM (Epley) = beban × (1 + reps / 30). Contoh 100 kg × 5 = 116,7 kg. Selalu berlabel "estimasi".
- Level heatmap dari volume harian: 0 tanpa sesi, 1 < 3.000, 2 < 4.500, 3 < 6.000, 4 ≥ 6.000 kg.
- Streak mengikuti jadwal pengguna (layar `jadwal`), bukan per hari kalender. Rincian di SPEC.md.
- Format angka id-ID: 41.200 · 116,7 · 2,5. Berat selalu kg.

## Design Tokens

### Warna (palet grafit + volt; sumber kebenaran: `theme/Color.kt`)
| Peran M3 | Light | Dark | Dipakai untuk |
|---|---|---|---|
| `primary` | #536B00 | #CDEB3B | Teks/ikon aktif, tautan, tab aktif, grafik |
| `onPrimary` | #FFFFFF | #1C2400 |  |
| `primaryContainer` | #ECF5C4 | #2A3510 | Chip terpilih, indikator tab, warm-up aktif |
| `onPrimaryContainer` | #2E3B05 | #E3F59A |  |
| `inversePrimary` | #CDEB3B | #536B00 | Aksi snackbar (Urungkan) |
| `secondary` | #4D5754 | #B3BCB8 |  |
| `onSecondary` | #FFFFFF | #1F2624 |  |
| `secondaryContainer` | #E2E7E5 | #283030 | Tombol tonal (Mulai routine, Ubah) |
| `onSecondaryContainer` | #1A211E | #D7DEDB |  |
| `tertiary` | #8A5A00 | #E9A93C | Teks kuningan (label PR) |
| `onTertiary` | #FFFFFF | #2B1C00 |  |
| `tertiaryContainer` | #FBEBCB | #3A2A10 | Latar ikon PR |
| `onTertiaryContainer` | #5A3A00 | #F5CB80 |  |
| `background` | #F5F7F6 | #0B0D0D | Latar layar |
| `onBackground` | #111614 | #EDF1EF |  |
| `surface` | #FFFFFF | #131716 | Kartu, daftar |
| `onSurface` | #111614 | #EDF1EF | Teks utama |
| `surfaceVariant` | #DDE3EC | #29344A |  |
| `onSurfaceVariant` | #49514E | #A7B0AC | Teks sekunder, isian dari sesi lalu |
| `inverseSurface` | #1E2422 | #EDF1EF | Snackbar |
| `inverseOnSurface` | #EDF1EF | #1E2422 |  |
| `error` | #A63D32 | #EFA59A | Validasi, hapus |
| `onError` | #FFFFFF | #3B0E09 |  |
| `errorContainer` | #F8E4E1 | #47201B | Banner error lokal |
| `onErrorContainer` | #6E1F17 | #F8D6D0 |  |
| `outline` | #78807D | #6F7875 | Batas field/chip/checkbox (≥3:1) |
| `outlineVariant` | #D5DBD8 | #2A3130 | Garis kartu, pemisah |
| `scrim` | #050810 (α 73) | #000000 (α 99) |  |
| `surfaceBright` | #FFFFFF | #29344A |  |
| `surfaceDim` | #DDE3EC | #0A0F1C |  |
| `surfaceContainerLowest` | #FFFFFF | #0E1110 |  |
| `surfaceContainerLow` | #F0F2F1 | #171B1A | Bottom sheet |
| `surfaceContainer` | #EAEDEC | #1B201F | Nav bar, panel set aktif, chip sinkron |
| `surfaceContainerHigh` | #E2E6E4 | #222827 | Dialog, search bar |
| `surfaceContainerHighest` | #D9DEDC | #2A302F | Track switch, tombol nonaktif |

### Warna tambahan (`LiftverseTheme.colors`)
| Token | Light | Dark | Dipakai untuk |
|---|---|---|---|
| `primaryFill` | #1A1F14 | #CDEB3B | Latar tombol utama, centang set selesai |
| `onPrimaryFill` | #CDEB3B | #1C2400 | Label tombol utama |
| `accent` | #CDEB3B | #CDEB3B | Volt: timer istirahat, chip Hari ini, tag naik, tanda wordmark (dark) |
| `onAccent` | #1C2400 | #1C2400 | Teks di atas volt |
| `accentText` | #536B00 | #CDEB3B | Angka streak, tanda wordmark (light) |
| `accentContainer` | #F0F8CF | #263010 | Latar lembut volt |
| `onAccentContainer` | #34430A | #E3F59A |  |
| `brass` | #E9A93C | #E9A93C | Badge PR saja |
| `onBrass` | #2B1C00 | #2B1C00 |  |
| `success` | #2E7652 | #7DC6A0 | Username tersedia |
| `onSuccess` | #FFFFFF | #0B2A1C |  |
| `successContainer` | #E0F0E7 | #15352A |  |
| `onSuccessContainer` | #134330 | #CBEBD9 |  |
| `heatmap` 0→4 | #E6EAE7 → #CCE277 → #A2C22E → #6E8C10 → #3F5206 | #1A1F1E → #344516 → #557220 → #93B92C → #D4F04A | Tingkat konsistensi |

Aturan warna: tombol utama selalu `primaryFill` + `onPrimaryFill` (light: grafit dengan label volt; dark: volt dengan label gelap). Volt di light mode hanya sebagai isi dengan teks `onAccent`. Kuningan hanya untuk PR. Kontras teks ≥ 4,5:1 dan komponen ≥ 3:1 sudah dicek di kedua mode.

### Tipografi (`theme/Type.kt`, semua `fontFeatureSettings = "tnum"`)
| Slot | Spesifikasi | Contoh |
|---|---|---|
| displayLarge | Archivo SemiExpanded 800, 56/60, −0,01em | 41.200 |
| displayMedium | Archivo 700, 36/40 | 116,7 kg |
| displaySmall | Archivo 800, 32/38 | Sesi selesai |
| headlineLarge | Archivo 800, 30/34 | Push Day |
| headlineMedium | Archivo 800, 28/34 | Judul onboarding |
| headlineSmall | Archivo 800, 24/30, −0,01em | Wordmark |
| titleLarge | Plus Jakarta Sans 700, 22/28 | Top app bar |
| titleMedium | Jakarta 600, 16/24 | Nama gerakan |
| titleSmall | Jakarta 700, 14/20 | Tombol 48 dp |
| bodyLarge / Medium / Small | Jakarta 400, 16/24 · 14/20 · 12/16 | Isi |
| labelLarge | Jakarta 700, 16/24 | Tombol 56 dp |
| labelMedium / Small | Jakarta 600, 12/16 · 11/16 | Tag, sumbu heatmap |
| numbers.stepper | Archivo 800, 28/32 | Nilai stepper |
| numbers.setRow | Archivo 700, 20/24 | Baris set ringkas |
| numbers.stat | Archivo 800, 26/32 | Statistik profil |
| numbers.small | Archivo 700, 16/20 | Timer, jumlah pengikut |

Teks UI Bahasa Indonesia, sentence case, tanpa huruf kapital semua.

### Radius (`theme/Theme.kt`)
2 sel heatmap · 6 tag (Custom, PR, Hari ini, naik) · 8 chip, badge nomor set, tile routine · 12 tombol, field, stepper, banner · 16 kartu, panel set aktif, timer, FAB · 28 bottom sheet (atas), dialog · penuh: avatar, indikator nav, switch.

### Spacing
4 ikon ke label · 8 antar chip/tombol · 12 antar kartu, celah stepper · 16 margin layar, padding kartu · 24 antar seksi · 32 layar kosong · 48 target sentuh minimum.

### Bayangan
Tidak ada. Kedalaman lewat tingkatan surface + garis 1 dp. `tonalElevation = 0.dp`, `surfaceTint = Transparent`.

## Assets
- `icon/liftverse-mark.svg`: tanda Liftverse (tiga bilah dalam orbit), hasil trace dari desain pengguna. **Draft**, akan diganti ekspor dari file desain asli.
- `icon/ic_launcher_foreground.png` (432 px, tanda #1C2400), latar adaptive icon `#CDEB3B`; `ic_launcher_monochrome.png`; `ic_launcher_foreground_alt_gelap.png` (alternatif latar #131716).
- `icon/playstore_512.png`, `icon/splash_icon_light.png` / `splash_icon_dark.png` (SplashScreen API; iconBackground #CDEB3B light / #131716 dark).
- `icon/ic_stat_liftverse.svg` + `ic_stat_notification_96.png`: ikon notifikasi 24 dp, tiga pilar tanpa orbit. Draft.
- Ikon UI: Material Symbols Rounded 24 dp, wght 400, FILL 0 default / 1 aktif.
- Avatar = inisial (belum ada unggah foto). Cover musik diambil dari platform (placeholder bergaris di desain).

## Screenshots
`screenshots/light/` (38 layar) dan `screenshots/dark/` (12 layar kunci), 412 × 915 px = 1 px per dp, diambil dari `design/LiftverseApp.dc.html`. Nama file mengikuti `screen` dan `variant` di tabel Screens. Gunakan sebagai pembanding visual. Gambar dibuat dengan render ulang DOM, jadi beberapa judul tebal yang di-center (misalnya nama di profil, judul layar kosong) bisa terlihat terpotong ke dua baris dan menumpuk; itu artefak tangkapan, bukan desain. Untuk ukuran, wrapping, dan warna yang presisi, buka file HTML di Chrome atau lihat token di atas.

## Files
- `README.md` (file ini) dan `SPEC.md`: aturan rinci per fitur (routine, progress, musik, ikon, offline, peta layar).
- `theme/Color.kt`, `theme/Type.kt`, `theme/Theme.kt`: tema Compose.
- `icon/`: semua aset ikon.
- `design/LiftverseApp.dc.html`: semua layar (prop `screen`, `variant`, `theme` light/dark, `palette` volt).
- `design/Liftverse 1 Fondasi` … `9 Ikon dan Splash`: lembar referensi (fondasi, baris set, layar kunci, layar lain, kondisi khusus, prototype, token, analisis warna, ikon).
- `design/support.js`, `design/assets/`: dibutuhkan agar file HTML terbuka.
- `screenshots/light/`, `screenshots/dark/`: gambar tiap layar.

Data contoh di desain (Raka Pratama, @rakalifts, Push Day Senin & Kamis 18.30, bench 4 set, PR 100 kg × 5) hanya untuk ilustrasi.
