package org.tasks.preferences;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastQ;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import timber.log.Timber;

public class PermissionChecker {

  private final Context context;

  @Inject
  public PermissionChecker(@ApplicationContext Context context) {
    this.context = context;
  }

  public boolean canAccessOpenTasks() {
    return checkPermissions("org.dmfs.permission.READ_TASKS", "org.dmfs.permission.WRITE_TASKS");
  }

  public boolean canAccessCalendars() {
    return checkPermissions(permission.READ_CALENDAR, permission.WRITE_CALENDAR);
  }

  public boolean canAccessAccounts() {
    return atLeastOreo() || checkPermissions(permission.GET_ACCOUNTS);
  }

  public boolean canAccessLocation() {
    return atLeastQ()
        ? checkPermissions(permission.ACCESS_FINE_LOCATION, permission.ACCESS_BACKGROUND_LOCATION)
        : checkPermissions(permission.ACCESS_FINE_LOCATION);
  }

  public boolean canAccessMic() {
    return checkPermissions(permission.RECORD_AUDIO);
  }

  private boolean checkPermissions(String... permissions) {
    for (String permission : permissions) {
      if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
        Timber.w("Request for %s denied", permission);
        return false;
      }
    }
    return true;
  }
}
