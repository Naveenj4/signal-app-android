# Signal Android – Security & Threat Model Limitations

## 1. Executive Summary
This document delineates the technical, architectural, and operating-system boundaries of the privacy enhancements introduced in this customized Signal Android client. While these protections mitigate ordinary capture, unintentional leakage, and unprivileged export vectors, they do not guarantee defense against all adversarial classes—particularly those possessing root privilege, kernel execution, or out-of-band physical access.

---

## 2. Platform & Hardware Boundaries

### A. Root Access and Compromised OS Environments
- **Limitation**: If the host Android operating system is rooted, runs a modified kernel, or has compromised SELinux policies, platform-level guarantees are invalidated.
- **Threat Vector**: A privileged user, su daemon, Magisk module, or LSPosed/Xposed framework can intercept window compositing (`SurfaceFlinger`), hook `WindowManagerService`, or read memory buffers directly from `/dev/graphics/fb0` or process memory (`/proc/$PID/mem`).
- **Design Stance**: `FLAG_SECURE` relies on the Android OS window manager enforcing framebuffer exclusion. The application cannot prevent capture if the underlying kernel or system framework is untrusted.

### B. Hardware & Out-of-Band Observation
- **Limitation**: External cameras, secondary recording devices, or optical scanning.
- **Threat Vector**: An observer photographing or video-recording the physical device screen with an external phone or camera.
- **Design Stance**: This physical vector is outside the domain of software-based application security.

### C. Flash Memory & Wear-Leveling Characteristics
- **Limitation**: Physical persistence on NAND/UFS flash memory.
- **Threat Vector**: Standard filesystem deletion (`File.delete()`) unlinks inode entries and marks blocks as free in the filesystem metadata (ext4/f2fs). However, underlying flash translation layers (FTL) perform wear-leveling and garbage collection asynchronously. Raw flash chips might retain residual physical bit patterns until garbage collection overwrites them.
- **Design Stance**: Because Signal encrypts all parts at rest using `AttachmentSecret` before writing to flash, even residual raw flash blocks contain only AES ciphertext, mitigating forensic extraction.

### D. Accessibility Services & Screen Readers
- **Limitation**: Android Accessibility API (`AccessibilityService`).
- **Threat Vector**: Legitimate or malicious accessibility services granted system permission by the user can inspect UI node hierarchies (`AccessibilityNodeInfo`) to extract text and descriptions.
- **Design Stance**: While `FLAG_SECURE` successfully prevents accessibility services from capturing bitmap screen buffers (`takeScreenshot`), text descriptions (such as message text or image content descriptions) can be read by enabled accessibility tools. This is a deliberate platform compromise to preserve accessibility for visually impaired users.

---

## 3. Scope of Protection Provided

| Vector | Protected | Notes |
| :--- | :---: | :--- |
| Standard Android Screenshot (Buttons/Gestures) | **YES** | Blocked via `FLAG_SECURE`. |
| Android Screen Recording (System or Third-Party App) | **YES** | Screen recorder records black frames for app window. |
| Android Recent Apps Switcher (Task Thumbnails) | **YES** | Blank thumbnail rendered in overview. |
| Exporting Received Photos/Videos to Android Gallery | **YES** | Blocked via `MediaExportPolicy` and `SaveAttachmentUtil`. |
| Exporting Received Media to Third-Party Apps | **YES** | Share sheet disabled and `PartProvider` access restricted. |
| Automatic Media Scanning by OS | **YES** | Parts reside in app-private storage with `.nomedia` properties. |
| Plaintext Attachment Storage on Shared SD Card | **YES** | All attachments stored as AES ciphertext in app-private directory. |
| Rooted Device / Magisk / Xposed Hooking | **NO** | Kernel/system compromise bypasses user-space sandbox. |
| External Optical Capture (Second Camera) | **NO** | Physical world limitation. |
| Screen Sharing / Casting over Miracast / Chromecast | **YES** | Miracast/Chromecast mirrors black screen for secure windows. |
