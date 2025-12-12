#!/bin/bash
# Build macOS app bundle and DMG for Timekeeper Desktop
# Run this script on macOS to create the distributable package

set -e

echo "🔨 Building Timekeeper macOS Application..."

# Check if we're on macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo "❌ Error: This script must be run on macOS"
    exit 1
fi

# Check if Gradle is available
if ! command -v ./gradlew &> /dev/null; then
    echo "❌ Error: gradlew not found. Make sure you're in the project root directory"
    exit 1
fi

# Step 1: Build the desktop module
echo "📦 Building desktop module..."
./gradlew :desktop:build

# Step 2: Create the macOS package
echo "📱 Creating macOS app bundle and DMG..."
./gradlew :desktop:packageDmg

# Step 3: Find and display the output
DMG_FILE=$(find ./desktop/build/compose/binaries/main/dmg -name "*.dmg" | head -1)

if [ -f "$DMG_FILE" ]; then
    echo ""
    echo "✅ SUCCESS! App package created:"
    echo "   📁 $DMG_FILE"
    echo ""
    echo "📊 File size: $(du -h "$DMG_FILE" | cut -f1)"
    echo ""
    echo "🚀 To distribute the app:"
    echo "   1. For local testing: Double-click the DMG file to mount and drag Timekeeper.app to Applications"
    echo "   2. For distribution: Consider code signing and notarization:"
    echo ""
    echo "   Code signing:"
    echo "   codesign --force --deep --sign \"Developer ID Application: Your Name\" \\"
    echo "       ./desktop/build/compose/binaries/main/app/Timekeeper.app"
    echo ""
    echo "   Notarization (for App Store / trusted distribution):"
    echo "   xcrun notarytool submit \"$DMG_FILE\" \\"
    echo "       --apple-id \"your@email.com\" \\"
    echo "       --team-id \"TEAMID\" \\"
    echo "       --password \"app-specific-password\" \\"
    echo "       --wait"
    echo ""
    echo "   Staple the notarization ticket:"
    echo "   xcrun stapler staple ./desktop/build/compose/binaries/main/app/Timekeeper.app"
else
    echo "❌ Error: DMG file not found. Build may have failed."
    exit 1
fi
