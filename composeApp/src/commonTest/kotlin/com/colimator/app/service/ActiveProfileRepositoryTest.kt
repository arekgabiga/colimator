package com.colimator.app.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActiveProfileRepositoryTest {

    @Test
    fun testInitialState() {
        val repo = ActiveProfileRepositoryImpl()
        assertEquals(null, repo.activeProfile.value)
        assertEquals("default", repo.getActiveProfileDisplayName())
        assertFalse(repo.hasProfileChanged())
    }

    @Test
    fun testSetActiveProfile() {
        val repo = ActiveProfileRepositoryImpl()
        
        repo.setActiveProfile("custom-profile")
        
        assertEquals("custom-profile", repo.activeProfile.value)
        assertEquals("custom-profile", repo.getActiveProfileDisplayName())
    }

    @Test
    fun testProfileChangeTracking() {
        val repo = ActiveProfileRepositoryImpl()
        
        // Initial set (from null to null or null to something)
        repo.setActiveProfile("new-profile")
        
        // It changed from null to "new-profile"
        assertTrue(repo.hasProfileChanged(), "Should report change after setting new profile")
        
        // Acknowledge the change
        repo.acknowledgeProfileChange()
        assertFalse(repo.hasProfileChanged(), "Should not report change after acknowledgement")
        
        // Set to same value
        repo.setActiveProfile("new-profile")
        assertFalse(repo.hasProfileChanged(), "Should not report change if value is same (implementation detail: usually it checks value equality)") 
        // Note: The implementation uses `_lastKnownProfile` which is updated on `setActiveProfile`. 
        // Let's verify expectations against the implementation:
        // Implementation:
        // setActiveProfile: _lastKnownProfile = current; _activeProfile = new
        // hasProfileChanged: _activeProfile != _lastKnownProfile
        
        // So:
        // 1. Start: active=null, last=null
        // 2. Set("A"): last=null, active="A". Changed? Yes ("A" != null)
        // 3. Ack(): last="A". (Explicitly sets last = active)
        // 4. Set("A"): last="A", active="A". Changed? No ("A" == "A")
        
        repo.setActiveProfile("another-profile")
        assertTrue(repo.hasProfileChanged())
    }
}
