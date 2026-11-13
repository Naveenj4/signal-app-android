/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.privacy

import android.net.Uri
import android.os.Binder
import android.os.Process
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.BuildConfig

/**
 * Central policy engine determining whether media export and sharing operations are permitted.
 *
 * Implements strict privacy rules:
 * 1. Blocks saving received/in-app media to public device storage (Gallery, Downloads, MediaStore).
 * 2. Blocks sharing received media to external applications via Android Share Sheets (ACTION_SEND).
 * 3. Restricts PartProvider access to the application's own process / UID.
 * 4. Restricts photo sending to group conversations only (disallowed in 1:1 person-to-person chats).
 */
object MediaExportPolicy {

  private val TAG = Log.tag(MediaExportPolicy::class.java)

  /**
   * When true, export of received protected media is strictly blocked.
   * Default is true.
   */
  @Volatile
  var isExportRestrictionEnforced: Boolean = true

  /**
   * When true, sending photos is permitted only in group chats, not in 1:1 person-to-person chats.
   * Default is true.
   */
  @Volatile
  var isGroupOnlyPhotoSharingEnforced: Boolean = true

  /**
   * Evaluates whether sending photos is permitted for the given conversation type.
   *
   * @param isGroup True if the destination conversation is a group, false if 1:1 chat.
   * @return True if sending photos is allowed, false if blocked by policy.
   */
  @JvmStatic
  fun isPhotoSendAllowed(isGroup: Boolean): Boolean {
    if (!isGroupOnlyPhotoSharingEnforced) {
      return true
    }
    return isGroup
  }

  /**
   * Evaluates whether saving the specified media attachment to shared storage is permitted.
   *
   * @param uri The URI of the media attachment, or null if checking general permission.
   * @param isOutgoing Whether the media is outgoing content owned by the sender.
   * @return True if saving to storage is allowed, false if blocked by policy.
   */
  @JvmStatic
  @JvmOverloads
  fun isSaveToStorageAllowed(uri: Uri? = null, isOutgoing: Boolean = false): Boolean {
    if (!isExportRestrictionEnforced) {
      return true
    }
    // Only user-selected outgoing media is permitted to be saved if needed;
    // received protected media is strictly forbidden from being written to shared storage.
    return isOutgoing
  }

  /**
   * Evaluates whether sharing the specified media attachment to external apps is permitted.
   *
   * @param uri The URI of the media attachment, or null if checking general permission.
   * @param isOutgoing Whether the media is outgoing content owned by the sender.
   * @return True if sharing to external apps is allowed, false if blocked by policy.
   */
  @JvmStatic
  @JvmOverloads
  fun isExternalShareAllowed(uri: Uri? = null, isOutgoing: Boolean = false): Boolean {
    if (!isExportRestrictionEnforced) {
      return true
    }
    return isOutgoing
  }

  /**
   * Evaluates whether an external package or calling UID is authorized to read media parts
   * from PartProvider.
   *
   * @param callingPackage The package name reported by ContentProvider.getCallingPackage().
   * @return True if the caller is authorized (app's own process), false if an external third party.
   */
  @JvmStatic
  fun isExternalPackageAccessAllowed(callingPackage: String?): Boolean {
    if (!isExportRestrictionEnforced) {
      return true
    }

    val callingUid = Binder.getCallingUid()
    val myUid = Process.myUid()

    if (callingUid == myUid) {
      return true
    }

    if (callingPackage != null && callingPackage == BuildConfig.APPLICATION_ID) {
      return true
    }

    Log.w(TAG, "Denied external access attempt by package: $callingPackage (uid: $callingUid, expected: $myUid)")
    return false
  }
}
