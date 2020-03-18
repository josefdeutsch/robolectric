package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.Q;

import android.os.Bundle;
import android.service.voice.VoiceInteractionService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** Shadow implementation of {@link android.service.voice.VoiceInteractionService}. */
@Implements(VoiceInteractionService.class)
public class ShadowVoiceInteractionService extends ShadowService {

  private final List<Bundle> hintBundles = Collections.synchronizedList(new ArrayList<>());

  @Implementation(minSdk = Q)
  protected void setUiHints(Bundle hints) {
    if (hints != null) {
      hintBundles.add(hints);
    }
  }

  /**
   * Returns list of bundles provided with calls to {@link #setUiHints(Bundle bundle)} in invocation
   * order.
   */
  public List<Bundle> getPreviousUiHintBundles() {
    return Collections.unmodifiableList(hintBundles);
  }

  @Nullable
  public Bundle getLastUiHintBundle() {
    if (hintBundles.isEmpty()) {
      return null;
    }

    return hintBundles.get(hintBundles.size() - 1);
  }

  public void reset() {
    hintBundles.clear();
  }
}
