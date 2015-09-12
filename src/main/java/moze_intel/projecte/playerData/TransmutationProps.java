package moze_intel.projecte.playerData;

import com.google.common.collect.Lists;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.PELogger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;

import java.util.Iterator;
import java.util.List;

public class TransmutationProps implements IExtendedEntityProperties
{
	private final EntityPlayer player;

	private double transmutationEmc;
	private List<ItemStack> knowledge = Lists.newArrayList();
	private ItemStack[] inputLocks = new ItemStack[9];
	private boolean hasFullKnowledge;
	private boolean hasMigrated = false;
	public static final String PROP_NAME = "ProjectETransmutation";

	public static void register(EntityPlayer player)
	{
		player.registerExtendedProperties(PROP_NAME, new TransmutationProps(player));
	}

	public static TransmutationProps getDataFor(EntityPlayer player)
	{
		return ((TransmutationProps) player.getExtendedProperties(PROP_NAME));
	}

	public TransmutationProps(EntityPlayer player)
	{
		this.player = player;
	}

	public ItemStack[] getInputLocks()
	{
		return inputLocks;
	}

	public void setInputLocks(ItemStack[] inputLocks)
	{
		this.inputLocks = inputLocks;
	}

	protected boolean hasFullKnowledge()
	{
		return hasFullKnowledge;
	}

	protected void setFullKnowledge(boolean fullKnowledge)
	{
		this.hasFullKnowledge = fullKnowledge;
	}

	protected double getTransmutationEmc()
	{
		return transmutationEmc;
	}

	protected void setTransmutationEmc(double transmutationEmc)
	{
		this.transmutationEmc = transmutationEmc;
	}

	protected List<ItemStack> getKnowledge()
	{
		pruneStaleKnowledge();
		return knowledge;
	}

	private void pruneDuplicateKnowledge()
	{
		ItemHelper.compactItemListNoStacksize(knowledge);
		for (ItemStack s : knowledge)
		{
			if (s.stackSize > 1)
			{
				s.stackSize = 1;
			}
		}
	}

	private void pruneStaleKnowledge()
	{
		Iterator<ItemStack> iter = knowledge.iterator();
		while (iter.hasNext())
		{
			if (!EMCHelper.hasEmcValueForCreation(iter.next()))
			{
				iter.remove();
			}
		}
	}

	protected NBTTagCompound saveForPacket()
	{
		NBTTagCompound compound = new NBTTagCompound();
		compound.setDouble("transmutationEmc", transmutationEmc);
		compound.setBoolean("tome", hasFullKnowledge);

		pruneStaleKnowledge();
		NBTTagList knowledgeWrite = new NBTTagList();
		for (ItemStack i : knowledge)
		{
			NBTTagCompound tag = i.writeToNBT(new NBTTagCompound());
			knowledgeWrite.appendTag(tag);
		}

		NBTTagList inputLockWrite = ItemHelper.toIndexedNBTList(inputLocks);
		compound.setTag("knowledge", knowledgeWrite);
		compound.setTag("inputlocks", inputLockWrite);
		return compound;
	}

	public void readFromPacket(NBTTagCompound compound)
	{
		transmutationEmc = compound.getDouble("transmutationEmc");
		hasFullKnowledge = compound.getBoolean("tome");

		NBTTagList list = compound.getTagList("knowledge", Constants.NBT.TAG_COMPOUND);
		knowledge.clear();
		for (int i = 0; i < list.tagCount(); i++)
		{
			ItemStack item = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
			if (item != null)
			{
				knowledge.add(item);
			}
		}

		NBTTagList list2 = compound.getTagList("inputlocks", Constants.NBT.TAG_COMPOUND);
		inputLocks = ItemHelper.copyIndexedNBTToArray(list2, new ItemStack[9]);
	}

	@Override
	public void saveNBTData(NBTTagCompound compound)
	{
		NBTTagCompound properties = new NBTTagCompound();
		properties.setDouble("transmutationEmc", transmutationEmc);
		properties.setBoolean("tome", hasFullKnowledge);

		pruneStaleKnowledge();
		NBTTagList knowledgeWrite = new NBTTagList();
		for (ItemStack i : knowledge)
		{
			NBTTagCompound tag = i.writeToNBT(new NBTTagCompound());
			knowledgeWrite.appendTag(tag);
		}

		NBTTagList inputLockWrite = ItemHelper.toIndexedNBTList(inputLocks);
		properties.setTag("knowledge", knowledgeWrite);
		properties.setTag("inputlock", inputLockWrite);
		properties.setBoolean("migrated", hasMigrated);
		compound.setTag(PROP_NAME, properties);
	}

	@Override
	public void loadNBTData(NBTTagCompound compound)
	{
		NBTTagCompound properties = compound.getCompoundTag(PROP_NAME);
		hasMigrated = properties.getBoolean("migrated");
		if (!hasMigrated && !player.worldObj.isRemote) // hasMigrated is also false if tag was not present
		{
			if (Transmutation.hasLegacyData(player))
			{
				properties = Transmutation.migratePlayerData(player);
				PELogger.logInfo("** MIGRATED TRANSMUTE DATA for player: " + player.getCommandSenderName() + " **");
			}
			else
			{
				PELogger.logInfo("** LEGACY TRANSMUTE DATA NOT FOUND. NOW USING NEW SAVING for player: " + player.getCommandSenderName() + " **");
			}

			hasMigrated = true;
		}

		transmutationEmc = properties.getDouble("transmutationEmc");
		hasFullKnowledge = properties.getBoolean("tome");

		NBTTagList list = properties.getTagList("knowledge", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < list.tagCount(); i++)
		{
			ItemStack item = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
			if (item != null)
			{
				knowledge.add(item);
			}
		}
		pruneDuplicateKnowledge();
		NBTTagList list2 = properties.getTagList("inputlock", Constants.NBT.TAG_COMPOUND);
		inputLocks = ItemHelper.copyIndexedNBTToArray(list2, new ItemStack[9]);
	}

	@Override
	public void init(Entity entity, World world) {}
}
