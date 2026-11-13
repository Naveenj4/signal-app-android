/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.privacy

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.thoughtcrime.securesms.BuildConfig

class MediaExportPolicyTest {

  @Before
  fun setUp() {
    MediaExportPolicy.isExportRestrictionEnforced = true
    MediaExportPolicy.isGroupOnlyPhotoSharingEnforced = true
  }

  @After
  fun tearDown() {
    MediaExportPolicy.isExportRestrictionEnforced = true
    MediaExportPolicy.isGroupOnlyPhotoSharingEnforced = true
  }

  @Test
  fun `sending photos in group chat is allowed`() {
    assertThat(MediaExportPolicy.isPhotoSendAllowed(isGroup = true)).isTrue()
  }

  @Test
  fun `sending photos in 1-to-1 person chat is disallowed when policy enforced`() {
    assertThat(MediaExportPolicy.isPhotoSendAllowed(isGroup = false)).isFalse()
  }

  @Test
  fun `saving received media to storage is disallowed when policy enforced`() {
    assertThat(MediaExportPolicy.isSaveToStorageAllowed(isOutgoing = false)).isFalse()
  }

  @Test
  fun `saving outgoing media to storage is allowed when policy enforced`() {
    assertThat(MediaExportPolicy.isSaveToStorageAllowed(isOutgoing = true)).isTrue()
  }

  @Test
  fun `external sharing of received media is disallowed when policy enforced`() {
    assertThat(MediaExportPolicy.isExternalShareAllowed(isOutgoing = false)).isFalse()
  }

  @Test
  fun `external sharing of outgoing media is allowed when policy enforced`() {
    assertThat(MediaExportPolicy.isExternalShareAllowed(isOutgoing = true)).isTrue()
  }

  @Test
  fun `external package access is disallowed for third-party packages`() {
    assertThat(MediaExportPolicy.isExternalPackageAccessAllowed("com.suspicious.app")).isFalse()
  }

  @Test
  fun `external package access is allowed for own application ID`() {
    assertThat(MediaExportPolicy.isExternalPackageAccessAllowed(BuildConfig.APPLICATION_ID)).isTrue()
  }

  @Test
  fun `all export operations allowed when policy unenforced`() {
    MediaExportPolicy.isExportRestrictionEnforced = false
    MediaExportPolicy.isGroupOnlyPhotoSharingEnforced = false

    assertThat(MediaExportPolicy.isPhotoSendAllowed(isGroup = false)).isTrue()
    assertThat(MediaExportPolicy.isSaveToStorageAllowed(isOutgoing = false)).isTrue()
    assertThat(MediaExportPolicy.isExternalShareAllowed(isOutgoing = false)).isTrue()
    assertThat(MediaExportPolicy.isExternalPackageAccessAllowed("com.suspicious.app")).isTrue()
  }
}
