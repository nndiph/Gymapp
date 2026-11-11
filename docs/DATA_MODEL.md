# DATA_MODEL — Liftverse

Versi: 0.1 (draf) · Terakhir diperbarui: 2026-11-10

Basis data: **Cloud Firestore** (NoSQL, dokumen/koleksi). Model dirancang untuk: (a) sedikit read, (b) offline-first, (c) privasi per user. Semua skema memiliki field `schemaVersion` agar migrasi mudah.

## 1. Konvensi

- **Berat disimpan selalu dalam kg.** Konversi ke lb hanya di UI (pengaturan user, fase berikutnya).
- **Waktu:** `Timestamp` UTC untuk urutan; plus `dayKey` (`"yyyy-MM-dd"`) berdasarkan **zona waktu lokal user saat mencatat**, dipakai untuk heatmap/streak supaya tidak bergeser karena zona waktu.
- **ID gerakan (`exerciseKey`):** `seed:<slug>` untuk gerakan bawaan, `custom:<docId>` untuk gerakan custom.
- **Hapus = soft delete** untuk gerakan custom (`archived: true`) agar riwayat lama tetap valid.
- Riwayat menyimpan **snapshot nama gerakan** (`nameSnapshot`), sehingga tetap terbaca meski gerakan diubah/diarsipkan.

## 2. Gerakan: seed + custom

### 2.1 Seed (bawaan) — TIDAK di Firestore
File `app/src/main/assets/seed/exercises.json`, dimuat ke memori saat app start.

```json
{
  "version": 1,
  "exercises": [
    {
      "id": "barbell-bench-press",
      "name": "Barbell Bench Press",
      "primaryMuscle": "chest",
      "secondaryMuscles": ["triceps", "shoulders"],
      "equipment": "barbell",
      "trackingType": "WEIGHT_REPS",
      "aliases": ["bench press", "flat bench"]
    }
  ]
}
```

- Sumber kandidat: `free-exercise-db` dan `wger`. **Verifikasi lisensi di sumber resmi sebelum mengimpor**, simpan atribusi yang diwajibkan di `NOTICE`/layar "Tentang". Buat skrip konversi (`tools/seed/`) yang bisa dijalankan ulang, jangan edit JSON hasil secara manual.
- Terjemahan nama ke Bahasa Indonesia: opsional, via field `names: { "id": "...", "en": "..." }` di versi berikutnya.
- Tambahkan alias lokal (mis. nama yang dipakai di gym Indonesia) agar pencarian lebih bermanfaat.

### 2.2 Custom — `users/{uid}/customExercises/{exerciseId}`

```json
{
  "name": "Incline DB Press (grip netral)",
  "primaryMuscle": "chest",
  "secondaryMuscles": ["shoulders"],
  "equipment": "dumbbell",
  "trackingType": "WEIGHT_REPS",
  "basedOn": "seed:incline-dumbbell-press",
  "notes": "Siku 45°, pause 1 detik",
  "archived": false,
  "createdAt": "<Timestamp>",
  "updatedAt": "<Timestamp>",
  "schemaVersion": 1
}
```

- `trackingType` ∈ `WEIGHT_REPS`, `BODYWEIGHT_REPS`, `DURATION`, `DISTANCE_DURATION`. Tipe **tidak boleh diubah** setelah ada riwayat yang memakainya (ubah = buat gerakan baru).
- `basedOn` opsional: menautkan variasi ke gerakan induk (berguna untuk progress gabungan nanti).
- MVP: custom bersifat **privat**. Fase berikutnya: opsi "usulkan ke library umum" dengan moderasi/duplikat-check.

## 3. Pengguna & username

### `users/{uid}`
```json
{
  "username": "user1",
  "displayName": "User One",
  "bio": "",
  "isPrivate": false,
  "createdAt": "<Timestamp>",
  "schemaVersion": 1
}
```

### `usernames/{usernameLower}` (menjamin keunikan)
```json
{ "uid": "<uid>" }
```
Pembuatan username dilakukan dalam **transaction**: gagal jika dokumen `usernames/{x}` sudah ada. Username: huruf kecil, angka, `_`, panjang 3–20.

## 4. Sesi latihan — `users/{uid}/sessions/{sessionId}`

Satu dokumen berisi seluruh gerakan dan set (ukuran jauh di bawah batas 1 MiB per dokumen).

```json
{
  "startedAt": "<Timestamp>",
  "endedAt": "<Timestamp>",
  "dayKey": "2026-11-10",
  "name": "Push Day",
  "routineId": "r_abc123",
  "note": "",
  "durationSec": 3600,
  "exercises": [
    {
      "exerciseKey": "seed:barbell-bench-press",
      "nameSnapshot": "Barbell Bench Press",
      "trackingType": "WEIGHT_REPS",
      "sets": [
        { "weightKg": 60, "reps": 10, "isWarmup": true },
        { "weightKg": 80, "reps": 8, "rpe": 8.5, "isWarmup": false }
      ]
    }
  ],
  "totals": { "setsCount": 12, "volumeKg": 5400 },
  "schemaVersion": 1
}
```

- `totals` dihitung di klien (volume = Σ berat × reps, tanpa set warm-up).
- Set untuk `DURATION` memakai `durationSec`; `DISTANCE_DURATION` memakai `distanceM` + `durationSec`; `BODYWEIGHT_REPS` memakai `reps` (+ `addedWeightKg` opsional).

## 5. Routine — `users/{uid}/routines/{routineId}`

