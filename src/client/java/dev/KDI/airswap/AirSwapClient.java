package dev.KDI.airswap;

import dev.KDI.airswap.config.AirSwapConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AirSwapClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("airswap");
    public static final AirSwapController CONTROLLER = new AirSwapController();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("airswap.json");
    private static AirSwapConfig config = new AirSwapConfig();
    private static long lastModified = Long.MIN_VALUE;
    private static long lastSize = -1;
    private static int reloadTicks;

    @Override
    public void onInitializeClient() {
        if (Files.exists(CONFIG_PATH)) {
            reloadConfig();
        } else {
            saveConfig();
        }
        KeyBinding toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.airswap.toggle", InputUtil.UNKNOWN_KEY.getCode(), KeyBinding.Category.MISC));
        ClientTickEvents.START_CLIENT_TICK.register(CONTROLLER::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++reloadTicks >= 20) {
                reloadTicks = 0;
                reloadIfChanged();
            }
            while (toggle.wasPressed()) {
                if (client.player != null) {
                    config.enabled = !config.enabled;
                    if (!config.enabled) {
                        CONTROLLER.restoreNow(client);
                    }
                    saveConfig();
                    client.player.sendMessage(Text.translatable(config.enabled
                            ? "message.airswap.enabled" : "message.airswap.disabled"), true);
                }
            }
            CONTROLLER.endTick(client);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CONTROLLER.reset();
            reloadConfig();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CONTROLLER.reset());
    }

    public static AirSwapConfig config() {
        return config;
    }

    public static void saveConfig() {
        try {
            config.write(CONFIG_PATH);
            recordFileState();
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Could not save Air Swap configuration", exception);
        }
    }

    private static void reloadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }
        try {
            recordFileState();
            config = AirSwapConfig.read(CONFIG_PATH);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not load airswap.json; keeping the last valid settings (defaults on first load)", exception);
        }
    }

    private static void reloadIfChanged() {
        try {
            if (Files.exists(CONFIG_PATH) && (Files.getLastModifiedTime(CONFIG_PATH).toMillis() != lastModified
                    || Files.size(CONFIG_PATH) != lastSize)) {
                reloadConfig();
            }
        } catch (IOException exception) {
            LOGGER.debug("Could not check airswap.json for changes", exception);
        }
    }

    private static void recordFileState() throws IOException {
        lastModified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
        lastSize = Files.size(CONFIG_PATH);
    }
}