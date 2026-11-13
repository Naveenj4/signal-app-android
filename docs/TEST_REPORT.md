# SECURECHAT – Comprehensive Test Plan & Matrix

## 1. Scope & Verification Strategy
This document establishes the test scenarios, validation steps, and expected outcomes across all functional, security, and regression domains defined in the SecureChat Master Specification.

---

## 2. Test Matrices

### 2.1 Feature A: Screenshot & Screen Recording Protection
| Test ID | Test Scenario | Expected Result | Status |
| :--- | :--- | :--- | :---: |
| **SEC-01** | Private 1:1 conversation screenshot | OS blocks capture / produces black bitmap | Planned |
| **SEC-02** | Group conversation screenshot | OS blocks capture / produces black bitmap | Planned |
| **SEC-03** | Fullscreen Image / Video preview screenshot | OS blocks capture / produces black bitmap | Planned |
| **SEC-04** | Screen recording during conversation | Recorded video captures black canvas | Planned |
| **SEC-05** | Recent-apps switcher task thumbnail | Thumbnail is blank/black; no conversation preview | Planned |
| **SEC-06** | Screen rotation & Activity recreation | `FLAG_SECURE` remains active continuously | Planned |
| **SEC-07** | Android 14+ ScreenCaptureCallback | Callback fires audit log; content remains secured | Planned |

### 2.2 Feature B: 1:1 Private Photo Messaging
| Test ID | Test Scenario | Expected Result | Status |
| :--- | :--- | :--- | :---: |
| **PHT-01** | Admin sends 1:1 photo | Attachment sent, delivered, and decrypted | Planned |
| **PHT-02** | Regular member sends 1:1 photo | Attachment sent, delivered, and decrypted | Planned |
| **PHT-03** | Multiple photos selected in 1:1 | All photos attached, encrypted, and delivered | Planned |
| **PHT-04** | Photo upload failure & retry | Existing retry queue and re-encryption succeed | Planned |
| **PHT-05** | Photo preview in 1:1 conversation | In-memory rendering succeeds under `FLAG_SECURE` | Planned |

### 2.3 Feature C: Group Photo Permission System (14 Scenarios)
| Test ID | Test Scenario | Expected Result | Status |
| :--- | :--- | :--- | :---: |
| **GRP-01** | Admin sends group photo | **ALLOWED**: Photo attaches and sends cleanly | Planned |
| **GRP-02** | Regular member sends group photo | **DENIED**: Intercepted with explanation toast | Planned |
| **GRP-03** | Group creator with admin rights sends photo | **ALLOWED**: Photo attaches and sends cleanly | Planned |
| **GRP-04** | Admin sends multiple group photos | **ALLOWED**: Multiple photos attach and send | Planned |
| **GRP-05** | Regular member selects a photo from picker | **DENIED**: Blocked with explanation toast | Planned |
| **GRP-06** | Regular member attempts camera attachment | **DENIED**: Suppressed or blocked for group | Planned |
| **GRP-07** | Regular member sends text message | **ALLOWED**: Text messaging fully operational | Planned |
| **GRP-08** | Admin sends text message | **ALLOWED**: Text messaging fully operational | Planned |
| **GRP-09** | Regular member promoted to Admin | **ALLOWED**: Photo permissions become active | Planned |
| **GRP-10** | Admin demoted to Regular Member | **DENIED**: Photo sending immediately revoked | Planned |
| **GRP-11** | Admin removed from group | **DENIED**: All group actions revoked | Planned |
| **GRP-12** | Stale permission state during send | **DENIED**: GroupTable revalidated before upload | Planned |
| **GRP-13** | Unauthorized incoming group photo | **REJECTED**: Suppressed; does not render | Planned |
| **GRP-14** | Membership change during active upload | Job re-checks role; cancels if demoted | Planned |

### 2.4 Feature D: External Media Sharing Protection
| Test ID | Test Scenario | Expected Result | Status |
| :--- | :--- | :--- | :---: |
| **EXP-01** | "Save to Gallery" on received media | Blocked; no file created in `MediaStore` | Planned |
| **EXP-02** | "Save to Downloads" on received media | Blocked; no file created in Downloads | Planned |
| **EXP-03** | Android Share Sheet (`ACTION_SEND`) | Action removed/disabled for received media | Planned |
| **EXP-04** | Third-party app querying `PartProvider` | Throws `SecurityException` | Planned |
| **EXP-05** | Drag-and-drop of received media | Export payload blocked | Planned |

### 2.5 Feature E: Temporary Encrypted Storage
| Test ID | Test Scenario | Expected Result | Status |
| :--- | :--- | :--- | :---: |
| **RET-01** | Check public folders (`Pictures/`, `DCIM/`) | No plaintext media files found | Planned |
| **RET-02** | Account logout / deregistration | Cache swept; keys destroyed | Planned |
| **RET-03** | Application low memory trim (`onTrimMemory`) | Decrypted memory buffers purged | Planned |
| **RET-04** | App crash recovery / process death | Orphaned temporary files cleaned on startup | Planned |

### 2.6 Baseline Signal Regression Tests
| Test ID | Test Scenario | Expected Result | Status |
| :--- | :--- | :--- | :---: |
| **REG-01** | User registration & authentication | Verification code flow operational | Planned |
| **REG-02** | Voice and video calling | WebRTC calls connect with audio/video | Planned |
| **REG-03** | Push notifications | Messages wake client and notify user | Planned |
| **REG-04** | SQLCipher database integrity | Preserved across restarts and updates | Planned |

