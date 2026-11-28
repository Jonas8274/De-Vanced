/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/googlephotos/misc/gms/GmsCoreSupportPatch.kt
 */
package app.morphe.patches.googlephotos.misc.gms

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.googlephotos.misc.extension.sharedExtensionPatch
import app.morphe.patches.googlephotos.misc.gms.Constants.MORPHE_PHOTOS_PACKAGE_NAME
import app.morphe.patches.googlephotos.misc.gms.Constants.PHOTOS_PACKAGE_NAME
import app.morphe.patches.googlephotos.misc.login.selectedAccountPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatch
import app.morphe.util.returnEarly
import org.w3c.dom.Element

// Must be declared above gmsCoreSupportPatch, because top-level values initialize in file order.
// Disables Google's "This device isn't Play Protect certified" screen, which shows up in the
// patched app because the patched package isn't recognized as certified.
private val disableUncertifiedDeviceActivityPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val activities = document.getElementsByTagName("activity")
            for (i in 0 until activities.length) {
                val activity = activities.item(i) as Element
                if (activity.getAttribute("android:name").endsWith("UncertifiedDeviceActivity")) {
                    activity.setAttribute("android:enabled", "false")
                }
            }
        }
    }
}

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatch(
    fromPackageName = PHOTOS_PACKAGE_NAME,
    toPackageName = MORPHE_PHOTOS_PACKAGE_NAME,
    mainActivityOnCreateFingerprint = HomeActivityOnCreateFingerprint,
    extensionPatch = sharedExtensionPatch,
    gmsCoreSupportResourcePatchFactory = ::gmsCoreSupportResourcePatch,
    executeBlock = {
        // Photos' bundled Google Play Services availability check rejects GmsCore's signature.
        // Returning SUCCESS keeps account/profile initialization and Maps-backed views usable.
        IsGooglePlayServicesAvailableFingerprint.methodOrNull?.returnEarly(0)
    },
) {
    dependsOn(selectedAccountPatch, disableUncertifiedDeviceActivityPatch)
    compatibleWith(AppCompatibilities.GOOGLE_PHOTOS)
}

private fun gmsCoreSupportResourcePatch(appPermissionReplacements: MutableMap<String, String>) =
    app.morphe.patches.shared.misc.gms.gmsCoreSupportResourcePatch(
        fromPackageName = PHOTOS_PACKAGE_NAME,
        toPackageName = MORPHE_PHOTOS_PACKAGE_NAME,
        spoofedPackageSignature = "24bb24c05e47e0aefa68a58a766179d9b613a600",
        appPermissionReplacements = appPermissionReplacements,
    )
