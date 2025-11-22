# Jellyfin Jellyseerr TV Integration

This repository provides the client-side integration to bring the full power of Jellyseerr directly to your Android TV via Jellyfin.

## ⚠️ Prerequisite: Server-Side Plugin (Required!)

**Before you start:** This Android TV integration requires the **Jellyfin Requests Bridge** plugin to be installed and configured on your Jellyfin server. Without it, the "Discover" features will not work.

👉 **Step 1: Install the Server Plugin first:**
[**Go to Serekay/jellyfin-requests-bridge**](https://github.com/Serekay/jellyfin-requests-bridge)

Follow the installation instructions there. Once the plugin is running on your server, proceed with the setup below.

---

## 📺 Remote Access Guide: Jellyfin + Tailscale (Android TV)

This guide explains how to set up **Tailscale** on your Android TV to access your Jellyfin server securely from anywhere, without opening ports on your router.

We will also configure an **"Always-On"** feature using ADB, ensuring the VPN starts automatically after a reboot while allowing other apps (Netflix, YouTube) to bypass the VPN.

### Step 1: Install & Connect Tailscale on TV

1.  **Download App:** Go to the Google Play Store on your TV and install **"Tailscale"**.
2.  **Log In:** Open the app and select **"Log in"**.
    * A QR code or a URL with a 6-digit code will appear.
3.  **Authorize Device:**
    * On your PC or Phone, go to the [Tailscale Admin Console](https://login.tailscale.com/admin/machines).
    * Select **"Add Device"** and enter the code displayed on your TV.
    * Log in with your Google/Microsoft/GitHub account if prompted.
4.  **Verify:** Once authorized, the TV app should say "Connected".

### Step 2: Connect Jellyfin

1.  **Install Jellyfin:** Install the **Jellyfin** app from the Google Play Store on your TV.
2.  **Add Server:**
    * Open Jellyfin.
    * Enter the **Tailscale IP address** of your Jellyfin server (e.g., `http://100.x.x.x:8096`).
3.  **Login:** Enter your Jellyfin username and password.

> **Tip for PC/Laptop Users:** If you are setting this up on a remote laptop instead of a TV, open a command prompt (CMD) after installing Tailscale and run `tailscale up`. Send the generated link to your Admin to authorize the device without sharing login credentials.

---

### ⚡ Bonus: Enable "Always-On" Auto-Connect (ADB Method)

By default, Android TV might close the VPN connection after a restart. We will use a tool called **ADB TV** to force Tailscale to start automatically in the background.

**How this works:**
* **Auto-Start:** Tailscale connects silently in the background immediately after the TV boots.
* **Split Tunneling:** Only traffic destined for your Tailscale network (Jellyfin) goes through the VPN. **Apps like Netflix, YouTube, or Disney+ continue to use your normal home internet connection directly**, ensuring no speed loss or geo-blocking issues for streaming services.

#### Instructions:

1.  **Install ADB TV:**
    * Open the Google Play Store on your TV.
    * Search for and install **"ADB TV"** (sometimes called "ADB Shell").

2.  **Enable Developer Options:**
    * Go to Android TV **Settings** → **Device Preferences** → **About**.
    * Scroll down to **"Build"** (Build Number).
    * Click the **Select/OK button 7 times** rapidly until it says "You are now a developer!".

3.  **Enable USB Debugging:**
    * Go back to **Settings** → **Device Preferences** → **Developer Options**.
    * Find **"USB Debugging"** and turn it **ON**.
    * Open the **ADB TV** app you installed. It might ask for permission to access the device—allow it ("Always allow from this computer").

4.  **Run Commands:**
    * Inside the ADB TV app, you will see a command console/input field.
    * Enter the following two commands one by one (press Enter/Run after each line):

    **Command 1 (Set Tailscale as Always-On VPN):**
    ```bash
    settings put secure always_on_vpn_app com.tailscale.ipn
    ```

    **Command 2 (Disable Lockdown - allows other apps to use normal internet):**
    ```bash
    settings put secure always_on_vpn_lockdown 0
    ```

#### ✅ Setup Complete
Restart your TV. Tailscale will now automatically connect in the background. You can open Jellyfin immediately, and it will work, while your other streaming apps remain unaffected.