package com.derrick.wellnesscheck;

public interface PermissionsListener{
    void permissionsGranted();
    void permissionsDenied();
    void showRationale(String[] permissions);
}
