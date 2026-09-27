# Robustness review — 2026-09-23

Reviewed baseline: 0.7.2, commit 933138f. Improvements in the 0.7.3 test build.

## What was already present

- Network operations run on workers, with connection/read timeouts, response-size bounds and connection cleanup.
- Errors reach the UI; expired Flickr credentials produce a reconnect message. Album loading offers a retry button. Thumbnail failures leave a placeholder.
- Images are decoded with subsampling after reading dimensions. The slideshow retains two display layers during transitions and one prefetched image, rather than a complete album of bitmaps.
- Player shutdown removes timers, cancels animation, interrupts workers, disconnects the active image request and releases view references.
- Five JVM suites were already tracked: OAuth/pairing security, Flickr fixtures, sorting, language selection and slideshow timing. They were not wired into CI.

## Concrete findings and changes

- Recovery stopped after three failed attempts and did not recover an initial failure with unknown album size. Temporary failures now retry the same photo at 5, 10, 20, 40 and then 60-second intervals until success, pause or close. HTTP 408/429/5xx, DNS/socket/timeouts/EOF, and Flickr API 105/106 are retryable. Authentication and malformed-image errors are not retried indefinitely.
- Error retries bypassed the slideshow clock. They now use the same scheduler, so pause, manual navigation and close cancel them. The last successfully displayed photo remains in place. Errors stay visible until an action or recovery replaces them.
- The default-order page cache grew with every visited page. It now retains only three pages. Explicit local sorting still loads all photo metadata once; very large sorted albums remain a scaling limitation.
- A remote-control `heldBitmap` field retained the last image selected with OK. It is now cleared after key release, background-dialog handoff, album navigation and stop.
- Obsolete queued album thumbnail requests retained views and delayed newer requests. Their queue is cleared on page changes, opening an album and stop; stale tasks check generation before downloading.
- Decoded slideshow images are capped at the smaller of 8.3 million pixels and max heap / 48. With ARGB_8888, three frames nominally use at most one quarter of the heap. This is a budget heuristic, not a guarantee about total RAM: encoded buffers, UI, Android native/GPU memory and metadata also consume memory.
- Player metadata is owned by its worker. Closing no longer mutates the worker's collections concurrently. In-flight API requests can still retain objects until their network timeout.

## Verification and limits

All six JVM suites pass locally, including recovery with virtual time. The Android application compiles. These are regression tests, not Android rendering, heap profiling, network-stack integration or a claim of comprehensive test coverage. The GitHub Actions workflow is prepared but has not yet run remotely.

We have not measured a sustained memory leak, nor proven its absence. A 6–12 hour failure cannot be inferred from the fact that code was AI-assisted. Android Memory Profiler/heap dumps or repeated memory measurements on a real device are needed. Peak allocations can still cause an out-of-memory failure under severe pressure; there is no claim that such errors are universally caught.

## Device acceptance testing

The owner confirmed on 2026-09-23 that 0.7.3 works, including recovery after unplugging and reconnecting Ethernet. A measured 6–12 hour memory test remains outstanding.

1. Install the signed 0.7.3 test APK over 0.7.2. Confirm login and settings remain.
2. Run a populated album, interrupt the TV's network temporarily, then restore it. The previous image should remain and playback should recover without reopening the album. Also try opening an album with no connection.
3. Pause while retrying; ensure there are no automatic switches. Resume, navigate left/right, and leave the slideshow; no old timer should trigger a switch afterwards.
4. Switch albums/pages quickly and use short/long OK presses. Check backgrounds, focus and thumbnails.
5. Run representative large photos with transitions for 6–12 hours. For actual leak assessment, collect heap/native memory over time and across repeated open/close cycles; look for continuing growth after warm-up rather than a single high reading.

References: https://developer.android.com/topic/performance/graphics/load-bitmap
and https://developer.android.com/topic/performance/issues/bitmap-memory-usage
