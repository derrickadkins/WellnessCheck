package com.derrick.wellnesscheck.utils;

public interface PermissionsListener{
    void permissionsGranted();
    void permissionsDenied();
    void showRationale(String[] permissions);
}
