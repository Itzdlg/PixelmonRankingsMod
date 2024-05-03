package sh.dominick.commissions.pixelmonrankings.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;

import java.util.List;

public class ItemStackUtil {
    private ItemStackUtil() {}

    public static void writeLore(ItemStack item, ITextComponent... lines) {
        CompoundNBT tag = item.getOrCreateTag();
        CompoundNBT displayTag = tag.getCompound("display");
        ListNBT loreTag = new ListNBT();

        for (ITextComponent line : lines)
            loreTag.add(StringNBT.valueOf(ITextComponent.Serializer.toJson(line)));

        displayTag.put("Lore", loreTag);
        tag.put("display", displayTag);
        item.setTag(tag);
    }

    public static void writeLore(ItemStack item, List<? extends String> lines) {
        ITextComponent[] arr = new ITextComponent[lines.size()];

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.startsWith("{") && line.endsWith("}")) arr[i] = ITextComponent.Serializer.fromJsonLenient(line);
            else arr[i] = new StringTextComponent(line);
        }

        writeLore(item, arr);
    }
}
