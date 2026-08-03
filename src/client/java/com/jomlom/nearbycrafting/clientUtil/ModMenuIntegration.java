package com.jomlom.nearbycrafting.clientUtil;

import com.jomlom.nearbycrafting.util.NearbyCraftingConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                    .title(Component.nullToEmpty("Nearby Crafting Configuration"));

            // Base Category
            builder.category(
                    ConfigCategory.createBuilder()
                            .name(Component.nullToEmpty("Nearby Crafting Configuration"))
                            .group(
                                    OptionGroup.createBuilder()
                                            .name(Component.nullToEmpty("Crafting Table"))
                                            .option(Option.<Boolean>createBuilder()
                                                    .name(Component.nullToEmpty("Enabled"))
                                                    .description(OptionDescription.of(Component.nullToEmpty("Allows crafting tables to reach nearby item containers and use their contents for crafting.")))
                                                    .binding(
                                                            true,
                                                            () -> NearbyCraftingConfig.craftingTableCanReach,
                                                            newVal -> {
                                                                NearbyCraftingConfig.craftingTableCanReach = newVal;
                                                                NearbyCraftingConfig.HANDLER.save();
                                                            }
                                                    )
                                                    .controller(opt -> BooleanControllerBuilder.create(opt)
                                                            .formatValue(val -> Component.literal(val ? "True" : "False"))
                                                            .coloured(true))
                                                    .build())
                                            .option(Option.<Integer>createBuilder()
                                                    .name(Component.nullToEmpty("Reach Radius"))
                                                    .description(OptionDescription.of(Component.nullToEmpty("Radius (in blocks) which crafting tables can reach item containers.")))
                                                    .binding(
                                                            NearbyCraftingConfig.defaultReach,
                                                            () -> NearbyCraftingConfig.craftingTableReach,
                                                            newVal -> {
                                                                NearbyCraftingConfig.craftingTableReach = newVal;
                                                                NearbyCraftingConfig.HANDLER.save();
                                                            }
                                                    )
                                                    .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                            .range(0, 50)
                                                            .step(1)
                                                            .formatValue(val -> Component.literal(val + " blocks")))
                                                    .build())
                                            .build()
                            )
                            .group(
                                    OptionGroup.createBuilder()
                                            .name(Component.nullToEmpty("Player Inventory Crafting"))
                                            .option(Option.<Boolean>createBuilder()
                                                    .name(Component.nullToEmpty("Enabled"))
                                                    .description(OptionDescription.of(Component.nullToEmpty("Allows players to reach nearby item containers and use their contents for crafting.")))
                                                    .binding(
                                                            true,
                                                            () -> NearbyCraftingConfig.craftingPlayerCanReach,
                                                            newVal -> {
                                                                NearbyCraftingConfig.craftingPlayerCanReach = newVal;
                                                                NearbyCraftingConfig.HANDLER.save();
                                                            }
                                                    )
                                                    .controller(opt -> BooleanControllerBuilder.create(opt)
                                                            .formatValue(val -> Component.literal(val ? "True" : "False"))
                                                            .coloured(true))
                                                    .build())
                                            .option(Option.<Integer>createBuilder()
                                                    .name(Component.nullToEmpty("Reach Radius"))
                                                    .description(OptionDescription.of(Component.nullToEmpty("Radius (in blocks) which players can reach item containers.")))
                                                    .binding(
                                                            NearbyCraftingConfig.defaultReach,
                                                            () -> NearbyCraftingConfig.craftingPlayerReach,
                                                            newVal -> {
                                                                NearbyCraftingConfig.craftingPlayerReach = newVal;
                                                                NearbyCraftingConfig.HANDLER.save();
                                                            }
                                                    )
                                                    .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                            .range(0, 50)
                                                            .step(1)
                                                            .formatValue(val -> Component.literal(val + " blocks")))
                                                    .build())
                                            .build()
                            )
                            .build()
            );

            // Dynamic Block Toggles by Namespace
            TreeMap<String, Map<String, Boolean>> sortedToggles = new TreeMap<>(NearbyCraftingConfig.containerBlockToggles);

            for (Map.Entry<String, Map<String, Boolean>> namespaceEntry : sortedToggles.entrySet()) {
                String namespace = namespaceEntry.getKey();
                Map<String, Boolean> blocks = new TreeMap<>(namespaceEntry.getValue());

                OptionGroup.Builder groupBuilder = OptionGroup.createBuilder()
                        .name(Component.nullToEmpty("Enabled Blocks"));

                groupBuilder.option(
                        Option.<Boolean>createBuilder()
                                .name(Component.nullToEmpty("Info"))
                                .description(OptionDescription.of(Component.nullToEmpty("These are all of the detected blocks with inventories in this namespace. Enable/disable them to control Nearby Crafting access.")))
                                .binding(false, () -> false, val -> {})
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .formatValue(val -> Component.nullToEmpty(""))
                                        .coloured(false))
                                .build()
                );

                for (Map.Entry<String, Boolean> blockEntry : blocks.entrySet()) {
                    String blockId = blockEntry.getKey();
                    Boolean enabled = blockEntry.getValue();

                    groupBuilder.option(
                            Option.<Boolean>createBuilder()
                                    .name(Component.nullToEmpty(blockId))
                                    .description(OptionDescription.of(Component.nullToEmpty("Determines whether Nearby Crafting can access this block’s inventory during crafting.")))
                                    .binding(
                                            true,
                                            () -> NearbyCraftingConfig.containerBlockToggles
                                                    .getOrDefault(namespace, Map.of())
                                                    .getOrDefault(blockId, true),
                                            newVal -> {
                                                NearbyCraftingConfig.containerBlockToggles
                                                        .computeIfAbsent(namespace, x -> new TreeMap<>())
                                                        .put(blockId, newVal);
                                                NearbyCraftingConfig.HANDLER.save();
                                            }
                                    )
                                    .controller(opt -> BooleanControllerBuilder.create(opt)
                                            .formatValue(val -> Component.literal(val ? "True" : "False"))
                                            .coloured(true))
                                    .build()
                    );
                }

                builder.category(
                        ConfigCategory.createBuilder()
                                .name(Component.nullToEmpty("Blocks: " + namespace))
                                .group(groupBuilder.build())
                                .build()
                );
            }

            return builder.build().generateScreen(parent);
        };
    }
}
