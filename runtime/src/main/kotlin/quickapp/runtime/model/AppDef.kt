package quickapp.runtime.model

class AppDef(val name: String) {
    val screens = mutableListOf<ScreenDef>()
}

fun AppDef.screen(name: String, block: ScreenDef.() -> Unit = {}) {
    if (screens.any { it.name == name }) {
        throw IllegalArgumentException("Screen with name '$name' already exists")
    }
    val screen = ScreenDef(name)
    screen.block()
    screens.add(screen)
} 