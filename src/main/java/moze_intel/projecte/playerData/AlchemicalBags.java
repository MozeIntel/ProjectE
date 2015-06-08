package moze_intel.projecte.playerData;

import com.google.common.collect.Maps;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.ClientSyncBagDataPKT;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.Map;
import java.util.Map.Entry;

public final class AlchemicalBags 
{
	@Deprecated
	private static Map<String, Map<Byte, ItemStack[]>> MAP = Maps.newLinkedHashMap();

	public static ItemStack[] get(EntityPlayer player, byte color)
	{
		return PEAlchBags.getDataFor(player).getInv(color);
	}

	public static void set(EntityPlayer player, byte color, ItemStack[] inv)
	{
		PEAlchBags.getDataFor(player).setInv(color, inv);
	}

	public static void sync(EntityPlayer player)
	{
		NBTTagCompound tag = new NBTTagCompound();
		PEAlchBags.getDataFor(player).saveNBTData(tag);
		PacketHandler.sendTo(new ClientSyncBagDataPKT(tag), (EntityPlayerMP) player);
	}

	/**
	 * @return NBTTagList of inventories for use in the new saving system
	 */
	public static NBTTagList migratePlayerData(EntityPlayer player)
	{
		Map<Byte, ItemStack[]> data = MAP.get(player.getCommandSenderName());

		NBTTagList list = new NBTTagList();

		for (Entry<Byte, ItemStack[]> entry : data.entrySet())
		{
			NBTTagCompound subNbt = new NBTTagCompound();
			subNbt.setByte("color", entry.getKey());

			NBTTagList subList = new NBTTagList();

			ItemStack[] inv = entry.getValue();

			for (int i = 0; i < inv.length; i++)
			{
				ItemStack stack = inv[i];

				if (stack != null)
				{
					NBTTagCompound subNbt2 = new NBTTagCompound();
					subNbt2.setByte("index", (byte) i);
					stack.writeToNBT(subNbt2);
					subList.appendTag(subNbt2);
				}
			}

			subNbt.setTag("inv", subList);
			list.appendTag(subNbt);
		}
		return list;
	}

	@Deprecated
	public static void legacySet(String player, byte bagColour, ItemStack[] inv)
	{
		Map<Byte, ItemStack[]> setdata;

		if (MAP.containsKey(player))
		{
			setdata = MAP.get(player);
		}
		else
		{
			setdata = Maps.newLinkedHashMap();
			MAP.put(player, setdata);
		}
		if (setdata == null)
		{
			setdata = Maps.newLinkedHashMap();
			MAP.put(player, setdata);
		}
		setdata.put(bagColour, inv);
	}
}
