package com.jomlom.nearbycrafting;

import com.jomlom.nearbycrafting.util.NearbyCraftingConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collections;
import java.util.HashMap;


public class NearbyCrafting implements ModInitializer {

	public static final String MOD_ID = "nearbycrafting";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


	@Override
	public void onInitialize() {
		NearbyCraftingConfig.HANDLER.load();
		InitializeCommands();
	}

	private void InitializeCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("nearbycrafting")

					// Operator permission
					.requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))

					// Crafting Table subcommand
					.then(Commands.literal("craftingTable")
							.then(Commands.literal("enable")
									.executes(context -> setCraftingTableEnabled(context, true)))
							.then(Commands.literal("disable")
									.executes(context -> setCraftingTableEnabled(context, false)))
							.then(Commands.literal("setReach")
									.then(Commands.argument("radius", IntegerArgumentType.integer(0, 50))
											.executes(context -> setCraftingTableReach(context, IntegerArgumentType.getInteger(context, "radius")))))
							.then(Commands.literal("getReach")
									.executes(NearbyCrafting::getCraftingTableReach))
					)

					// Player Inventory Crafting subcommand
					.then(Commands.literal("playerInventoryCrafting")
							.then(Commands.literal("enable")
									.executes(context -> setPlayerInventoryEnabled(context, true)))
							.then(Commands.literal("disable")
									.executes(context -> setPlayerInventoryEnabled(context, false)))
							.then(Commands.literal("setReach")
									.then(Commands.argument("radius", IntegerArgumentType.integer(0, 50))
											.executes(context -> setPlayerInventoryReach(context, IntegerArgumentType.getInteger(context, "radius")))))
							.then(Commands.literal("getReach")
									.executes(NearbyCrafting::getPlayerInventoryReach))
					)

					// CONTAINERS subcommand for toggling blocks accessibility individually
					.then(Commands.literal("CONTAINERS")

							.then(Commands.literal("enable")
									.then(Commands.argument("block", IdentifierArgument.id())
											.suggests(ContainerBlockSuggestionProvider.SUGGEST_CONTAINER_BLOCKS)
											.executes(context -> setContainerBlockEnabled(context, true))))

							.then(Commands.literal("disable")
									.then(Commands.argument("block", IdentifierArgument.id())
											.suggests(ContainerBlockSuggestionProvider.SUGGEST_CONTAINER_BLOCKS)
											.executes(context -> setContainerBlockEnabled(context, false))))

							.then(Commands.literal("get")
									.then(Commands.argument("block", IdentifierArgument.id())
											.suggests(ContainerBlockSuggestionProvider.SUGGEST_CONTAINER_BLOCKS)
											.executes(this::getContainerBlockStatus)))

							.then(Commands.literal("list")
									.executes(this::listContainerBlocks))
					)
			);
		});
	}

	public static class ContainerBlockSuggestionProvider {
		public static final SuggestionProvider<CommandSourceStack> SUGGEST_CONTAINER_BLOCKS = (context, builder) -> {
			for (Identifier blockId : BuiltInRegistries.BLOCK.keySet()) {
				if (NearbyCraftingConfig.containerBlockToggles
						.getOrDefault(blockId.getNamespace(), Collections.emptyMap())
						.containsKey(blockId.toString())) {
					builder.suggest(blockId.toString());
				}
			}
			return builder.buildFuture();
		};
	}

	private int setContainerBlockEnabled(CommandContext<CommandSourceStack> context, boolean enabled) throws CommandSyntaxException {
		Identifier blockId = IdentifierArgument.getId(context, "block");

		setBlockEnabled(blockId, enabled);

		context.getSource().sendSuccess(() ->
				Component.literal("Container block " + blockId + " set to " + (enabled ? "enabled" : "disabled")), true);
		return 1;
	}

	private int listContainerBlocks(CommandContext<CommandSourceStack> context) {
		StringBuilder sb = new StringBuilder("Container blocks and their enabled states:\n");

		NearbyCraftingConfig.containerBlockToggles.forEach((namespace, map) -> {
			map.forEach((blockId, enabled) -> {
				sb.append(blockId).append(" : ").append(enabled ? "Enabled" : "Disabled").append("\n");
			});
		});

		context.getSource().sendSuccess(() ->
				Component.literal(sb.toString()), false);
		return 1;
	}

	public static boolean isContainerEnabled(BlockEntity blockEntity) {
		Identifier blockId = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
		String namespace = blockId.getNamespace();
		String id = blockId.toString();

		var namespaceToggles = NearbyCraftingConfig.containerBlockToggles
				.computeIfAbsent(namespace, ignored -> new HashMap<>());
		boolean discovered = !namespaceToggles.containsKey(id);
		namespaceToggles.putIfAbsent(id, true);
		if (discovered) {
			NearbyCraftingConfig.HANDLER.save();
		}

		return namespaceToggles.get(id);
	}

	private int getContainerBlockStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Identifier blockId = IdentifierArgument.getId(context, "block");
		String ns = blockId.getNamespace();
		String id = blockId.toString();

		boolean enabled = NearbyCraftingConfig.containerBlockToggles
				.getOrDefault(ns, Collections.emptyMap())
				.getOrDefault(id, true);

		context.getSource().sendSuccess(() ->
				Component.literal("Container block " + blockId + " is " + (enabled ? "enabled" : "disabled")), false);

		return 1;
	}

	public void setBlockEnabled(Identifier blockId, boolean enabled) {
		String ns = blockId.getNamespace();
		String id = blockId.toString();
		NearbyCraftingConfig.containerBlockToggles
				.computeIfAbsent(ns, x -> new HashMap<>())
				.put(id, enabled);
		NearbyCraftingConfig.HANDLER.save();
	}

	private static int setCraftingTableEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
		NearbyCraftingConfig.craftingTableCanReach = enabled;
		NearbyCraftingConfig.HANDLER.save();
		context.getSource().sendSuccess(() ->
				Component.nullToEmpty("Crafting Table reach enabled: " + enabled), true);
		return 1;
	}

	private static int setCraftingTableReach(CommandContext<CommandSourceStack> context, int radius) {
		NearbyCraftingConfig.craftingTableReach = radius;
		NearbyCraftingConfig.HANDLER.save();
		context.getSource().sendSuccess(() ->
				Component.nullToEmpty("Crafting Table reach radius set to: " + radius), true);
		return 1;
	}

	private static int getCraftingTableReach(CommandContext<CommandSourceStack> context) {
		int radius = NearbyCraftingConfig.craftingTableReach;
		context.getSource().sendSuccess(() ->
				Component.nullToEmpty("Crafting Table reach radius: " + radius), false);
		return 1;
	}

	private static int setPlayerInventoryEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
		NearbyCraftingConfig.craftingPlayerCanReach = enabled;
		NearbyCraftingConfig.HANDLER.save();
		context.getSource().sendSuccess(() ->
				Component.nullToEmpty("Player Inventory Crafting reach enabled: " + enabled), true);
		return 1;
	}

	private static int setPlayerInventoryReach(CommandContext<CommandSourceStack> context, int radius) {
		NearbyCraftingConfig.craftingPlayerReach = radius;
		NearbyCraftingConfig.HANDLER.save();
		context.getSource().sendSuccess(() ->
				Component.nullToEmpty("Player Inventory Crafting reach radius set to: " + radius), true);
		return 1;
	}

	private static int getPlayerInventoryReach(CommandContext<CommandSourceStack> context) {
		int radius = NearbyCraftingConfig.craftingPlayerReach;
		context.getSource().sendSuccess(() ->
				Component.nullToEmpty("Player Inventory Crafting reach radius: " + radius), false);
		return 1;
	}
}