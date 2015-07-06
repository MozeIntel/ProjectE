package moze_intel.projecte.manual;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public abstract class AbstractPage
{
	protected final PageCategory category;

	protected AbstractPage(PageCategory category)
	{
		this.category = category;
	}

	public boolean shouldAppearInIndex()
	{
		return true;
	}

	public abstract String getBodyText();
	
	public abstract String getHeaderText();

	public static AbstractPage createItemPage(Item item, PageCategory category)
	{
		return new ItemPage(new ItemStack(item), category);
	}

	public static AbstractPage createItemPage(Block block, PageCategory category)
	{
		return new ItemPage(new ItemStack(block), category);
	}

	public static AbstractPage createItemPage(ItemStack stack, PageCategory category)
	{
		return new ItemPage(stack.copy(), category);
	}

	public static AbstractPage createTextPage(String identifier, PageCategory category)
	{
		return new TextPage(identifier, category);
	}

	public static AbstractPage createImagePage(String header, ResourceLocation imageLocation, PageCategory category)
	{
		return new ImagePage(header, imageLocation, category);
	}
}