package moze_intel.projecte.manual;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public class ItemPage extends AbstractPage
{
    private final ItemStack stack;

    protected ItemPage(ItemStack stack, PageCategory category)
    {
        super(category);
        this.stack = stack;
    }

    public ItemStack getItemStack()
    {
        return stack.copy();
    }

    @Override
    public String getHeaderText()
    {
        return StatCollector.translateToLocal(stack.getUnlocalizedName() + ".name");
    }

    @Override
    public String getBodyText()
    {
        return StatCollector.translateToLocal("pe.manual." + stack.getUnlocalizedName().substring(5)); // Strip "item." or "tile."
    }
}