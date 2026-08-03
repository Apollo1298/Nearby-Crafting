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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

@Mixin(InventoryMenu.class)
public abstract class PlayerScreenHandlerMixin implements RecipeBookInventoryProvider {

    @Shadow @Final private Player owner;

    @Override
    public List<Container> getInventoriesForAutofill() {
        if (!NearbyCraftingConfig.craftingPlayerCanReach) { return List.of(owner.getInventory()); }
        Level world = owner.level();
        BlockPos playerPos = owner.blockPosition();
        List<Container> inventories = new ArrayList<>();
        int radius = NearbyCraftingConfig.craftingPlayerReach;
        BlockPos.betweenClosedStream(playerPos.offset(-radius, -radius, -radius), playerPos.offset(radius, radius, radius))
                .forEach(currentPos -> {
                    BlockEntity blockEntity = world.getBlockEntity(currentPos);
                    if (blockEntity instanceof Container inventory) {
                        if (NearbyCrafting.isContainerEnabled(blockEntity)) {
                            inventories.add(inventory);
                        }
                    }
                });
        inventories.add(owner.getInventory());
        return inventories;
    }
}
