package takeyourminestream.ijustseen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import takeyourminestream.ijustseen.platform.ClientHooks;
import takeyourminestream.ijustseen.platform.ModEnvironment;
import takeyourminestream.ijustseen.platform.NeoForgeClientHooks;

@Mod(value = TakeYourMineStreamClient.FML_MOD_ID, dist = Dist.CLIENT)
public class NeoForgeEntrypoint {
    public NeoForgeEntrypoint(IEventBus modBus) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        ModEnvironment.bootstrap(FMLPaths.GAMEDIR.get(), FMLPaths.CONFIGDIR.get(), "neoforge");
        ClientHooks.install(new NeoForgeClientHooks(modBus));
        ModBootstrap.initClient(TakeYourMineStreamClient.createInstance());
    }
}
