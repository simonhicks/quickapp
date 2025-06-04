package quickapp.runtime.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AppDefTest {
    
    @Test
    fun `adding duplicate screen names throws`() {
        val app = AppDef("test")
        
        app.screen("home")
        
        assertThrows<IllegalArgumentException> {
            app.screen("home")
        }
    }
    
    @Test
    fun `first registered screen is index 0 in screens`() {
        val app = AppDef("test")
        
        app.screen("home")
        app.screen("settings")
        
        assertEquals("home", app.screens[0].name)
        assertEquals("settings", app.screens[1].name)
    }
} 