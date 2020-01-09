# System Preferences
_A shared preference store with the key "systemprefs" set to private._

## Booleans
- guide_filter_kids
  **Preferences.xml**: ❌
  **Default**: `false`
  **Use:** Stores the `kids` filter in the live tv guide
- guide_filter_movies
  **Preferences.xml**: ❌
  **Default**: `false`
  **Use:** Stores the `movies` filter in the live tv guide
- guide_filter_news
  **Preferences.xml**: ❌
  **Default**: `false`
  **Use:** Stores the `news` filter in the live tv guide
- guide_filter_premiere
  **Preferences.xml**: ❌
  **Default**: `false`
  **Use:** Stores the `premiere` filter in the live tv guide
- guide_filter_series
  **Preferences.xml**: ❌
  **Default**: `false`
  **Use:** Stores the `series` filter in the live tv guide
- guide_filter_sports
  **Preferences.xml**: ❌
  **Default**: `false`
  **Use:** Stores the `sports` filter in the live tv guide
- syspref_audio_warned
  **Preferences.xml**: ❌
  **Default**: `false`
  **Use:** Will display a message to the user requesting the value for `pref_audio_option` when set to `false`.
           Value is changed to `true` after choosing an option causing this to only display the first time the app is used.

## Strings
- sys_pref_config_version
  **Preferences.xml**: ❌
  **Use:** Stores the current revision of the preferences.
           It contains an integer (saved as string) which can be used to check the revision.
           Current revision is **5**
- sys_pref_last_tv_channel
  **Preferences.xml**: ❌
  **Use:** Used to store the last chosen live TV channel
- sys_pref_prev_tv_channel
  **Preferences.xml**: ❌
  **Use:** Used to store the previously chosen live TV channel

# Shared Preferences
_The default shared preference store obtained with `getDefaultSharedPreferences()`._

## Booleans
- pref_alt_pw_entry
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_auto_pw_prompt
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_bitstream_ac3
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_bitstream_dts
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_enable_cinema_mode
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_enable_debug
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_enable_info_panel
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_enable_premieres
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_enable_themes
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_enable_tv_queuing
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_enable_vlc_livetv
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_live_direct
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_live_tv_mode
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_live_tv_use_external
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_refresh_switching
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_send_path_external
  **Preferences.xml**: ✔
  **Use:** [TODO]
- pref_show_backdrop
  **Preferences.xml**: ✔
  **Use:** [TODO]

## Strings
- pref_audio_option
  **Preferences.xml**: ✔
  **Value**: _Enum_
  - 0 (Direct)
  - 1 (Downmix to Stereo)
  **Default**: `0` _note: Value is asked on first start_
  **Use:** When set to `1` audio will be downmixed.
           Disables the AC3, EAC3 and AAC_LATM audio codecs
- pref_auto_logoff_timeout
  **Preferences.xml**: ✔
  **Value**: _Timeout in milliseconds_
  - 1800000 (30 Minutes)
  - 3600000 (1 Hour)
  - 7200000 (2 Hours)
  - 10800000 (3 Hours)
  - 21600000 (6 Hours)
  - 86400000 (24 Hours)
  **Use:** How long a login is active.
           Only works when `pref_login_behavior` is set to `0` 
- pref_login_behavior
  **Preferences.xml**: ✔
  **Value**: _Enum_
  - 0 (Show login screen)
  - 1 (Automatically login as this user)
  **Default**: `0`
  **Use:** Behavior for login when starting the app
- pref_max_bitrate
  **Preferences.xml**: ✔
  **Value**: _Bitrate in megabit_
  - 0 (Auto)
  - 120 (120 Mbits/sec)
  - 110 (110 Mbits/sec)
  - 100 (100 Mbits/sec)
  - 90 (90 Mbits/sec)
  - 80 (80 Mbits/sec)
  - 70 (70 Mbits/sec)
  - 60 (60 Mbits/sec)
  - 50 (50 Mbits/sec)
  - 40 (40 Mbits/sec)
  - 30 (30 Mbits/sec)
  - 21 (21 Mbits/sec)
  - 15 (15 Mbits/sec)
  - 10 (10 Mbits/sec)
  - 5 (5 Mbits/sec)
  - 3 (3 Mbits/sec)
  - 2 (2 Mbits/sec)
  - 1.5 (1.5 Mbits/sec)
  - 1 (1 Mbit/sec)
  - 0.72 (720 Kbits/sec)
  - 0.42 (420 Kbits/sec)
  **Default**: `0`
  **Use:** Preferred maximum bitrate when watching series/movies.
- pref_resume_preroll
  **Preferences.xml**: ✔
  **Value**: _Duration in seconds_
  - 0 (None)
  - 5 (5 seconds)
  - 10 (10 seconds)
  - 20 (20 seconds)
  - 30 (30 seconds)
  - 60 (1 minute)
  - 120 (2 minutes)
  - 300 (5 minutes)  
  **Default**: `0`
  **Use:** [TODO]
- pref_video_player
  **Preferences.xml**: ✔
  **Value**: Preferred video player
    - auto
    - exoplayer
    - libvlc
    - external  
    **Default**: `auto`
  **Use:** [TODO]

# Preferences.xml
_All preferences defined in the `preferences.xml` file._
## Acra
_Not mentioned in code because the library searches the values by itself. See [Letting your users control ACRA](https://github.com/ACRA/acra/wiki/AdvancedUsage#letting-your-users-control-acra) for more information about these preferences._
- acra.alwaysaccept
- acra.enable
- acra.syslog.enable

## Other
_These are all documented above_
- pref_alt_pw_entry
- pref_audio_option
- pref_auto_logoff_timeout
- pref_auto_pw_prompt
- pref_bitstream_ac3
- pref_bitstream_dts
- pref_enable_cinema_mode
- pref_enable_debug
- pref_enable_info_panel
- pref_enable_premieres
- pref_enable_themes
- pref_enable_tv_queuing
- pref_enable_vlc_livetv
- pref_live_direct
- pref_live_tv_category
- pref_live_tv_mode
- pref_live_tv_use_external
- pref_login_behavior
- pref_max_bitrate
- pref_playback_category
- pref_refresh_switching
- pref_reporting_category
- pref_resume_preroll
- pref_send_path_external
- pref_show_backdrop
- pref_video_player