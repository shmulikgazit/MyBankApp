# LivePerson Android SDK Demo App

This is a demo Android application that demonstrates how to integrate the LivePerson Messaging SDK into your Android app. It's designed as a learning project to understand Android development and LivePerson integration using **unauthenticated conversations with Monitoring**.

## 📱 What This App Does

- Demonstrates LivePerson SDK integration
- Shows how to start unauthenticated conversations
- Provides a clean, modern UI as a learning example
- Includes proper error handling and user feedback
- Uses demo credentials for immediate testing

## 🏗️ Project Structure

```
MyBankApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/mybrand/livepersondemo/
│   │   │   └── MainActivity.java          # Main activity with LivePerson integration
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml      # Main screen layout
│   │   │   ├── values/
│   │   │   │   ├── strings.xml            # App strings and text
│   │   │   │   ├── colors.xml             # Color definitions
│   │   │   │   └── styles.xml             # App styling
│   │   │   ├── drawable/                  # Icons and button backgrounds
│   │   │   └── xml/                       # Backup and data extraction rules
│   │   └── AndroidManifest.xml            # App permissions and activities
│   ├── build.gradle                       # App-level dependencies
│   └── proguard-rules.pro                 # ProGuard rules for release builds
├── build.gradle                           # Project-level configuration
├── settings.gradle                        # Project settings
└── gradle.properties                      # Gradle configuration
```

## 🚀 Getting Started

### Prerequisites

1. **Android Studio** (latest version recommended)
2. **Android SDK** with minimum API level 21 (Android 5.0)
3. **Java 8** or higher
4. **Internet connection** for downloading dependencies

### Setup Instructions

1. **Open the Project**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to your `MyBankApp` folder and open it

2. **Sync Dependencies**
   - Android Studio should automatically start syncing
   - If not, click "Sync Now" in the notification bar
   - Wait for all dependencies to download (this may take a few minutes)

3. **Build the Project**
   - Go to Build → Make Project (or press Ctrl+F9)
   - Ensure there are no compilation errors

4. **Run the App**
   - Connect an Android device or start an emulator
   - Click the "Run" button (green play icon) or press Shift+F10
   - Select your target device

## 🎯 How to Use the App

1. **Launch the App**
   - The app will open with a simple interface showing the LivePerson demo

