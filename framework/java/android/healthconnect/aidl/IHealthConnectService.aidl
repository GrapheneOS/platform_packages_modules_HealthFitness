
package android.healthconnect.aidl;

import android.os.UserHandle;
import java.util.List;

/** {@hide} */
interface IHealthConnectService {
    /* @hide */
    void grantHealthPermission(String packageName, String permissionName, in UserHandle user);
    /* @hide */
    void revokeHealthPermission(String packageName, String permissionName, String reason, in UserHandle user);
    /* @hide */
    void revokeAllHealthPermissions(String packageName, String reason, in UserHandle user);
    /* @hide */
    List<String> getGrantedHealthPermissions(String packageName, in UserHandle user);

}
