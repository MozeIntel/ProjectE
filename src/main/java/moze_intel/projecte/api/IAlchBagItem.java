package moze_intel.projecte.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * This interfaces specifies items that perform a specific function every tick when inside an Alchemical Bag, on a player.
 */
public interface IAlchBagItem
{
	/**
	 * Called on both client and server every time the alchemical bag ticks this item.
	 *
	 * @param inv The inventory of the bag
	 * @param player The player whose bag is being ticked.
	 * @param stack The ItemStack being ticked
	 */
	void updateInAlchBag(ItemStack[] inv, EntityPlayer player, ItemStack stack);
}