```json
{
  "name": "Push Day",
  "description": "",
  "weekdays": [1, 4],
  "reminderEnabled": true,
  "reminderTime": "18:30",
  "items": [
    { "exerciseKey": "seed:barbell-bench-press", "nameSnapshot": "Barbell Bench Press", "targetSets": 4, "targetReps": "6-8" }
  ],
  "archived": false,
  "updatedAt": "<Timestamp>",
  "schemaVersion": 1
}
```
`weekdays`: ISO (1 = Senin … 7 = Minggu). `reminderTime`: `"HH:mm"` waktu lokal perangkat.

## 6. Agregasi (tanpa Cloud Functions)

### `users/{uid}/monthlyStats/{yyyy-MM}`
```json
{
  "days": {
    "2026-11-10": { "sessions": 1, "sets": 12, "volumeKg": 5400 }
  },
  "sessionsCount": 8,
  "volumeKg": 41200,
  "updatedAt": "<Timestamp>",
  "schemaVersion": 1
}
```
Heatmap 12 bulan = membaca 12 dokumen ini.

**Strategi pembaruan: "recompute-day" (idempoten).** Setiap kali sesi dibuat/diubah/dihapus: (1) query semua sesi dengan `dayKey` yang sama (dari cache lokal), (2) hitung ulang entri hari itu, (3) tulis `days.<dayKey>` dan total bulan dalam satu `WriteBatch` bersama penulisan sesi. Cara ini otomatis memperbaiki diri jika pernah ada ketidaksinkronan. Sediakan fungsi "hitung ulang bulan" untuk perbaikan.

### `users/{uid}/exerciseStats/{exerciseKeySafe}`
```json
{
  "exerciseKey": "seed:barbell-bench-press",
  "prWeightKg": 100,
  "prE1rmKg": 118.5,
  "lastPerformedDayKey": "2026-11-10",
  "recent": [
    { "dayKey": "2026-11-03", "bestWeightKg": 95, "bestReps": 5, "e1rmKg": 110.8 }
  ],
  "schemaVersion": 1
}
```
- `exerciseKeySafe`: ganti `:` dengan `_` (mis. `seed_barbell-bench-press`).
- e1RM (estimasi 1RM) memakai rumus Epley: `berat × (1 + reps/30)`. Tandai di UI sebagai estimasi.
- `recent` dibatasi N titik terakhir (mis. 30). Riwayat penuh tetap bisa diturunkan dari `sessions`.

## 7. Sosial

- `users/{uid}/following/{targetUid}` → `{ "createdAt": ... }`
- `users/{uid}/followers/{followerUid}` → `{ "createdAt": ... }`

Follow = satu `WriteBatch` menulis kedua dokumen. Jumlah follower/following memakai `count()` aggregation query (tanpa counter manual). MVP: akun privat tidak bisa difollow/dilihat detailnya; permintaan follow ke akun privat ditunda ke fase berikutnya.

## 8. Draf Security Rules (`firestore.rules`)

> **Draf, wajib diuji dengan Firebase Emulator** sebelum deploy. Prinsip: default tolak; pemilik boleh menulis datanya; orang lain hanya membaca jika profil publik.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() { return request.auth != null; }
    function isOwner(uid) { return signedIn() && request.auth.uid == uid; }
    function userDoc(uid) { return get(/databases/$(database)/documents/users/$(uid)); }
    function isPublic(uid) { return userDoc(uid).data.isPrivate == false; }

    match /users/{uid} {
      allow read: if signedIn();                 // username, nama, bio terlihat oleh user yang login
      allow create, update, delete: if isOwner(uid);

      match /customExercises/{id} { allow read, write: if isOwner(uid); }
      match /routines/{id}        { allow read: if isOwner(uid) || isPublic(uid);
                                    allow write: if isOwner(uid); }
      match /sessions/{id}        { allow read: if isOwner(uid) || isPublic(uid);
                                    allow write: if isOwner(uid); }
      match /monthlyStats/{id}    { allow read: if isOwner(uid) || isPublic(uid);
                                    allow write: if isOwner(uid); }
      match /exerciseStats/{id}   { allow read: if isOwner(uid) || isPublic(uid);
                                    allow write: if isOwner(uid); }

      match /following/{targetUid} {
        allow read: if signedIn();
        allow write: if isOwner(uid);
      }
      match /followers/{followerUid} {
        allow read: if signedIn();
        allow create: if signedIn() && request.auth.uid == followerUid && isPublic(uid);
        allow delete: if signedIn() && request.auth.uid == followerUid;
      }
    }

    match /usernames/{name} {
      allow read: if signedIn();
      allow create: if signedIn() && request.resource.data.uid == request.auth.uid;
      allow delete: if signedIn() && resource.data.uid == request.auth.uid;
      allow update: if false;
    }
  }
}
```

Hal yang harus ditambahkan saat implementasi: validasi bentuk data (tipe field, rentang nilai, batas ukuran array), batasi `isPrivate` hanya boleh bernilai boolean, cegah pengguna mengubah `username` tanpa transaksi yang benar, dan uji kasus penyalahgunaan (baca data user privat, tulis ke data orang lain).

## 9. Indeks

Query MVP hanya memakai field tunggal (urut `startedAt desc`, filter `dayKey`), jadi indeks single-field otomatis cukup. Jika kelak butuh query gabungan, tambahkan ke `firestore.indexes.json` dan commit.

## 10. Rencana migrasi

Setiap dokumen punya `schemaVersion`. Klien harus toleran terhadap field baru/hilang (nilai default di mapper). Perubahan breaking dikerjakan sebagai: tambah field baru → klien baru membaca keduanya → migrasi data → hapus field lama pada rilis berikutnya.
