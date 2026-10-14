package dev.KDI.airswap.config;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.KDI.airswap.AirSwapClient;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AirSwapConfigScreen {
    private static final Set<Screen> SCREENS = Collections.newSetFromMap(new WeakHashMap<>());

    static {
        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
            if (SCREENS.contains(screen)) {
                ScreenEvents.remove(screen).register(removed -> AirSwapClient.saveConfig());
            }
        });
    }

    private AirSwapConfigScreen() {
    }

    public static Screen create(Screen parent) {
        AirSwapConfig defaults = new AirSwapConfig();
        Option<Boolean> allSwords = bool("all_swords", defaults.allSwords,
                () -> AirSwapClient.config().allSwords, value -> AirSwapClient.config().allSwords = value);
        Option<String> allowedSwords = Option.<String>createBuilder()
                .name(name("allowed_swords"))
                .description(description("allowed_swords"))
                .binding(String.join(", ", defaults.allowedSwords),
                        () -> String.join(", ", AirSwapClient.config().allowedSwords),
                        value -> AirSwapClient.config().allowedSwords = parseSwordList(value))
                .controller(StringControllerBuilder::create)
                .available(!AirSwapClient.config().allSwords)
                .build();
        allowedSwords.addListener((option, value) -> AirSwapClient.config().allowedSwords = parseSwordList(value));
        allSwords.addListener((option, value) -> allowedSwords.setAvailable(!value));

        Screen screen = YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("config.airswap.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("config.airswap.general"))
                        .tooltip(Text.translatable("config.airswap.warning"))
                        .option(bool("enabled", defaults.enabled,
                                () -> AirSwapClient.config().enabled, value -> AirSwapClient.config().enabled = value))
                        .option(bool("swap_back", defaults.swapBack,
                                () -> AirSwapClient.config().swapBack, value -> AirSwapClient.config().swapBack = value))
                        .option(integer("swap_back_delay", defaults.swapBackDelay, 0, 10,
                                () -> AirSwapClient.config().swapBackDelay, value -> AirSwapClient.config().swapBackDelay = value))
                        .option(bool("players_only", defaults.playersOnly,
                                () -> AirSwapClient.config().playersOnly, value -> AirSwapClient.config().playersOnly = value))
                        .option(decimal("minimum_fall_distance", defaults.minimumFallDistance,
                                () -> AirSwapClient.config().minimumFallDistance,
                                value -> AirSwapClient.config().minimumFallDistance = value))
                        .option(allSwords)
                        .option(allowedSwords)
                        .option(bool("only_hotbar", defaults.onlyHotbar,
                                () -> AirSwapClient.config().onlyHotbar, value -> AirSwapClient.config().onlyHotbar = value))
                        .option(bool("show_swap_message", defaults.showSwapMessage,
                                () -> AirSwapClient.config().showSwapMessage, value -> AirSwapClient.config().showSwapMessage = value))
                        .option(bool("disable_when_sneaking", defaults.disableWhenSneaking,
                                () -> AirSwapClient.config().disableWhenSneaking, value -> AirSwapClient.config().disableWhenSneaking = value))
                        .option(integer("cooldown_ticks", defaults.cooldownTicks, 0, 20,
                                () -> AirSwapClient.config().cooldownTicks, value -> AirSwapClient.config().cooldownTicks = value))
                        .option(ButtonOption.createBuilder()
                                .name(name("reset"))
                                .description(description("reset"))
                                .action((currentScreen, button) -> {
                                    resetDefaults();
                                    AirSwapClient.saveConfig();
                                    MinecraftClient.getInstance().setScreen(create(parent));
                                })
                                .build())
                        .build())
                .save(AirSwapClient::saveConfig)
                .build()
                .generateScreen(parent);
        SCREENS.add(screen);
        return screen;
    }

    private static Option<Boolean> bool(String key, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        Option<Boolean> option = Option.<Boolean>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
        option.addListener((changed, value) -> setter.accept(value));
        return option;
    }

    private static Option<Integer> integer(String key, int defaultValue, int min, int max,
                                            Supplier<Integer> getter, Consumer<Integer> setter) {
        Option<Integer> option = Option.<Integer>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(defaultValue, getter, setter)
                .controller(value -> IntegerSliderControllerBuilder.create(value).range(min, max).step(1))
                .build();
        option.addListener((changed, value) -> setter.accept(value));
        return option;
    }

    private static Option<Double> decimal(String key, double defaultValue, Supplier<Double> getter, Consumer<Double> setter) {
        Option<Double> option = Option.<Double>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(defaultValue, getter, setter)
                .controller(value -> DoubleSliderControllerBuilder.create(value).range(0.0, 10.0).step(0.1))
                .build();
        option.addListener((changed, value) -> setter.accept(value));
        return option;
    }

    private static Text name(String key) {
        return Text.translatable("config.airswap." + key);
    }

    private static OptionDescription description(String key) {
        return OptionDescription.of(Text.translatable("config.airswap." + key + ".tooltip"));
    }

    private static List<String> parseSwordList(String value) {
        List<String> swords = new ArrayList<>();
        for (String entry : value.split(",")) {
            String id = AirSwapConfig.normalizeItemId(entry);
            if (id != null && !swords.contains(id)) {
                swords.add(id);
            }
        }
        return swords;
    }

    private static void resetDefaults() {
        AirSwapConfig defaults = new AirSwapConfig();
        AirSwapConfig config = AirSwapClient.config();
        config.enabled = defaults.enabled;
        config.swapBack = defaults.swapBack;
        config.swapBackDelay = defaults.swapBackDelay;
        config.playersOnly = defaults.playersOnly;
        config.minimumFallDistance = defaults.minimumFallDistance;
        config.allSwords = defaults.allSwords;
        config.allowedSwords = defaults.allowedSwords;
        config.onlyHotbar = defaults.onlyHotbar;
        config.showSwapMessage = defaults.showSwapMessage;
        config.disableWhenSneaking = defaults.disableWhenSneaking;
        config.cooldownTicks = defaults.cooldownTicks;
    }
}