--------------------------------------------------------------
--------------------------------------------------------------
--------------------------------------------------------------

# Start emulator with visible window
/Users/sagar/Library/Android/sdk/emulator/emulator -avd Pixel_9

# In another terminal, wait 2-3 minutes for boot, then:
cd /Users/sagar/Documents/ChatGPT/medstudy
export ANDROID_HOME=/Users/sagar/Library/Android/sdk
./gradlew :app:installDebug
/Users/sagar/Library/Android/sdk/platform-tools/adb shell am start -n com.medstudy.app/com.medstudy.app.MainActivity

--------------------------------------------------------------
--------------------------------------------------------------
--------------------------------------------------------------

# List all running AVDs
/Users/sagar/Library/Android/sdk/platform-tools/adb devices

# Kill emulator for specific AVD (replace Pixel_9 with your AVD name)
/Users/sagar/Library/Android/sdk/emulator/emulator -avd Pixel_9 -no-window -kill

--------------------------------------------------------------
--------------------------------------------------------------
--------------------------------------------------------------

# List all running emulator processes
ps aux | grep emulator

# Kill all emulator processes
pkill -9 -f emulator

# Or more specifically
pkill -9 -f "/emulator"

# Check if any are still running
ps aux | grep emulator