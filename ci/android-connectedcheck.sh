#!/bin/bash -eux

# Minimum supported version: SDK15
# In current code, non-English test runs on SDK >=24

for sdkver in 35
do
    sdkmanager "system-images;android-$sdkver;default;x86_64"
    echo no | avdmanager create avd -f -n vm$sdkver -k "system-images;android-$sdkver;default;x86_64"
    ${ANDROID_HOME}/emulator/emulator -avd vm$sdkver -no-window -no-audio &
    EMULATOR_PID=$!
    for i in $(seq 60); do if adb -e shell getprop init.svc.bootanim ; then break ; fi; sleep 5; done
    adb -e shell getprop init.svc.bootanim
    for i in $(seq 60); do if adb -e shell getprop init.svc.bootanim | grep stopped ; then break ; fi; sleep 5; done
    adb -e shell getprop init.svc.bootanim | grep stopped
    for i in $(seq 60); do if adb -e shell pm list packages ; then break ; fi; sleep 5; done
    adb -e shell pm list packages
    for i in $(seq 10); do if adb -e shell input keyevent 82 ; then break; fi; sleep 5; done  # unlock device
    for i in $(seq 24); do if adb -e shell dumpsys power | grep mHoldingWakeLockSuspendBlocker=true ; then break; fi; sleep 5; done
    sleep 20
    # https://stackoverflow.com/questions/39457305/android-testing-waited-for-the-root-of-the-view-hierarchy-to-have-window-focus
    adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS
    sleep 10
    ./gradlew connectedCheck || (sleep 30 && ./gradlew connectedCheck)
    kill $EMULATOR_PID
    while test -e /proc/$EMULATOR_PID; do sleep 1; done
    avdmanager delete avd -n vm$sdkver
    sdkmanager --uninstall "system-images;android-$sdkver;default;x86_64"
done

for sdkver in 24 15
do
    sdkmanager "system-images;android-$sdkver;default;x86"
    echo no | avdmanager create avd -f -n vm$sdkver -k "system-images;android-$sdkver;default;x86"
    ${ANDROID_HOME}/emulator/emulator -avd vm$sdkver -no-window -no-audio &
    EMULATOR_PID=$!
    for i in $(seq 60); do if adb -e shell getprop init.svc.bootanim ; then break ; fi; sleep 5; done
    adb -e shell getprop init.svc.bootanim
    for i in $(seq 60); do if adb -e shell getprop init.svc.bootanim | grep stopped ; then break ; fi; sleep 5; done
    adb -e shell getprop init.svc.bootanim | grep stopped
    for i in $(seq 60); do if adb -e shell pm list packages ; then break ; fi; sleep 5; done
    adb -e shell pm list packages
    for i in $(seq 10); do if adb -e shell input keyevent 82 ; then break; fi; sleep 5; done  # unlock device
    for i in $(seq 24); do if adb -e shell dumpsys power | grep mHoldingWakeLockSuspendBlocker=true ; then break; fi; sleep 5; done
    sleep 30
    ./gradlew connectedCheck || (sleep 30 && ./gradlew connectedCheck)
    kill $EMULATOR_PID
    while test -e /proc/$EMULATOR_PID; do sleep 1; done
    avdmanager delete avd -n vm$sdkver
    sdkmanager --uninstall "system-images;android-$sdkver;default;x86"
done
