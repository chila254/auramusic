# AuraMusic v1.0.10 (Build 11) Release Notes

## What's New in This Update

### Video Player Improvements
This release improves the video player experience with better loading states and performance:

1. **Video Switching Loading Indicator**
   - Smooth loading animation while video is being fetched
   - Prevents black screen flash when switching between audio/video modes

2. **Improved Video Lyrics Sync**
   - Reduced polling interval from 150ms to 50ms
   - Lyrics are now perfectly synchronized with video playback

3. **Music Video Search Algorithm Overhaul**
   - Completely rewritten video search with much higher accuracy
   - Normalized title comparison with automatic bracketed content stripping
   - Artist token matching for more reliable artist detection
   - Multi-query search with cross-query result comparison
   - Expanded exclusion list for non-official videos (karaoke, sped up, slowed, nightcore, etc.)
   - Early exit for high-confidence matches
   - Minimum confidence threshold for more reliable results

### General Improvements
- Updater now automatically follows redirects for GitHub API requests
- All repository URLs updated across entire codebase

### Bug Fixes & Improvements
- Fixed black screen flash when switching between audio/video modes
- Fixed lyrics offset not being properly applied in video mode
- Fixed video background during loading state
- Fixed repository URL references throughout the app

---

## 💻 Technical Details

### Full Changelog (Commits since last release):
- [`a6b3f72`](https://github.com/TeamAuraMusic/AuraMusic/commit/a6b3f72) Fix repository URL spelling
- [`251f8ed`](https://github.com/TeamAuraMusic/AuraMusic/commit/251f8ed) Bump version to v1.0.10 (Build 11)
- [`ad097b5`](https://github.com/TeamAuraMusic/AuraMusic/commit/ad097b5) Fix GitHub organization URL in release notes
- [`d193c90`](https://github.com/TeamAuraMusic/AuraMusic/commit/d193c90) Update release notes with clickable commit links
- [`6aa1e3b`](https://github.com/TeamAuraMusic/AuraMusic/commit/6aa1e3b) Updated CHANGELOG.md and Indicate RELEASE_NOTES.md

### Repository Update
AuraMusic is now officially hosted at the Team AuraMusic organization:
**https://github.com/TeamAuraMusic/AuraMusic**

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.9...v1.0.10
