module uk.co.bithatch.snake.lib.backend.openrazer {
    requires transitive uk.co.bithatch.snake.lib;
	requires transitive dbus.java;
	requires transitive jnr.ffi; 
	provides uk.co.bithatch.snake.lib.Backend with uk.co.bithatch.snake.lib.backend.openrazer.OpenRazerBackend;
}