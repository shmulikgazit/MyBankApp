# Resolving the Chat Avatar Issue in the Android SDK

**Hi Team,**

Following up on the reported issue where the brand logo is incorrectly appearing as the avatar for all chat messages, we have identified the cause and have a clear solution for your development team.

## The Issue

After upgrading the LivePerson Messaging SDK to a version beyond 5.23.0, all messages sent by either a bot or a human agent are incorrectly displaying the company's brand logo instead of the appropriate agent or bot avatar.

## The Cause

This behavior change is due to an update introduced in **LivePerson Android SDK version 5.23.0**, which now provides separate customization for bot avatars.

The SDK now uses a more specific hierarchy to find the correct avatar:
1.  It first looks for a custom avatar URL set in the LivePerson user configuration for the specific bot or agent.
2.  If no URL is found, it looks for a custom avatar image (a "drawable") provided within your Android application.
    *   For bots, it looks for `lp_messaging_ui_ic_bot_avatar`.
    *   For human agents, it looks for `lp_messaging_ui_ic_agent_avatar`.
3.  If neither of these is provided, the SDK falls back to a default.
    *   For **bots**, the new fallback is the **brand logo**. **This is why you are seeing your brand logo for every message.**

Your app was correctly configured for the old SDK behavior but needs a small update to support this new, more specific avatar handling. For more details on this change, please see the [official release notes](https://developers.liveperson.com/mobile-app-messaging-sdk-for-android-release-notes-5-23-0.html).

## The Solution

To resolve this, your Android development team needs to provide specific drawable resources for both the bot and human agent avatars in your application. This is done by creating a simple resource file in your project and defining the overrides.

**Action for Your Development Team:**

Please instruct your developers to create a new XML file in your Android project at the following path: `app/src/main/res/values/lp_overrides.xml` (or add to an existing one).

They should add the following content to this file, replacing `@drawable/your_bot_avatar` and `@drawable/your_human_avatar` with the names of the actual drawable files for your desired bot and human avatars.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 
      Provide a custom drawable for the bot avatar.
      This will be used for all messages sent by a bot when no avatar URL is configured.
      See documentation: https://developers.liveperson.com/mobile-app-messaging-sdk-for-android-sdk-attributes-5-0-and-above.html#lp_messaging_ui_ic_bot_avatar
    -->
    <drawable name="lp_messaging_ui_ic_bot_avatar">@drawable/your_bot_avatar</drawable>

    <!-- 
      Provide a custom drawable for the human agent avatar.
      This will be used for all messages sent by a human when no avatar URL is configured.
      See documentation: https://developers.liveperson.com/mobile-app-messaging-sdk-for-android-sdk-attributes-5-0-and-above.html#lp_messaging_ui_ic_agent_avatar
    -->
    <drawable name="lp_messaging_ui_ic_agent_avatar">@drawable/your_human_avatar</drawable>
</resources>
```

By implementing these overrides, your app will correctly display the distinct avatars for bots and human agents, resolving the issue.

Please let us know if you have any further questions.
