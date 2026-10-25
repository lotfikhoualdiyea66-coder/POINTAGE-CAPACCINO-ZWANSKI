# Model catalog

This project intentionally does not enable live facial recognition by default.

## Active configuration
- Provider: OpenCV YuNet (detection) + SFace (embeddings) on the kiosk; matching on the API server.
- Status: implemented as an optional check on top of the employee PIN, **off by default** (dashboard → Vérification faciale).
- Match rule: cosine similarity ≥ `FACE_MATCH_THRESHOLD` (default 0.363, OpenCV's SFace reference) for every capture.
- Liveness: frontal capture → visible head turn (nose position between the eyes must move) → frontal capture; captures must match each other.

## Bundled face prototype
- YuNet `face_detection_yunet_2023mar.onnx`, MIT, OpenCV Zoo commit `26cc381e4d2594bb9f47a26eb8fd96c94a13660d`, SHA-256 `8f2383e4dd3cfbb4553ea8718107fc0423210dc964f9f4280604804ed2552fa4`.
- SFace `face_recognition_sface_2021dec.onnx`, Apache-2.0, OpenCV Zoo commit `25f423d0e04c31a17254620e58febd7386da523b`, SHA-256 `0ba9fbfa01b5270c96627c4ef784da859931e02f04419c829e83484087c34e79`.
- The Android kiosk uses YuNet for detection/landmarks and SFace for embeddings during authorized enrollment and face verification.
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
The model file licenses are documented and their SHA-256 hashes are verified. License permission alone does not establish liveness quality, demographic accuracy, workplace privacy approval, or production readiness. Keep the face check disabled for real staff until those are reviewed and tested on the S22.
