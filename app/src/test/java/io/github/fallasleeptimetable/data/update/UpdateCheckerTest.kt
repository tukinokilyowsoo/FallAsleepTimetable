package io.github.fallasleeptimetable.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun detectsNewerVersion() {
        assertTrue(UpdateChecker.isNewer("v0.7.0-beta", "0.6.1-beta"))
        assertTrue(UpdateChecker.isNewer("0.6.10", "0.6.9"))
        assertTrue(UpdateChecker.isNewer("1.0.0", "0.99.99"))
    }

    @Test
    fun ignoresSameAndOlderVersions() {
        assertFalse(UpdateChecker.isNewer("v0.6.1-beta", "0.6.1-beta"))
        assertFalse(UpdateChecker.isNewer("0.6.0-beta", "0.6.1-beta"))
        assertFalse(UpdateChecker.isNewer("0.6.1-beta", "0.6.1"))
    }

    @Test
    fun comparesPrereleaseNumbers() {
        assertTrue(UpdateChecker.isNewer("1.1.0-beta2", "1.1.0-beta1"))
        assertFalse(UpdateChecker.isNewer("1.1.0-beta1", "1.1.0-beta2"))
        assertFalse(UpdateChecker.isNewer("1.1.0-beta2", "1.1.0-beta2"))
    }

    @Test
    fun stableReleaseIsNewerThanSameCoreBeta() {
        assertTrue(UpdateChecker.isNewer("1.1.0", "1.1.0-beta4"))
        assertTrue(UpdateChecker.isNewer("v1.1.0", "1.1.0-beta1"))
        assertFalse(UpdateChecker.isNewer("1.1.0-beta4", "1.1.0"))
    }
}
