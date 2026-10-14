package dev.KDI.airswap.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

public final class AirSwapModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            return parent -> null;
        }
        return parent -> YaclBridge.create(parent);
    }

    private static final class YaclBridge {
        private static Screen create(Screen parent) {
            return AirSwapConfigScreen.create(parent);
        }
    }
}