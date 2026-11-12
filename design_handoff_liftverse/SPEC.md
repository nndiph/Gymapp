# Liftverse · handoff desain untuk Jetpack Compose

Sumber desain: `LiftverseApp.dc.html` (semua layar, satu komponen). Lembar: 1 Fondasi, 2 Baris set, 3 Layar kunci, 4 Layar lain, 5 Kondisi khusus, 6 Prototype, 7 Handoff.

File tema siap salin ke `app/src/main/java/app/repset/ui/theme/`:
- `Color.kt`: `LightColors`, `DarkColors` (peran Material 3), `LiftverseExtendedColors` (primaryFill, brass, success, heatmap), `heatmapLevel()`.
- `Type.kt`: `LiftverseTypography` + `LiftverseNumberStyles` (Archivo SemiExpanded untuk angka, Plus Jakarta Sans untuk UI, `tnum` di semua gaya).
- `Theme.kt`: `LiftverseShapes`, `LiftverseSpacing`, `LiftverseTheme { }` tanpa dynamic color.

## Palet grafit + volt (berlaku, menggantikan kobalt)

- Sumber kebenaran warna: `handoff/Color.kt`. Palet kobalt lama sudah tidak dipakai.
- primary: #536B00 (light, teks/ikon/aktif) · #CDEB3B (dark). primaryFill: #1A1F14 + label #CDEB3B (light) · #CDEB3B + label #1C2400 (dark). primaryContainer: #ECF5C4 / #2A3510.
- Heatmap light #E6EAE7 → #CCE277 → #A2C22E → #6E8C10 → #3F5206; dark #1A1F1E → #344516 → #557220 → #93B92C → #D4F04A.
- Kuningan tetap khusus PR.

## Aturan penting

- Netral memakai grafit (chroma hampir nol, sedikit ke hijau): latar #F5F7F6 / #0B0D0D, teks #111614 / #EDF1EF. Netral kebiruan lama dihapus karena bentrok dengan volt.

