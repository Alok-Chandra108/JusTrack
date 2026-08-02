# Research Notes - Kotlin Compile Daemon Connection Failure

## Issue Analysis
The error `Daemon compilation failed: Could not connect to Kotlin compile daemon` during `./gradlew :app:kspDebugKotlin` suggests that the Kotlin compiler daemon is failing to start or communication is being blocked/lost.

### Current Configuration
- **Gradle:** 9.6.1
- **AGP:** 8.7.2
- **Kotlin:** 2.0.21
- **KSP:** 2.0.21-1.0.26
- **JDK Home:** Hardcoded to `C:\Program Files\Android\Android Studio\jbr`
- **Memory:** `org.gradle.jvmargs` and `kotlin.daemon.jvmargs` both set to `-Xmx4096m`.

### Potential Causes
1. **Memory Over-allocation:** Allocating 4GB to both the Gradle daemon and the Kotlin daemon (8GB total + OS overhead) may cause the OS to kill the process if the machine doesn't have sufficient RAM.
2. **JDK Path Issue:** The hardcoded `org.gradle.java.home` points to a specific Android Studio directory which might not be accessible or might have version mismatches when running from the command line.
3. **Gradle 9 Compatibility:** Gradle 9.x is very new and might have tighter requirements for daemon communication or might conflict with the `org.gradle.configuration-cache=true` setting when combined with KSP.
4. **Stuck Daemons:** Previous failed builds might have left zombie processes that are preventing new daemons from starting/connecting.

## Proposed Fixes
1. **Optimize `gradle.properties`**:
    - Reduce `-Xmx` values to more standard levels (e.g., 2048m).
    - Comment out `org.gradle.java.home` to allow Gradle to use the environment's JDK (which is usually more stable for CLI).
    - Disable configuration cache temporarily to rule it out.
2. **Stop Daemons**: Run `./gradlew --stop` to clear existing processes.
3. **Increase Timeout**: Add properties to increase the connection timeout if the daemon is just slow to start.
