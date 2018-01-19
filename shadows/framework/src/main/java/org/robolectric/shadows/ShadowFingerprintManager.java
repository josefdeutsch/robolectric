package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.N_MR1;

import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.os.Handler;
import android.util.Log;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Provides testing APIs for {@link FingerprintManager}
 */
@Implements(FingerprintManager.class)
public class ShadowFingerprintManager {

  private static final String TAG = "ShadowFingerprintManager";

  private boolean isHardwareDetected;
  private boolean hasEnrolledFingerprints;
  private CryptoObject pendingCryptoObject;
  private AuthenticationCallback pendingCallback;

  /**
   * Simulate a successful fingerprint authentication. An authentication request must have been
   * issued with {@link FingerprintManager#authenticate(CryptoObject, CancellationSignal, int, AuthenticationCallback, Handler)} and not cancelled.
   */
  public void authenticationSucceeds() {
    if (pendingCallback == null) {
      throw new IllegalStateException("No active fingerprint authentication request.");
    }

    AuthenticationResult result;
    if (RuntimeEnvironment.getApiLevel() >= N_MR1) {
      result = new AuthenticationResult(pendingCryptoObject, null, 0);
    } else {
      result = ReflectionHelpers.callConstructor(AuthenticationResult.class,
          ClassParameter.from(CryptoObject.class, pendingCryptoObject),
          ClassParameter.from(Fingerprint.class, null));
    }

    pendingCallback.onAuthenticationSucceeded(
        result);
  }

  /**
   * Simulate a failed fingerprint authentication. An authentication request must have been
   * issued with {@link FingerprintManager#authenticate(CryptoObject, CancellationSignal, int, AuthenticationCallback, Handler)} and not cancelled.
   */
  public void authenticationFails() {
    if (pendingCallback == null) {
      throw new IllegalStateException("No active fingerprint authentication request.");
    }

    pendingCallback.onAuthenticationFailed();
  }

  @Implementation
  public void authenticate(CryptoObject crypto, CancellationSignal cancel,
      int flags, AuthenticationCallback callback, Handler handler) {
    if (callback == null) {
      throw new IllegalArgumentException("Must supply an authentication callback");
    }

    if (cancel != null) {
      if (cancel.isCanceled()) {
        Log.w(TAG, "authentication already canceled");
        return;
      } else {
        cancel.setOnCancelListener(() -> {
          this.pendingCallback = null;
          this.pendingCryptoObject = null;
        });
      }
    }

    this.pendingCryptoObject = crypto;
    this.pendingCallback = callback;
  }

  /**
   * Sets the return value of {@link FingerprintManager#hasEnrolledFingerprints()}
   */
  public void setHasEnrolledFingerprints(boolean hasEnrolledFingerprints) {
    this.hasEnrolledFingerprints = hasEnrolledFingerprints;
  }

  @Implementation
  public boolean hasEnrolledFingerprints() {
    return this.hasEnrolledFingerprints;
  }

  /**
   * Sets the return value of {@link FingerprintManager#isHardwareDetected()}
   */
  public void setIsHardwareDetected(boolean isHardwareDetected) {
    this.isHardwareDetected = isHardwareDetected;
  }

  @Implementation
  public boolean isHardwareDetected() {
    return this.isHardwareDetected;
  }
}
