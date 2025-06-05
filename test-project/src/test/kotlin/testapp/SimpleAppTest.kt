import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class SimpleAppTest {
    @Test
    fun `test plugin loads and logs message`() {
        val outContent = ByteArrayOutputStream()
        val originalOut = System.out
        
        try {
            System.setOut(PrintStream(outContent))
            
            // Just compile our simple app - this should trigger the plugin
            val output = outContent.toString()
            assertTrue(output.contains("QuickApp plugin loaded"), 
                "Plugin should log its loading message")
            
        } finally {
            System.setOut(originalOut)
        }
    }
} 
