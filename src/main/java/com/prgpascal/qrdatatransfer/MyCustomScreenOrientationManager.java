/*
 * Copyright (C) 2016 Riccardo Leschiutta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prgpascal.qrdatatransfer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.WindowManager;


/**
 * Class that locks and unlocks the Screen orientation.
 * When the screen is locked, the screen orientation is fixed. This prevents exceptions that can be thrown
 * by the Activity in case of a ProgressDialog is shown and user rotates the device affecting the Activity
 * recreation.
 *
 * I found a clean solution here:
 * http://stackoverflow.com/questions/6599770/screen-orientation-lock
 *
 * Before locking/unlocking the screen orientation, I check the device SDK version, because the
 * 'Reverse Portrait' screen orientation (defined by ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
 * constant) is not supported by FROYO and previous Android OS versions.
 *
 * @author Riccardo Leschiutta
 */
public class MyCustomScreenOrientationManager {

    /** Lock the current screen orientation */
    public static void lockScreen(Activity context){

        // Check the compatibility
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.FROYO){
            context.setRequestedOrientation(currentScreenOrientation(context));
        }
    }


    /** Unlocks the current screen orientation */
    public static void unlockScreen(Activity context){

        // Check the compatibility
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.FROYO){
            context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }


    /** Get the current screen orientation type */
    private static int currentScreenOrientation(Context context){

        // get the context WindowManager
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        switch (context.getResources().getConfiguration().orientation){
            case Configuration.ORIENTATION_PORTRAIT:
                if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.FROYO){
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

                } else {
                    int rotation = windowManager.getDefaultDisplay().getRotation();
                    if (rotation == android.view.Surface.ROTATION_90|| rotation == android.view.Surface.ROTATION_180){
                        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    } else {
                        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    }
                }

            case Configuration.ORIENTATION_LANDSCAPE:
                if(android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.FROYO){
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

                } else {
                    int rotation = windowManager.getDefaultDisplay().getRotation();
                    if(rotation == android.view.Surface.ROTATION_0 || rotation == android.view.Surface.ROTATION_90){
                        return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    } else {
                        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    }
                }

            default:
                return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

        }
    }
}