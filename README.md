# CalorieSnap - AI-Powered Nutrition Assistant

<div align="center">
  <img src="assets/1.png" alt="CalorieSnap App Screenshot" width="300"/>
</div>

An elegant, AI-powered nutrition assistant that estimates calories and macro/micronutrients instantly from a single food photo.

## Features

- 📸 **Photo-Based Analysis** - Snap a picture of your meal to get instant calorie estimates
- 🥗 **Macro & Micronutrient Breakdown** - Get detailed nutritional information in seconds
- 🤖 **AI-Powered** - Advanced AI technology for accurate food recognition and analysis
- ⚡ **Lightning Fast** - Instant results from a single photo

## Prerequisites

- [Android Studio](https://developer.android.com/studio)

## Setup & Run Locally

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and configure it with your API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

## Project Structure

```
foodcalory/
├── README.md              # This file
├── build.gradle.kts       # Root build configuration
├── settings.gradle.kts    # Settings configuration
├── .env.example           # Example environment variables
├── app/                   # Android application source code
│   ├── build.gradle.kts  # App module build config
│   └── src/              # Application source files
│       └── main/         # Main application code
└── assets/               # Asset images and resources
    ├── 1.png             # Screenshot 1
    ├── 2.png             # Screenshot 2
    ├── 3.png             # Screenshot 3
    └── 4.png             # Screenshot 4
```





## License

This project is proprietary software. All rights reserved.
