# GitHub Workflow - Automated Release Building

## Overview

The GitHub Actions workflow automatically builds and creates releases for the Timekeeper project. It now includes two jobs:

1. **build** (Ubuntu) - Builds the CLI tool and creates the release
2. **build-macos-desktop** (macOS) - Builds the desktop DMG and uploads it to the release

## Workflow File

Location: `.github/workflows/gradle.yml`

## Jobs

### Job 1: `build` (Ubuntu)

**Runs on**: `ubuntu-latest`

**Tasks**:
1. Checkout code
2. Set up JDK 21 (GraalVM)
3. Setup Gradle
4. Build with Gradle
5. Generate shadow JAR
6. Generate native binary
7. Create timestamp tag (e.g., `release-202512121045`)
8. Create GitHub Release with CLI artifacts:
   - `app/build/libs/timekeeper.jar`
   - `app/build/native/nativeCompile/timekeeper`

**Outputs**:
- `tag` - The release tag created (passed to the next job)

### Job 2: `build-macos-desktop` (macOS)

**Runs on**: `macos-latest`

**Triggers**: After the `build` job completes successfully

**Tasks**:
1. Checkout code at the release tag
2. Set up JDK 21 (GraalVM)
3. Setup Gradle
4. Build and install CLI binary to `/usr/local/bin/`
   - Needed by the desktop app
5. Build desktop DMG via `./gradlew :desktop:packageDmg`
6. Upload DMG to the same release using `gh release upload`

## Release Artifacts

Each release now includes:

```
Release: release-YYYYMMDD_HHMM
├── timekeeper.jar              (CLI JAR)
├── timekeeper                  (Native CLI binary)
└── Timekeeper-1.0.0.dmg        (macOS desktop app)
```

## Workflow Execution

### Trigger

The workflow runs automatically on every push to the `main` branch:

```yaml
on:
  push:
    branches: [ "main" ]
```

### Sequence

1. **Commit to main** → GitHub Actions triggered
2. **Build job runs** (Ubuntu)
   - Builds CLI
   - Creates release
   - Outputs tag
3. **macOS desktop job runs** (after build job)
   - Waits for build job to complete
   - Checks out at the release tag
   - Builds desktop DMG
   - Uploads to the existing release

### Execution Time

- **Build job**: ~5-10 minutes (CLI compilation + GraalVM native image)
- **macOS job**: ~10-15 minutes (Gradle setup + Compose packaging)
- **Total**: ~20-25 minutes for complete release

## Key Features

### ✅ Job Dependencies

```yaml
build-macos-desktop:
  needs: build
```

The macOS job waits for the build job to complete before starting.

### ✅ Tag Passing

The build job outputs the tag:

```yaml
outputs:
  tag: ${{ steps.create_tag.outputs.tag }}
```

The macOS job references it:

```yaml
with:
  ref: ${{ needs.build.outputs.tag }}
run: |
  TAG="${{ needs.build.outputs.tag }}"
```

### ✅ Release Upload

Uses GitHub CLI to upload to existing release:

```bash
gh release upload "$TAG" "$DMG_FILE" --clobber
```

The `--clobber` flag overwrites if the file already exists.

### ✅ Permissions

Both jobs have `contents: write` permission to:
- Create tags
- Create/modify releases
- Upload assets

## File Organization

```
.github/
└── workflows/
    └── gradle.yml                # Updated with macOS desktop job
```

## Example Release

When you push to main, you'll see a release like:

```
Release: release-20251212_2145

✅ Built by:
   - build (ubuntu-latest)
   - build-macos-desktop (macos-latest)

📦 Assets:
   - timekeeper.jar (110 KB)
   - timekeeper (45 MB - includes JRE)
   - Timekeeper-1.0.0.dmg (250 MB)
```

## Troubleshooting

### macOS job fails: "timekeeper: command not found"

**Cause**: The CLI binary wasn't installed properly

**Solution**: The `build-macos-desktop` job builds the binary fresh, so check the build output

### DMG file not found

**Cause**: The Gradle build failed or DMG wasn't created

**Solution**: Check the macOS job logs for Gradle errors

### Release upload fails

**Cause**: Permissions issue or network problem

**Solution**: Check that `GITHUB_TOKEN` has `contents: write` permission

## Manual Testing

To test the workflow locally before pushing:

```bash
# Test the build job (on Linux)
./gradlew build
./gradlew shadowJar
./gradlew nativeCompile

# Test the macOS job (on macOS)
./gradlew nativeCompile
cp app/build/native/nativeCompile/timekeeper /usr/local/bin/
./gradlew :desktop:packageDmg
```

## Future Enhancements

### Code Signing & Notarization

To add code signing to the macOS job:

```yaml
- name: Sign and Notarize DMG
  run: |
    codesign --force --deep --sign "Developer ID Application: Your Name" \
        "desktop/build/compose/binaries/main/app/Timekeeper.app"
    xcrun notarytool submit "Timekeeper-1.0.0.dmg" ...
```

### Release Notes

The workflow uses `generate_release_notes: true`, which automatically generates release notes from commits.

### Conditional Execution

To run the macOS job only on tags:

```yaml
on:
  push:
    tags:
      - 'v*'
```

## Permissions

The workflow requires these permissions:

- **contents: write** - Create tags, releases, upload assets
- **GITHUB_TOKEN** - Automatically provided by GitHub Actions

These are already configured in the workflow file.

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [softprops/action-gh-release](https://github.com/softprops/action-gh-release)
- [GitHub CLI](https://cli.github.com/)
