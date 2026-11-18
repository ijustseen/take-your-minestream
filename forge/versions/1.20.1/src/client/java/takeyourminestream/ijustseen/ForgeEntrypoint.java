package takeyourminestream.ijustseen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import takeyourminestream.ijustseen.platform.ClientHooks;
import takeyourminestream.ijustseen.platform.ForgeClientHooks;
import takeyourminestream.ijustseen.platform.ModEnvironment;

@Mod(TakeYourMineStreamClient.FML_MOD_ID)
public class ForgeEntrypoint {
    public ForgeEntrypoint() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        ModEnvironment.bootstrap(FMLPaths.GAMEDIR.get(), FMLPaths.CONFIGDIR.get(), "forge");
        ClientHooks.install(new ForgeClientHooks(FMLJavaModLoadingContext.get().getModEventBus()));
        ModBootstrap.initClient(TakeYourMineStreamClient.createInstance());
    }
}
