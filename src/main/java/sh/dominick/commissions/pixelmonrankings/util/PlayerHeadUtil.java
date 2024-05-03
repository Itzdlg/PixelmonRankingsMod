package sh.dominick.commissions.pixelmonrankings.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

import java.util.UUID;

public class PlayerHeadUtil {
    private PlayerHeadUtil() {}

    public static ItemStack getPlayerHead(UUID player, String texture, int amount) {
        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD, amount);
        CompoundNBT tag = playerHead.getOrCreateTag();

        CompoundNBT skullOwnerTag = new CompoundNBT();
        skullOwnerTag.putUUID("Id", player);

        CompoundNBT propertiesTag = new CompoundNBT();
        ListNBT texturesInner = new ListNBT();

        CompoundNBT textureInner = new CompoundNBT();
        textureInner.putString("Value", texture);

        texturesInner.add(0, textureInner);
        propertiesTag.put("textures", texturesInner);

        skullOwnerTag.put("Properties", propertiesTag);
        tag.put("SkullOwner", skullOwnerTag);

        return playerHead;
    }
}
