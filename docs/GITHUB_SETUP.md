# GITHUB_SETUP — Repset (working title)

Panduan menyiapkan repo, alur kerja, CI, dan proyek Firebase. Ganti `<user>` dengan nama akun GitHub dan `<repo>` dengan nama repo (mis. `repset`). Claude Code membantu menjalankan; **aksi yang mengubah akun GitHub/Firebase dikonfirmasi dulu ke pemilik proyek.**

## 0. Prasyarat

- `git`, **GitHub CLI** (`gh`; di Windows: `winget install GitHub.cli`), Android Studio (versi stabil terbaru), JDK 17, Node.js (untuk `firebase-tools`)
- Login: `gh auth login`
- Firebase CLI: `npm i -g firebase-tools` lalu `firebase login`

## 1. Keputusan awal

| Hal | Usulan |
|---|---|
| Visibilitas | **Public** (portofolio; juga membuat branch protection/ruleset gratis) |
| Nama repo | working title `repset` — final setelah nama app diputuskan |
| Lisensi | MIT (kode). Dataset gerakan seed punya lisensi sendiri → lihat `DATA_MODEL.md` |
| Alur kerja | Trunk-based: `main` selalu hijau, branch pendek `feat/*`, `fix/*`, `docs/*`, `chore/*` |

## 2. Struktur awal repo

```
<repo>/
├── CLAUDE.md
├── README.md
├── LICENSE
├── .gitignore
├── docs/
│   ├── PRD.md
│   ├── ARCHITECTURE.md
│   ├── DATA_MODEL.md
│   ├── ROADMAP.md
│   └── GITHUB_SETUP.md
└── .github/
    ├── workflows/ci.yml
    ├── ISSUE_TEMPLATE/{bug_report.md, feature_request.md}
    ├── pull_request_template.md
    └── dependabot.yml
```
Kode Android (`app/`, `gradle/`, dst.) ditambahkan di M0.

## 3. `.gitignore` — pastikan memuat

Gunakan template Android standar (GitHub: `gh repo create` bisa memberi template `Android`), lalu pastikan baris berikut ada:

```
# secrets & konfigurasi lokal
app/google-services.json
keystore.properties
*.jks
*.keystore
local.properties
*service-account*.json
.env

# Firebase emulator
firebase-debug.log
.firebase/
```
Commit `app/google-services.json.example` (struktur sama, nilai palsu) sebagai pengganti.

## 4. Buat repo & push pertama

```bash
mkdir <repo> && cd <repo>
git init -b main
# letakkan CLAUDE.md, README.md di root; PRD/ARCHITECTURE/DATA_MODEL/ROADMAP/GITHUB_SETUP di docs/
git add . && git commit -m "docs: add project planning documents"

gh repo create <user>/<repo> --public \
  --description "Android gym tracker with a social profile layer" \
  --source . --remote origin --push
```

## 5. Label

```bash
gh label create "type: feature"  --color 0E8A16 --force
gh label create "type: bug"      --color D73A4A --force
gh label create "type: chore"    --color C5DEF5 --force
gh label create "type: docs"     --color 0075CA --force
gh label create "area: logger"   --color FBCA04 --force
gh label create "area: profile"  --color FBCA04 --force
gh label create "area: dashboard" --color FBCA04 --force
gh label create "area: exercise" --color FBCA04 --force
gh label create "area: backend"  --color FBCA04 --force
gh label create "size: S" --color EDEDED --force
gh label create "size: M" --color D4C5F9 --force
gh label create "size: L" --color 5319E7 --force
gh label create "blocked" --color B60205 --force
```

## 6. Milestone & issue

```bash
for m in "M0 Fondasi" "M1 Auth & profil dasar" "M2 Library gerakan" \
         "M3 Catat latihan & riwayat" "M4 Profil & progress" \
         "M5 Dashboard & notifikasi" "M6 Sosial minimal" "M7 Hardening & rilis"; do
  gh api repos/{owner}/{repo}/milestones -f title="$m" >/dev/null
done

# contoh membuat satu issue dari ROADMAP
gh issue create --title "Setup Android project (Compose, Hilt, Navigation)" \
  --body "Lihat docs/ROADMAP.md → M0." \
  --label "type: chore" --label "size: M" --milestone "M0 Fondasi"
```
Claude Code dapat membuat seluruh issue dari butir-butir `docs/ROADMAP.md` (satu butir = satu issue) setelah pemilik proyek setuju.

## 7. Project board

