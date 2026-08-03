package com.jomlom.nearbycrafting.mixin;

import com.jomlom.nearbycrafting.NearbyCrafting;
import com.jomlom.nearbycrafting.util.NearbyCraftingConfig;
import com.jomlom.recipebookaccess.api.RecipeBookInventoryProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(CraftingMenu.class)
public abstract class CraftingScreenHandlerMixin implements RecipeBookInventoryProvider {

    @Shadow @Final private ContainerLevelAccess access;

    @Shadow @Final private Player player;

    @Override
    public List<Container> getInventoriesForAutofill() {
        if (!NearbyCraftingConfig.craftingTableCanReach) {
            return List.of(player.getInventory());
        }

        List<Container> inventories = new ArrayList<>();

        access.execute((world, pos) -> {
            int radius = NearbyCraftingConfig.craftingTableReach;

            BlockPos.betweenClosedStream(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))
                    .forEach(currentPos -> {
                        if (currentPos.equals(pos)) return;

                        BlockEntity blockEntity = world.getBlockEntity(currentPos);
                        if (blockEntity instanceof Container inventory) {
                            if (NearbyCrafting.isContainerEnabled(blockEntity)) {
                                inventories.add(inventory);
                            }
                        }
                    });

            // Always add player's own inventory
            inventories.add(player.getInventory());
        });

        return inventories;
    }

}
