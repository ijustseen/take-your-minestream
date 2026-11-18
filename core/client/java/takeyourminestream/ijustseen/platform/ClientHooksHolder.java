package takeyourminestream.ijustseen.platform;

final class ClientHooksHolder {
    private static ClientHooks instance;

    private ClientHooksHolder() {}

    static void set(ClientHooks hooks) {
        instance = hooks;
    }

    static ClientHooks get() {
        if (instance == null) {
            throw new IllegalStateException("ClientHooks not installed — call ClientHooksHolder.set() from loader entrypoint");
        }
        return instance;
    }
}
