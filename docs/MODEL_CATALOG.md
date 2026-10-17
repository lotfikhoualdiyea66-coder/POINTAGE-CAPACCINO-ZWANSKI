# Model catalog

This project intentionally does not enable live facial recognition by default.

## Active configuration
- Provider: `demo_disabled`
- Status: disabled
- Reason: employee matching and enrollment remain disabled until liveness, privacy, and physical-device validation are complete.

## Bundled face prototype
- YuNet `face_detection_yunet_2023mar.onnx`, MIT, OpenCV Zoo commit `26cc381e4d2594bb9f47a26eb8fd96c94a13660d`, SHA-256 `8f2383e4dd3cfbb4553ea8718107fc0423210dc964f9f4280604804ed2552fa4`.
- SFace `face_recognition_sface_2021dec.onnx`, Apache-2.0, OpenCV Zoo commit `25f423d0e04c31a17254620e58febd7386da523b`, SHA-256 `0ba9fbfa01b5270c96627c4ef784da859931e02f04419c829e83484087c34e79`.
- The current Android prototype only runs YuNet face detection for camera diagnostics. It does not enroll or identify employees, create attendance events, or implement liveness detection.
- Face frames are processed in memory and are not stored by the diagnostic screen.

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
The model file licenses are documented and their SHA-256 hashes are verified. License permission alone does not establish liveness quality, demographic accuracy, workplace privacy approval, or production readiness. Keep employee biometric enrollment disabled until those are reviewed and tested.