- Dynamic color mati. Selalu `LiftverseTheme`.
- `surfaceTint = Transparent`. Kedalaman dari tingkatan surface + garis 1 dp `outlineVariant`, bukan bayangan. `tonalElevation = 0.dp` pada Card, NavigationBar, BottomSheet.
- Tombol utama: `LiftverseTheme.colors.primaryFill` + `onPrimaryFill`. Light: grafit #1A1F14 dengan label volt #CDEB3B. Dark: volt #CDEB3B dengan label #1C2400. Teks/ikon/state aktif memakai `colorScheme.primary`: zaitun #536B00 (light) atau volt (dark).
- Volt (`LiftverseTheme.colors.accent`, #CDEB3B) adalah warna identitas dan "sekarang": indikator NavigationBar aktif (indicatorColor = accent, selectedIconColor = onAccent), kartu streak + minggu di Dashboard (isi accent; teks #1C2400/#3E4C12; tanpa biru: lingkaran hari selesai #1C2400 dengan centang volt, hari ini cincin #1C2400), panel set aktif (accentContainer), timer istirahat, hero Selesai sesi, chip "Hari ini", dan juga: tanda wordmark (bilah miring 9×22 dp, skew −14°), angka dan ikon streak (`accentText`), titik 8 dp + timer sesi berjalan, dan badge sesi aktif di tab Catat. Di light mode volt di atas putih hanya 1,35:1: pakai hanya sebagai isi dengan `onAccent` #1C2400, dan sebagai teks pakai `accentText` #536B00. Volt tidak pernah dipakai untuk tombol.
- Kuningan (`brass`) hanya badge PR. Teks kuningan memakai `tertiary`.
- Biru hanya untuk aksi utama, state aktif, dan data. Tombol sekunder memakai `secondaryContainer` atau outlined netral.
- Teks UI Bahasa Indonesia, sentence case. Format angka `id-ID`: 41.200, 116,7, 2,5.
- Target sentuh minimal 48 dp. Aksi utama di separuh bawah layar.

## Komponen

| Komponen | Spesifikasi |
|---|---|
| Button filled | tinggi 56 (utama) / 48, radius medium 12, `primaryFill`, label `labelLarge` 16 / `titleSmall` 14 |
| Button tonal | 48, `secondaryContainer` / `onSecondaryContainer` |
| Button outlined | 48, garis 1 dp `outline`, teks `onSurface` |
| Text button | 48, teks `primary` |
| Text field | `OutlinedTextField`, 56, radius 12, garis `outline` (fokus 2 dp `primary`, error 2 dp `error`) |
| Filter chip | 32 visual / 48 sentuh, radius 8, terpilih `primaryContainer` + ikon check, tidak terpilih garis `outline` |
| Tag (Custom, PR) | 22, radius 6. Custom: garis `outline` + ikon edit. PR: isi `brass`, teks `onBrass`, ikon trophy |
| Navigation bar | 80, `surfaceContainer`, indikator 64×32 `accent` (volt), ikon aktif `onAccent` FILL 1, tepat 3 tab |
| Badge sesi aktif | titik 8 dp `primary` di ikon Catat, contentDescription "Catat, sesi sedang berjalan" |
| Card | radius 16, `surface`, garis 1 dp `outlineVariant`, padding 16, tanpa bayangan |
| Bottom sheet | `ModalBottomSheet`, radius atas 28, `surfaceContainerLow`, drag handle 32×4 `outline` |
| Dialog | radius 28, `surfaceContainerHigh` |
| Indikator sinkron | chip 28 tinggi, radius penuh, `surfaceContainer`, ikon 16 + teks 12: cloud_done "Tersinkron" / smartphone "Tersimpan di perangkat" / sync "Menyinkronkan" |
| Sel heatmap | Satu baris 6 bulan (27 minggu × 7 hari), panah 48 dp untuk 6 bulan sebelumnya/berikutnya, sel ≈11 dp, celah 2, radius 2. Hari ini: cincin 2 dp `primary` (putus-putus bila kosong). Terpilih: cincin 2 dp `onSurface`. Seluruh grid bisa diketuk, ringkasan di bawahnya + tombol sebelumnya/berikutnya 48 dp |
| Delta | ikon 14 (arrow_upward / arrow_downward / remove) + teks. Naik: `success`. Turun dan sama: `onSurfaceVariant` |
| Sparkline | 56×24 (dashboard) / 64×28 (daftar), garis 2 dp `primary`, titik akhir r 3. 6 sesi terakhir |
| Batang pembanding | periode ini `primaryFill`; periode lalu `surfaceContainerHighest` + garis 1 dp `outline`; terjadwal putus-putus 1,5 dp `primary` |
| Ikon | Material Symbols Rounded, 24 dp, wght 400, FILL 0 default / FILL 1 aktif |

## Baris set (alternatif B)

- Satu set aktif per sesi, otomatis = set pertama yang belum selesai. Panel `surfaceContainer`, radius 16, padding 14/12.
- Stepper: tinggi 76, radius 12, `surface`, tombol −/+ 46×56, nilai `numbers.stepper` 28. Langkah per gerakan: barbell 2,5 kg, dumbbell 2 kg, kabel 2,5 kg, reps 1, durasi 15 dtk, jarak 0,1 km, durasi kardio 30 dtk.
- Ketuk angka → numpad sheet (tombol ±langkah, koma, hapus, Simpan).
- Selesai set 56 tinggi, Warm-up toggle 56 di kirinya, tombol hapus set 56×56 (ikon delete, warna error) paling kiri.
- Hapus set: buka set (ketuk barisnya) lalu tombol hapus. Hapus gerakan: ikon delete di kepala gerakan. Keduanya langsung terhapus tanpa dialog, dengan snackbar "Urungkan" selama 5 detik.
- Aturan menu: menu ⋮ hanya dipakai bila berisi minimal 3 aksi. Kurang dari itu, tampilkan aksinya langsung sebagai ikon (contoh: Laporkan dan Blokir di profil orang lain).
- Baris ringkas 56 tinggi: badge nomor 32 (W putus-putus untuk warm-up), nilai `numbers.setRow`, check 32 dalam area 52.
- Mengubah beban set aktif ikut mengubah set berikutnya yang belum selesai dan nilainya sama.
- Isian awal = nilai set yang sama dari sesi terakhir gerakan itu. Gerakan baru: 20 kg × 10.
- Ketukan: konfirmasi 1, ubah satu nilai 2, ubah beban + reps 3.

Tipe pencatatan: `WEIGHT_REPS` (kg + reps), `BODYWEIGHT_REPS` (reps + beban tambahan opsional, 0 = BW), `DURATION` (m:ss), `DISTANCE_DURATION` (km + m:ss).

## Timer istirahat

- Mulai otomatis saat set dicentang (bukan saat set dibatalkan). Durasi default: 2:30 untuk gerakan compound barbell (bench, squat, deadlift, OHP, row), 1:30 untuk lainnya, 1:00 untuk durasi/kardio. Bisa ditimpa per gerakan di routine (rencana berikutnya).
- Panel volt di atas tombol Tambah gerakan / Selesai: label, sisa waktu besar, −15, +15, Lewati, bilah progres linear 5 dp (bukan cincin). Isi `accent`, teks `onAccent` di kedua mode.
- Saat habis: label "Waktunya set berikutnya", waktu berubah jadi lebihan (+0:12), getar 2× dan notifikasi bila aplikasi di latar belakang (foreground service atau AlarmManager exact alarm dengan izin SCHEDULE_EXACT_ALARM bila tersedia).
- Durasi sesi tetap berjalan selama istirahat. Waktu istirahat dicatat per set: dari set dicentang sampai pengguna menekan Lewati, atau sampai set berikutnya dicentang dikurangi estimasi durasi set (reps × 4 detik). Istirahat setelah set terakhir tidak dihitung.
- Latihan aktif = durasi total − total istirahat. Ditampilkan di header sesi ("aktif 16:12 · istirahat 8:20"), di Selesai sesi (bilah biru/volt + rata-rata istirahat per set), dan di detail riwayat (ringkasan + istirahat per set dengan ikon timer).
- Data model: `SetLog.restAfterSec: Int?`; `Session.durationSec`, turunan `restSec = sum(restAfterSec)`, `activeSec = durationSec − restSec`.
- Timer berbasis waktu mulai (timestamp), bukan hitung mundur di memori, jadi tetap benar setelah aplikasi ditutup atau HP dikunci.

## Routine

- Tidak ada layar Kelola routine terpisah. Tab Catat (tanpa sesi aktif) menampilkan daftar routine: ketuk baris untuk form ubah, tombol putar untuk mulai, baris "Buat routine" di akhir daftar. Hapus routine ada di bawah form ubah.
- Sesi menyimpan `routineId` dan struktur awal (gerakan + jumlah set). Saat Selesaikan sesi, bila struktur berbeda, tampilkan checkbox "Perbarui routine X" dengan ringkasan perubahan (default tidak dicentang). Sesi kosong dengan minimal satu gerakan menawarkan "Simpan sebagai routine".

## Progress

- Semua perbandingan memakai hari yang sama: minggu ini (Senin s.d. hari ini) vs hari yang sama minggu lalu; bulan ini (tanggal 1 s.d. hari ini) vs tanggal yang sama bulan lalu.
- Dashboard: tiap gerakan di Latihan hari ini menampilkan target, set sesi lalu, sparkline volume 6 sesi, dan delta volume vs bulan lalu. Kartu "Minggu ini vs minggu lalu" berisi volume, set, batang harian berpasangan, dan baris bulan ini.
- Layar Progress: Minggu/Bulan, 4 statistik (sesi, volume, set, menit) dengan delta, grafik, set per otot utama (batang + penanda periode lalu), dan daftar gerakan diurutkan dari kenaikan terbesar.
- Progress per gerakan default ke volume per sesi; estimasi 1RM dan beban terberat sebagai pilihan.

## Musik latihan

- Profil menampilkan maksimal 3 playlist atau lagu dari Spotify, Apple Music, YouTube Music, Deezer, atau SoundCloud. Berbeda dari Routine (yang juga bergaya playlist): musik memakai cover asli platform + tag nama platform.
- Masuk lewat link, tanpa OAuth:
  1. Tempel link di layar Tambah musik (tawarkan isi clipboard bila berupa link musik), atau
  2. Bagikan dari aplikasi musik: intent-filter `ACTION_SEND` `text/plain` → buka layar Tambah musik dengan link terisi.
- Validasi domain: open.spotify.com, spotify.link, music.apple.com, music.youtube.com, deezer.com, deezer.page.link, soundcloud.com. Link pendek di-resolve dulu di server.
- Metadata dibaca di server (hindari CORS dan rate limit di HP), disimpan bersama profil, disegarkan berkala:
  - Spotify: `https://open.spotify.com/oembed?url=…` (judul, thumbnail). Jumlah lagu dari tag Open Graph `og:description` bila ada.
  - YouTube Music: ubah ke URL youtube.com lalu `https://www.youtube.com/oembed?url=…`.
  - SoundCloud: `https://soundcloud.com/oembed?url=…&format=json`.
  - Apple Music, Deezer: tag Open Graph (`og:title`, `og:image`, `og:description`) dari halaman publik.
- Playlist privat atau link tidak dikenal → error di field dengan cara memperbaikinya.
- Ketuk di profil orang lain membuka URL asli (App Links meneruskan ke aplikasi musik). Di profil sendiri, ⋮ berisi Buka, Ganti link, Hapus dari profil.
- Tidak ada pemutaran di Liftverse dan tidak ada OAuth Spotify: sejak 15 Mei 2025 extended quota Spotify Web API hanya untuk organisasi dengan ≥ 250 ribu MAU, dan development mode dibatasi 5 pengguna.
- Profil privat menyembunyikan musik bersama data lainnya.

## Ikon dan tanda

- Tanda: tiga bilah naik di dalam orbit (desain dari pengguna). Master raster: `assets/liftverse-mark.png` (hitam + alpha, rasio 1,65 : 1). SVG hasil trace: `handoff/icon/liftverse-mark.svg` (draft, akan direvisi).
- Launcher (adaptive icon): latar `#CDEB3B`, foreground `handoff/icon/ic_launcher_foreground.png` (tanda #1C2400, 432 px = 108 dp @xxxhdpi, lebar tanda 61 dp di dalam zona aman 66 dp), monochrome `ic_launcher_monochrome.png`. Alternatif gelap: latar `#131716` + `ic_launcher_foreground_alt_gelap.png`.
- Play Store: `playstore_512.png`.
- Splash (SplashScreen API): light `splash_icon_light.png` + iconBackground `#CDEB3B` di latar `#F5F7F6`; dark `splash_icon_dark.png` + iconBackground `#131716` di latar `#0B0D0D`.
- Notifikasi: `ic_stat_liftverse.svg` (versi kecil 24 dp: tiga pilar tanpa orbit) dan `ic_stat_notification_96.png`. Draft, akan direvisi desainer.
- Di aplikasi: tanda di depan wordmark berwarna `accentText` (#536B00 light, #CDEB3B dark), tinggi 22 dp di top bar Dashboard, 42 dp di layar Masuk.

## Pencapaian langsung di set aktif

- Dihitung ulang setiap kali stepper atau numpad mengubah nilai set aktif, sebelum Selesai set.
- Naik dari sesi lalu (chip volt di judul set aktif, "↑ +2,5 kg"): beban lebih berat dengan reps tidak turun lebih dari 1, atau reps lebih banyak di beban yang sama. Durasi lebih lama, jarak lebih jauh. Chip kecil di header panel: "↑ +2,5 kg".
- Calon PR (chip kuningan + trophy, "Calon PR"): estimasi 1RM Epley di atas rekor gerakan itu. Menggantikan chip naik. Tidak ada panel teks; contentDescription chip memuat detailnya ("Calon PR, estimasi 1RM 42 kg, rekor 35 kg").
- Warm-up tidak dihitung. Gerakan tanpa sesi lalu tidak menampilkan "naik".
- Setelah Selesai set: baris ringkas memakai tag volt "↑ +2,5 kg", atau tag PR bila rekor. Umumkan lewat `LiveRegion.Polite` dan getar pendek (`HapticFeedbackType.LongPress`) saat status pertama kali muncul.

## Rumus

- Volume set = kg × reps (bodyweight: hanya beban tambahan). Warm-up ikut volume, tidak ikut PR.
- Estimasi 1RM (Epley) = beban × (1 + reps / 30). 100 × (1 + 5/30) = 116,7 kg. Selalu berlabel "estimasi".
- PR = set kerja dengan estimasi 1RM di atas rekor sebelumnya.
- Streak mengikuti jadwal pilihan pengguna (`StreakSchedule`, disimpan lokal dan ikut sinkron):
  - `FIXED_DAYS(days: Set<DayOfWeek>, allowSwap: Boolean = true)`: +1 per hari latihan yang selesai. Hari libur dilewati, sesi di hari libur tetap +1. Hari latihan yang terlewat (sebelum hari ini) memutus streak, kecuali `allowSwap` dan jumlah sesi minggu itu ≥ jumlah hari latihan. Minggu berjalan dengan `allowSwap` tidak pernah memutus.
  - `WEEKLY_TARGET(target: 1..7)`: +1 per minggu Senin–Minggu dengan sesi ≥ target. Minggu berjalan tidak memutus.
  - Preset split hanya mengisi `days`: Ikuti routine (gabungan hari di semua routine), PPL × Upper/Lower (Sel–Kam, Sab, Min), Upper/Lower (Sen, Sel, Kam, Jum), PPL × 2 (Sen–Sab), Full body (Sen, Rab, Jum), Atur sendiri.
  - Default bila onboarding dilewati: `WEEKLY_TARGET(3)`.
  - Mengganti jadwal menghitung ulang streak dari riwayat. Layar Jadwal menampilkan pratinjaunya sebelum disimpan.
- Tingkat heatmap per hari dari volume: 0 tanpa sesi, 1 &lt; 3.000, 2 &lt; 4.500, 3 &lt; 6.000, 4 ≥ 6.000 kg.

## Peta layar (`screen` di LiftverseApp)

| screen | variant | Layar |
|---|---|---|
| dashboard | –, libur, baru, offline | Dashboard |
| catat | –, baru | Tab Catat: mulai sesi + kelola routine |
| sesi | –, numpad, naik, calon-pr, selesai, selesai-ubah, tipe, padat, offline, kosong | Sesi aktif |
| selesai | –, tanpa-pr | Selesai sesi |
| profil | –, baru, aktif | Profil sendiri |
| masuk | –, daftar | Masuk / daftar |
| username | –, dipakai, format | Buat username |
| jadwal | –, onboarding, pplul, mingguan | Jadwal dan streak |
| pemilih | –, browse, dada, kosong | Pemilih gerakan |
| gerakan-form | –, dari-cari, ubah | Form gerakan custom |
| riwayat | –, kosong, error | Riwayat |
| riwayat-detail | –, ubah, hapus | Detail sesi |
| routine-form | –, baru | Form routine |
| progres | –, bulan | Progress minggu/bulan |
| musik | –, preview, error, share | Tambah musik latihan |
| progress | – | Progress per gerakan |
| cari | –, kosong, offline | Cari pengguna |
| profil-lain | –, diikuti, menu, laporkan, blokir, offline | Profil publik orang lain |
| profil-privat | –, offline | Profil privat |
| koneksi | –, mengikuti, kosong, offline | Pengikut / mengikuti |
| notif | –, sistem, ditolak | Izin notifikasi |
| pengaturan | –, ditolak, baterai, keluar, hapus | Pengaturan |
| tentang | – | Tentang aplikasi |

## Offline

Room sebagai sumber kebenaran. Semua layar inti membaca dari database lokal; sinkron lewat WorkManager. Layar sosial: batas waktu 8 dtk lalu state "Tidak bisa memuat" + Coba lagi, tanpa spinner tanpa batas.