```bash
gh auth refresh -s project
gh project create --owner "@me" --title "<repo> roadmap"
```
Kolom: **Backlog · Ready · In progress · In review · Done**. Hubungkan repo & tambahkan issue ke board lewat UI (Projects → Add item).

## 8. Aturan branch (Ruleset)

Lewat UI: *Settings → Rules → Rulesets → New branch ruleset* untuk `main`:

- [x] Require a pull request before merging (boleh 0 approval karena proyek solo)
- [x] Require status checks to pass → pilih job `build` (muncul setelah CI berjalan sekali)
- [x] Block force pushes
- [x] Restrict deletions

## 9. CI — `.github/workflows/ci.yml`

Sudah dibuat di M0 (versi action dicek 2026-11-10: checkout v7, setup-java v6, gradle/actions v6). Dependabot akan mengusulkan pembaruan mingguan.

```yaml
name: CI
on:
  pull_request:
  push:
    branches: [main]

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v6
      - name: Use placeholder google-services.json
        run: cp app/google-services.json.example app/google-services.json
      - name: Lint, test, build
        run: ./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug
```

Tahap berikutnya (M7): job terpisah untuk uji security rules (Emulator) dan build rilis memakai GitHub Secrets (`GOOGLE_SERVICES_JSON` dalam base64, kredensial keystore). **Jangan** menaruh keystore di repo.

## 10. Template

**`.github/pull_request_template.md`**
```markdown
## Ringkasan
<!-- Apa yang berubah dan kenapa -->

## Issue terkait
Closes #

## Cara menguji
<!-- Langkah singkat; sebutkan jika diuji dalam mode pesawat -->

## Checklist
- [ ] CI hijau
- [ ] Fitur inti tetap jalan offline
- [ ] Tidak ada secret/log sensitif ter-commit
- [ ] Dokumen diperbarui jika perlu
```

**`.github/ISSUE_TEMPLATE/feature_request.md`**
```markdown
---
name: Feature request
about: Usulan fitur / tugas
labels: "type: feature"
---
## Tujuan
## Acceptance criteria
- [ ]
## Catatan teknis
```

**`.github/ISSUE_TEMPLATE/bug_report.md`**
```markdown
---
name: Bug report
about: Laporan bug
labels: "type: bug"
---
## Yang terjadi
## Yang diharapkan
## Langkah mereproduksi
## Perangkat / versi Android / versi app
```

**`.github/dependabot.yml`**
```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: "/"
    schedule: { interval: weekly }
  - package-ecosystem: github-actions
    directory: "/"
    schedule: { interval: weekly }
```

## 11. Proyek Firebase (konsol)

1. Buat proyek di Firebase Console. Matikan Google Analytics jika tidak dibutuhkan.
2. Tambah **app Android** dengan `applicationId` final (⚠️ jangan salah; tidak bisa diganti setelah rilis). Tambahkan SHA-1 debug (`./gradlew signingReport`) agar Google Sign-In berfungsi.
3. Unduh `google-services.json` → taruh di `app/` (tidak di-commit).
4. **Authentication:** aktifkan Google dan Email/Password.
5. **Firestore:** buat database dalam **mode produksi**, pilih **region Asia** (permanen). Deploy `firestore.rules` lewat CLI (`firebase deploy --only firestore:rules`) setelah diuji di emulator.
6. **Crashlytics:** aktifkan.
7. Tetap di paket **Spark** selama pengembangan. Saat menjelang rilis (M7): pindah ke **Blaze** dan **pasang budget alert** di Google Cloud Billing.
8. Tambahkan SHA-1/SHA-256 *release* dan sertifikat **Play App Signing** setelah app dibuat di Play Console.

## 12. Cheat sheet Conventional Commits

`feat:` fitur baru · `fix:` perbaikan bug · `docs:` dokumentasi · `refactor:` ubah struktur tanpa ubah perilaku · `test:` tes · `chore:` tugas rutin/build · `ci:` konfigurasi CI

Contoh: `feat(logger): prefill set from previous session`

## 13. Checklist "siap mulai coding"

- [ ] Repo ada di GitHub, `main` dilindungi
- [ ] Label, milestone, board terpasang
- [ ] Issue M0 dibuat
- [ ] Proyek Firebase siap (Auth, Firestore, Crashlytics)
- [ ] Nama working title & `applicationId` disepakati
- [ ] Sesi Claude Code pertama: baca `CLAUDE.md` → kerjakan M0
