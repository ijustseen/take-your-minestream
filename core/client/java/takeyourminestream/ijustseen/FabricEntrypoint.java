package takeyourminestream.ijustseen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import takeyourminestream.ijustseen.platform.ClientHooks;
import takeyourminestream.ijustseen.platform.FabricClientHooks;
import takeyourminestream.ijustseen.platform.ModEnvironment;

/** Fabric client entrypoint. */
public class FabricEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricLoader loader = FabricLoader.getInstance();
        ModEnvironment.bootstrap(loader.getGameDir(), loader.getConfigDir(), "fabric");
        ClientHooks.install(new FabricClientHooks());
        ModBootstrap.initClient(TakeYourMineStreamClient.createInstance());
    }
}
