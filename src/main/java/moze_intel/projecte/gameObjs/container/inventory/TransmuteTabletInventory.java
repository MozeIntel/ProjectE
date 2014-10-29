package moze_intel.projecte.gameObjs.container.inventory;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.gameObjs.ObjHandler;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.ClientSyncTableEMCPKT;
import moze_intel.projecte.playerData.Transmutation;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.Comparators;
import moze_intel.projecte.utils.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

public class TransmuteTabletInventory implements IInventory
{
	public double emc;
	private EntityPlayer player = null;
	private static final int LOCK_INDEX = 8;
	private static final int[] MATTER_INDEXES = new int[] {12, 11, 13, 10, 14, 21, 15, 20, 16, 19, 17, 18};
	private static final int[] FUEL_INDEXES = new int[] {22, 23, 24, 25};
	private ItemStack[] inventory = new ItemStack[26];
	public int learnFlag = 0;
	public String filter = "";
	
	public TransmuteTabletInventory(ItemStack stack, EntityPlayer player)
	{
		this.player = player;
		
		if (!stack.hasTagCompound())
		{
			stack.setTagCompound(new NBTTagCompound());
		}
		
		readFromNBT(stack.stackTagCompound);
	}
	
	public void handleKnowledge(ItemStack stack)
	{
		if (stack.stackSize > 1)
		{
			stack.stackSize = 1;
		}
		
		if (stack.hasTagCompound())
		{
			stack.stackTagCompound = null;
		}
		
		if (!stack.getHasSubtypes() && stack.getMaxDamage() != 0 && stack.getItemDamage() != 0)
		{
			stack.setItemDamage(0);
		}
		
		if (!hasKnowledge(stack) && !Transmutation.hasFullKnowledge(player.getCommandSenderName()))
		{
			learnFlag = 300;
			
			if (stack.getItem() == ObjHandler.tome)
			{
				Transmutation.setAllKnowledge(player.getCommandSenderName());
			}
			else
			{
				Transmutation.addToKnowledge(player.getCommandSenderName(), stack);
			}
			
			if (!player.worldObj.isRemote)
			{
				Transmutation.sync(player);
			}
		}
		
		updateOutputs();
	}
	
	public void checkForUpdates()
	{
		int matterEmc = Utils.getEmcValue(inventory[MATTER_INDEXES[0]]);
		int fuelEmc = Utils.getEmcValue(inventory[FUEL_INDEXES[0]]);
		
		int maxEmc = matterEmc > fuelEmc ? matterEmc : fuelEmc;
		
		if (maxEmc > emc)
		{
			updateOutputs();
		}
	}
	
	public void updateOutputs()
	{
		LinkedList<ItemStack> knowledge = (LinkedList<ItemStack>) Transmutation.getKnowledge(player.getCommandSenderName()).clone();
		
		for (int i : MATTER_INDEXES)
		{
			inventory[i] = null;
		}
		
		for (int i : FUEL_INDEXES)
		{
			inventory[i] = null;
		}
		
		ItemStack lock = inventory[LOCK_INDEX];
		
		if (lock != null)
		{
			int reqEmc = Utils.getEmcValue(lock);
			
			if (this.emc < reqEmc)
			{
				return;
			}
			
			Iterator<ItemStack> iter = knowledge.iterator();
			
			while (iter.hasNext())
			{
				ItemStack stack = iter.next();
				
				if (Utils.getEmcValue(stack) > reqEmc)
				{
					iter.remove();
					continue;
				}
				
				if (filter.length() > 0 && !stack.getDisplayName().toLowerCase().contains(filter))
				{
					iter.remove();
					continue;
				}
			}
		}
		else
		{
			Iterator<ItemStack> iter = knowledge.iterator();
			
			while (iter.hasNext())
			{
				ItemStack stack = iter.next();
				
				if (emc < Utils.getEmcValue(stack))
				{
					iter.remove();
					continue;
				}
				
				if (filter.length() > 0 && !stack.getDisplayName().toLowerCase().contains(filter))
				{
					iter.remove();
					continue;
				}
			}
		}
		
		Collections.sort(knowledge, Comparators.ITEMSTACK_DESCENDING);
		
		int matterCounter = 0;
		int fuelCounter = 0;
		
		for (ItemStack stack : knowledge)
		{
			if (FuelMapper.isStackFuel(stack))
			{
				if (fuelCounter < 4)
				{
					inventory[FUEL_INDEXES[fuelCounter]] = stack;
				
					fuelCounter++;
				}
			}
			else
			{
				if (matterCounter < 12)
				{
					inventory[MATTER_INDEXES[matterCounter]] = stack;
					
					matterCounter++;
 				}
			}
		}
	}
	
