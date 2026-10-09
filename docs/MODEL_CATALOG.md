# Model catalog

This project intentionally does not enable live facial recognition by default.

## Active configuration
- Provider: `demo_disabled`
- Status: disabled
- Reason: no verified commercial license and no device-backed live model path has been approved for this environment.

## Required documentation if a licensed provider is later approved
- Model name
- Version
- Source URL
- Checksum
- License terms
- Commercial-use permission statement for internal workplace use
- Local execution environment and storage approach
- Deletion/retention controls

## Risk note
The repository is kept safe by refusing to bundle or download recognition weights without documented rights. All biometric flows remain behind the provider interface and are marked unavailable until approved.
