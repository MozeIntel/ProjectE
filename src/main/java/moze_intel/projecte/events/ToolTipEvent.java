package moze_intel.projecte.events;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moze_intel.projecte.api.IPedestalItem;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.ObjHandler;
import moze_intel.projecte.gameObjs.gui.GUIPedestal;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.oredict.OreDictionary;

@SideOnly(Side.CLIENT)
public class ToolTipEvent 
{
	@SubscribeEvent
	public void tTipEvent(ItemTooltipEvent event)
	{
		ItemStack current = event.itemStack;
		Item currentItem = current.getItem();
		Block currentBlock = Block.getBlockFromItem(currentItem);

		if (current == null)
		{
			return;
		}

		if (ProjectEConfig.showPedestalTooltip
			&& currentItem instanceof IPedestalItem)
		{
			if (ProjectEConfig.showPedestalTooltipInGUI)
			{
				if (Minecraft.getMinecraft().currentScreen instanceof GUIPedestal)
				{
					event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "On Pedestal:");
					event.toolTip.addAll(((IPedestalItem) currentItem).getPedestalDescription());
				}
			}
			else
			{
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "On Pedestal:");
				event.toolTip.addAll(((IPedestalItem) currentItem).getPedestalDescription());
			}
			
		}

		if (ProjectEConfig.showUnlocalizedNames)
		{
			event.toolTip.add("UN: " + Item.itemRegistry.getNameForObject(current.getItem()));
		}
		
		if (ProjectEConfig.showODNames)
		{
			for (int id : OreDictionary.getOreIDs(current))
			{
				event.toolTip.add("OD: " + OreDictionary.getOreName(id));
			}
		}

		if (ProjectEConfig.showEMCTooltip)
		{
			if (Utils.doesItemHaveEmc(current))
			{
				int value = Utils.getEmcValue(current);

				event.toolTip.add(EnumChatFormatting.YELLOW + "EMC: " + EnumChatFormatting.WHITE + String.format("%,d", value));

				if (current.stackSize > 1)
				{
					long total = value * current.stackSize;

					if (total < 0 || total <= value || total > Integer.MAX_VALUE)
					{
						event.toolTip.add(EnumChatFormatting.YELLOW + "Stack EMC: " + EnumChatFormatting.WHITE + EnumChatFormatting.OBFUSCATED + "WAY TOO MUCH");
					}
					else
					{
						event.toolTip.add(EnumChatFormatting.YELLOW + "Stack EMC: " + EnumChatFormatting.WHITE + String.format("%,d", value * current.stackSize));
					}
				}
			}
		}

		if (ProjectEConfig.showStatTooltip)
		{
			/**
			 * Collector ToolTips
			 */
			if (currentBlock == ObjHandler.energyCollector)
			{
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Generation Rate: " + EnumChatFormatting.BLUE + Constants.COLLECTOR_MK1_GEN + " EMC/s");
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Storage: " + EnumChatFormatting.BLUE + Constants.COLLECTOR_MK1_MAX + " EMC");
			}

			if (currentBlock == ObjHandler.collectorMK2)
			{
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Generation Rate: " + EnumChatFormatting.BLUE + Constants.COLLECTOR_MK2_GEN + " EMC/s");
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Storage: " + EnumChatFormatting.BLUE + Constants.COLLECTOR_MK2_MAX + " EMC");
			}

			if (currentBlock == ObjHandler.collectorMK3)
			{
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Generation Rate: " + EnumChatFormatting.BLUE + Constants.COLLECTOR_MK3_GEN + " EMC/s");
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Storage: " + EnumChatFormatting.BLUE + Constants.COLLECTOR_MK3_MAX + " EMC");
			}

			/**
			 * Relay ToolTips
			 */
			if (currentBlock == ObjHandler.relay)
			{
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Output Rate: " + EnumChatFormatting.BLUE + Constants.RELAY_MK1_OUTPUT + " EMC/s");
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Storage: " + EnumChatFormatting.BLUE + Constants.RELAY_MK1_MAX + " EMC");
			}

			if (currentBlock == ObjHandler.relayMK2)
			{
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Output Rate: " + EnumChatFormatting.BLUE + Constants.RELAY_MK2_OUTPUT + " EMC/s");
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Storage: " + EnumChatFormatting.BLUE + Constants.RELAY_MK2_MAX + " EMC");
			}

			if (currentBlock == ObjHandler.relayMK3)
			{
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Output Rate: " + EnumChatFormatting.BLUE + Constants.RELAY_MK3_OUTPUT + " EMC/s");
				event.toolTip.add(EnumChatFormatting.DARK_PURPLE + "Max Storage: " + EnumChatFormatting.BLUE + Constants.RELAY_MK3_MAX + " EMC");
			}
		}

		if (current.hasTagCompound())
		{
			if (current.stackTagCompound.getBoolean("ProjectEBlock"))
			{
				event.toolTip.add(EnumChatFormatting.GREEN + "Wrenched block!");
				
				if (current.stackTagCompound.getDouble("EMC") > 0)
				{
					event.toolTip.add(EnumChatFormatting.YELLOW + "Stored EMC: " + EnumChatFormatting.WHITE + String.format("%,d", (int) current.stackTagCompound.getDouble("EMC")));
				}
			}
			
			if (current.stackTagCompound.hasKey("StoredEMC"))
			{
				event.toolTip.add(EnumChatFormatting.YELLOW + "Stored EMC: " + EnumChatFormatting.WHITE + String.format("%,d", (int) current.stackTagCompound.getDouble("StoredEMC")));
			}
			else if (current.stackTagCompound.hasKey("StoredXP"))
			{
				event.toolTip.add(EnumChatFormatting.DARK_GREEN + "Stored XP: " + EnumChatFormatting.GREEN + String.format("%,d", current.stackTagCompound.getInteger("StoredXP")));
			}
		}
	}
}