	private boolean hasKnowledge(ItemStack stack)
	{
		for (ItemStack s : Transmutation.getKnowledge(player.getCommandSenderName()))
		{
			if (s == null)
			{
				continue;
			}
			
			if (stack.getItem().equals(s.getItem()) && stack.getItemDamage() == s.getItemDamage())
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void setPlayer(EntityPlayer player)
	{
		this.player = player;
	}
	
	
	public void readFromNBT(NBTTagCompound nbt)
	{
		NBTTagList list = nbt.getTagList("Items", NBT.TAG_COMPOUND);
		
		for (int i = 0; i < list.tagCount(); i++)
		{
			NBTTagCompound tag = list.getCompoundTagAt(i);
			
			ItemStack stack = ItemStack.loadItemStackFromNBT(tag);
			
			inventory[tag.getByte("slot")] = stack;
		}
	}
	
	public void writeToNBT(NBTTagCompound nbt)
	{
		NBTTagList list = new NBTTagList();
		
		for (int i = 0; i <= 8; i++)
		{
			if (inventory[i] != null)
			{
				NBTTagCompound tag = new NBTTagCompound();
				inventory[i].writeToNBT(tag);
				tag.setByte("slot", (byte) i);
				
				list.appendTag(tag);
			}
		}
		
		nbt.setTag("Items", list);
	}
	
	@Override
	public int getSizeInventory() 
	{
		return 26;
	}

	@Override
	public ItemStack getStackInSlot(int slot) 
	{
		return inventory[slot];
	}

	@Override
	public ItemStack decrStackSize(int slot, int qty) 
	{
		ItemStack stack = inventory[slot];
		if (stack != null)
		{
			if (stack.stackSize <= qty)
			{
				inventory[slot] = null;
			}
			else
			{
				stack = stack.splitStack(qty);
				if (stack.stackSize == 0)
				{
					inventory[slot] = null;
				}
			}
		}
		return stack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) 
	{
		if (inventory[slot] != null)
		{
			ItemStack stack = inventory[slot];
			inventory[slot] = null;
			return stack;
		}
		
		return null;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) 
	{
		inventory[slot] = stack;
		
		if (stack != null && stack.stackSize > this.getInventoryStackLimit())
		{
			stack.stackSize = this.getInventoryStackLimit();
		}
		
		this.markDirty();
	}

	@Override
	public String getInventoryName() 
	{
		return "Transmutation Stone";
	}

	@Override
	public boolean hasCustomInventoryName() 
	{
		return false;
	}

	@Override
	public int getInventoryStackLimit() 
	{
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer var1) 
	{
		return true;
	}

	@Override
	public void openInventory() 
	{
		emc = Transmutation.getStoredEmc(player.getCommandSenderName());
		
		updateOutputs();
	}

	@Override
	public void closeInventory() 
	{
		writeToNBT(player.getHeldItem().stackTagCompound);
		
		Transmutation.setStoredEmc(player.getCommandSenderName(), emc);
		
		player = null;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) 
	{
		return false;
	}

	@Override
	public void markDirty() {}
	
	public void addEmc(double value)
	{
		emc += value;
		
		if (emc >= Constants.TILE_MAX_EMC || emc < 0)
		{
			emc = Constants.TILE_MAX_EMC;
		}
	}
	
	public void removeEmc(double value) 
	{
		emc -= value;
		
		if (emc < 0)
		{
			emc = 0;
		}
	}

	public boolean hasMaxedEmc()
	{
		return emc >= Constants.TILE_MAX_EMC;
	}
}
