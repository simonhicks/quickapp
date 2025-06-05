package quickapp.plugin

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FilenameCleanerTest {
    
    @Test
    fun `cleans FooBar_kt correctly`() {
        assertEquals("foobar", cleanFilename("FooBar.kt"))
    }
    
    @Test
    fun `cleans foo_bar_kt correctly`() {
        assertEquals("foobar", cleanFilename("foo_bar.kt"))
    }
    
    @Test
    fun `handles filename without extension`() {
        assertEquals("foobar", cleanFilename("foo_bar"))
    }
    
    @Test
    fun `handles complex filenames`() {
        assertEquals("myawesomefile", cleanFilename("My-Awesome_File.txt"))
    }
} 