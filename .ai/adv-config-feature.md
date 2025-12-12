# Advanced Configuration Feature Planning

## Goal

Implement a GUI editor for Colima settings, initially focusing on CPU, RAM, and Disk configuration, as outlined in the Planned Features section of `README.md`.

## Round 1 Decisions (User Feedback)

1.  **Scope**: Skip "Runtime" selection (focus on Docker). Keep CPU, RAM, Disk, and Kubernetes.
2.  **Lifecycle**: Explicit "Save & Restart" button.
3.  **Target Profile**: Allow configuring any profile (not just the active one).
4.  **UI/UX**: Similar style to the existing Profile page (maybe larger).
5.  **Validation**: Enforce limits (blocking invalid values).

## Round 2 Decisions (User Feedback)

1.  **Disk Shrinking**: **Disable** lowering disk size for existing profiles (prevents data loss).
2.  **UI Entry Point**: Add **Gear Icon** to profile list items.
3.  **New Profiles**: Include these "Advanced" settings in the **Create Profile** dialog.
4.  **Kubernetes**: Simple **Enable/Disable** toggle.
5.  **Validation Tech**: **sysctl** is approved (verify it works without root, which it does).

## Round 3 Decisions (User Feedback)

1.  **Persistence**: Edited `~/.colima/<profile>/colima.yaml` directly.
2.  **Dependencies**: Use a YAML parsing library (e.g., `snakeyaml`).
3.  **Defaults**: **2 CPU / 2GB RAM / 60GB Disk** for new profiles.
4.  **Unsaved Changes**: Show **confirmation dialog** on close if unsaved.
5.  **Disk Units**: **GiB** only.

## Final Plan Summary

The planning phase is complete. We will implement:

- **Configuration Dialog**: Accessible via Gear icon on Profile list and "Advanced" section in Create Profile.
- **Settings**: CPU, RAM, Disk, Kubernetes (Toggle).
- **Backend**: Direct `colima.yaml` manipulation using a generic YAML library.
- **Validation**: `sysctl` based limits.

## Open Questions (Round 3)

1.  **Persistence Mechanism (Technical)**:

    - To configure _stopped/inactive_ profiles without starting them, the app must likely read/write the `colima.yaml` config file directly (as `colima start` flags only apply when starting).
    - _Recommendation_: We implement robust direct YAML editing for `~/.colima/<profile>/colima.yaml`.

2.  **Dependencies**:

    - To do the above reliably, can we add a Kotlin/Java YAML parsing library (like `snakeyaml` or various Kotlin wrappers)?
    - Yes, using a library is safer than regex for parsing config files.

3.  **Default Values**:

    - When creating a _New Profile_, what defaults should we pre-fill?
    - 2 CPUs, 2GB RAM, 60GB Disk (matches typical Colima defaults).

4.  **Unsaved Changes**:

    - If a user modifies settings but cancels/closes the dialog without saving:
    - Show a "Discard changes?" confirmation alert to prevent accidental loss of edits.

5.  **Disk Units**:
    - Is simpler better? Can we stick to **GiB** (Integers) for disk size?
    - Yes, GiB only. No fractional GB or MB needed for VM disks.
