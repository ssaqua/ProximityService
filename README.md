# ProximityService

<a href="https://play.google.com/store/apps/details?id=ss.proximityservice">
  <img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="100">
</a>

Minimal Android service for turning off the screen using the proximity sensor.

Does nothing other than acquiring the [PROXIMITY_SCREEN_OFF_WAKE_LOCK](https://developer.android.com/reference/android/os/PowerManager.html#PROXIMITY_SCREEN_OFF_WAKE_LOCK)
and disabling the [KeyguardLock](https://developer.android.com/reference/android/app/KeyguardManager.KeyguardLock.html)
on starting the service and doing the reverse on stopping the service, as well as showing a few toasts
where appropriate to indicate the service status.

## License


    Copyright 2016 ssaqua

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
