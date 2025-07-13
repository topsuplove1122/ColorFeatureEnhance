package com.itosfish.colorfeatureenhance

import android.content.Context
import android.content.SharedPreferences
import com.itosfish.colorfeatureenhance.utils.DisclaimerManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * DisclaimerManager 单元测试
 */
class DisclaimerManagerTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    
    @Before
    fun setup() {
        mockContext = mockk()
        mockSharedPreferences = mockk()
        mockEditor = mockk(relaxed = true)
        
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.commit() } returns true
    }
    
    @Test
    fun `shouldShowDisclaimer returns true when disclaimer not accepted`() {
        // Given
        every { mockSharedPreferences.getBoolean("disclaimer_accepted", false) } returns false
        
        // When
        val disclaimerManager = DisclaimerManager.getInstance(mockContext)
        val result = disclaimerManager.shouldShowDisclaimer()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `shouldShowDisclaimer returns false when disclaimer already accepted`() {
        // Given
        every { mockSharedPreferences.getBoolean("disclaimer_accepted", false) } returns true
        
        // When
        val disclaimerManager = DisclaimerManager.getInstance(mockContext)
        val result = disclaimerManager.shouldShowDisclaimer()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `markDisclaimerAccepted saves acceptance state and timestamp`() {
        // Given
        every { mockSharedPreferences.getBoolean("disclaimer_accepted", false) } returns false
        
        // When
        val disclaimerManager = DisclaimerManager.getInstance(mockContext)
        disclaimerManager.markDisclaimerAccepted()
        
        // Then
        verify { mockEditor.putBoolean("disclaimer_accepted", true) }
        verify { mockEditor.putLong("acceptance_time", any()) }
        verify { mockEditor.commit() }
    }
    
    @Test
    fun `resetDisclaimerStatus removes acceptance state`() {
        // Given
        every { mockSharedPreferences.getBoolean("disclaimer_accepted", false) } returns true
        
        // When
        val disclaimerManager = DisclaimerManager.getInstance(mockContext)
        disclaimerManager.resetDisclaimerStatus()
        
        // Then
        verify { mockEditor.remove("disclaimer_accepted") }
        verify { mockEditor.remove("acceptance_time") }
        verify { mockEditor.commit() }
    }
    
    @Test
    fun `getAcceptanceTime returns stored timestamp`() {
        // Given
        val expectedTime = 1234567890L
        every { mockSharedPreferences.getLong("acceptance_time", 0L) } returns expectedTime
        
        // When
        val disclaimerManager = DisclaimerManager.getInstance(mockContext)
        val result = disclaimerManager.getAcceptanceTime()
        
        // Then
        assertEquals(expectedTime, result)
    }
}
