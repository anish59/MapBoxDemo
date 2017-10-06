package com.mapboxdemo.helper;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

/**
 * Created by anish on 02-10-2017.
 */

public class FunctionHelper {

    public static void askForPermission(Context context, @NonNull String[] permissions, PermissionListener permissionListener) {
        if (permissions.length == 0 && permissionListener == null) {
            return;
        }
        TedPermission.with(context)
                .setPermissionListener(permissionListener)
                .setDeniedMessage("If you reject permission,you can not use this service\\n\\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(permissions)
                .check();
    }
}