2. **Start a Conversation**
   - Tap the "Start Conversation" button
   - The app will initialize the LivePerson SDK (you'll see "Initializing..." briefly)
   - A conversation screen will open where you can chat

3. **Chat Experience**
   - You can type messages and send them
   - This is a demo environment, so responses may be automated or from demo agents
   - You can exit the conversation by pressing the back button

## 🔧 Key Android Development Concepts Demonstrated

### 1. **Activity Lifecycle**
- `MainActivity.java` shows proper activity lifecycle management
- Demonstrates `onCreate()`, `onResume()`, and `onPause()` methods

### 2. **UI Components**
- Uses `ConstraintLayout` for responsive design
- Implements `Button`, `TextView`, and `ImageView` components
- Shows proper event handling with `OnClickListener`

### 3. **Resources Management**
- **Strings**: All text is externalized in `strings.xml`
- **Colors**: Consistent color scheme in `colors.xml`
- **Styles**: Reusable styling in `styles.xml`
- **Layouts**: Clean XML layout structure

### 4. **Threading**
- Demonstrates proper UI thread handling with `runOnUiThread()`
- Shows background operations for SDK initialization

### 5. **Error Handling**
- Implements proper error handling for SDK initialization
- Uses `Toast` for user feedback
- Includes logging for debugging

### 6. **Permissions**
- Shows how to declare permissions in `AndroidManifest.xml`
- Includes permissions required for messaging (camera, storage, etc.)

## 📚 LivePerson SDK Integration Details

### Authentication Type
This app uses **unauthenticated conversations** which are perfect for:
- Demo applications
- Guest users
- Getting started with LivePerson
- Learning and testing

### Key Integration Points

1. **Initialization**
   ```java
   LivePerson.initialize(this, new InitLivePersonProperties(
       BRAND_ID, APP_ID, monitoringInitParams, callback));
   ```

2. **Starting Conversations (UN_AUTH + Monitoring)**
   ```java
   // 1) Initialize Monitoring + LivePerson
   LivePerson.initialize(getApplicationContext(), new InitLivePersonProperties(
       BRAND_ID,
       APP_ID,
       new MonitoringInitParams(APP_INSTALL_ID),
       callback));

   // 2) Show conversation (unauth)
   LPAuthenticationParams params = new LPAuthenticationParams(LPAuthenticationType.UN_AUTH);
   LivePerson.showConversation(this, params, new ConversationViewParams());
   ```

3. **Configure Your Account**
   - Replace `BRAND_ID` in `MainActivity.java` with your account ID
   - Replace `APP_INSTALL_ID` with your Monitoring App Install ID (from LP Console)

## 🛠️ Customization Options

### Changing Colors
Edit `app/src/main/res/values/colors.xml` to customize the app's color scheme.

### Modifying Text
All app text is in `app/src/main/res/values/strings.xml` for easy localization.

### UI Layout Changes
Modify `app/src/main/res/layout/activity_main.xml` to change the interface.

### Using Your Own LivePerson Account
1. Replace `BRAND_ID` in `MainActivity.java` with your account ID
2. Replace `APP_INSTALL_ID` with your Monitoring App Install ID
3. Update `APP_ID` to match your app's package name
4. If you use a corporate network or custom DNS and the emulator can’t resolve LP domains (e.g., `lo.v.liveperson.net`), set the emulator’s Private DNS to Off/Automatic, or set AVD DNS servers to `8.8.8.8,1.1.1.1` and cold boot.

### Enabling Monitoring for Unauthenticated Conversations
To receive an engagement in the UN_AUTH flow you must configure a Mobile App channel and an associated engagement in LivePerson:

1) Create a Mobile App channel
- In CCUI: Manage → Channel Setup → Mobile App
- Create a Mobile App entry and copy its “App Install ID”
- Paste this value into `APP_INSTALL_ID` in `MainActivity.java` (or set it in the app via Settings)

2) Create an unauthenticated engagement
- Base it on the Mobile App channel created above
- Ensure the engagement’s targeting allows your device/app to be eligible
- Once active, the SDK’s Monitoring request to the monitoring domain (`*.v.liveperson.net`) will return an engagement (HTTP 201) and the conversation can start

## 🔍 Learning Resources

### Android Development
- [Android Developer Documentation](https://developer.android.com/)
- [Android Studio User Guide](https://developer.android.com/studio)
- [Java for Android](https://developer.android.com/codelabs/java-to-kotlin)

### LivePerson SDK
- [LivePerson Android SDK Documentation](https://developers.liveperson.com/mobile-app-messaging-sdk-for-android-quick-start.html)
- [SDK Configuration Guide](https://developers.liveperson.com/mobile-app-messaging-sdk-for-android-configuration.html)

## 🚨 Troubleshooting

### Common Issues

1. **Build Errors**
   - Ensure you're using Android Studio with the latest updates
   - Check that your Android SDK is updated
   - Try "Clean Project" then "Rebuild Project"

2. **SDK Not Initializing**
   - Check your internet connection
   - Verify the Brand ID is correct
   - Look at the Android logs for specific error messages

3. **App Crashes**
   - Check the Android Monitor/Logcat for crash logs
   - Ensure all required permissions are granted
   - Verify the target device meets minimum API requirements

### Getting Help
- Check the Android Studio logs (View → Tool Windows → Logcat)
- Look for error messages with the tag "LivePersonDemo"
- Refer to the LivePerson documentation for SDK-specific issues

## 📄 License

This is a demo application for learning purposes. The LivePerson SDK has its own licensing terms.

## 🎓 Next Steps for Learning

1. **Explore Authentication**: Try implementing authenticated conversations
2. **Add Features**: Experiment with push notifications, file sharing
3. **Customize UI**: Modify the LivePerson conversation UI
4. **Add More Screens**: Create a multi-screen Android app
5. **Database Integration**: Add local data storage with Room or SQLite

---

**Happy Learning! 🎉**

This demo app provides a solid foundation for understanding both Android development and LivePerson integration. Feel free to experiment, modify, and expand upon this code as you learn!
