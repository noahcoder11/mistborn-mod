package com.not_noah.mistborn_metal_arts.client.screen;

import com.not_noah.mistborn_metal_arts.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class MetalArtsMachineMenu extends AbstractContainerMenu {
    private final IItemHandler inventory;
    private final ContainerLevelAccess access;

    public MetalArtsMachineMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, new ItemStackHandler(3), ContainerLevelAccess.NULL);
    }

    public MetalArtsMachineMenu(int id, Inventory playerInventory, IItemHandler inventory, ContainerLevelAccess access) {
        super(ModMenus.METAL_ARTS_MACHINE.get(), id);
        this.inventory = inventory;
        this.access = access;

        this.addSlot(new SlotItemHandler(inventory, 0, 56, 17)); // Input 1
        this.addSlot(new SlotItemHandler(inventory, 1, 56, 53)); // Input 2
        this.addSlot(new SlotItemHandler(inventory, 2, 116, 35)); // Output

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 3) {
                if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
