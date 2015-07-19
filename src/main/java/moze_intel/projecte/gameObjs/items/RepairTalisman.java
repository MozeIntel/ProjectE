package moze_intel.projecte.gameObjs.items;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.google.common.collect.Lists;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moze_intel.projecte.api.IAlchBagItem;
import moze_intel.projecte.api.IAlchChestItem;
import moze_intel.projecte.api.IModeChanger;
import moze_intel.projecte.api.IPedestalItem;
import moze_intel.projecte.api.tooltip.ITTAlchBagFunctionalityGroup;
import moze_intel.projecte.api.tooltip.ITTAlchChestFunctionalityGroup;
import moze_intel.projecte.api.tooltip.ITTBaubleFunctionalityGroup;
import moze_intel.projecte.api.tooltip.special.ITTGeneralFunctionality;
import moze_intel.projecte.api.tooltip.special.ITTPedestalFunctionalityGroupSpecial;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.items.rings.RingToggle;
import moze_intel.projecte.gameObjs.tiles.AlchChestTile;
import moze_intel.projecte.gameObjs.tiles.DMPedestalTile;
import moze_intel.projecte.handlers.PlayerTimers;
import moze_intel.projecte.utils.MathUtils;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "Baubles")
public class RepairTalisman extends ItemPE implements ITTAlchBagFunctionalityGroup, ITTAlchChestFunctionalityGroup, ITTBaubleFunctionalityGroup, ITTPedestalFunctionalityGroupSpecial, ITTGeneralFunctionality
{
	public RepairTalisman()
	{
		this.setUnlocalizedName("repair_talisman");
		this.setMaxStackSize(1);
	}
	
	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int par4, boolean par5) 
	{
		if (!stack.hasTagCompound())
		{
			stack.stackTagCompound = new NBTTagCompound();
		}
		
		if (world.isRemote || !(entity instanceof EntityPlayer))
		{
			return;
		}
		
		EntityPlayer player = (EntityPlayer) entity;

		PlayerTimers.activateRepair(player);

		if (PlayerTimers.canRepair(player))
		{
			repairAllItems(player);
		}
	}

	public void repairAllItems(EntityPlayer player)
	{
		IInventory inv = player.inventory;

		for (int i = 0; i < 40; i++)
		{
			ItemStack invStack = inv.getStackInSlot(i);

			if (invStack == null || invStack.getItem() instanceof IModeChanger || !invStack.getItem().isRepairable())
			{
				continue;
			}

			if (invStack.equals(player.getCurrentEquippedItem()) && player.isSwingInProgress) {
				//Don't repair item that is currently used by the player.
				continue;
			}

			if (!invStack.getHasSubtypes() && invStack.getMaxDamage() != 0 && invStack.getItemDamage() > 0)
			{
				invStack.setItemDamage(invStack.getItemDamage() - 1);
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register)
	{
		this.itemIcon = register.registerIcon(this.getTexture("repair_talisman"));
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public baubles.api.BaubleType getBaubleType(ItemStack itemstack)
	{
		return BaubleType.BELT;
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public void onWornTick(ItemStack stack, EntityLivingBase player) 
	{
		this.onUpdate(stack, player.worldObj, player, 0, false);
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public void onEquipped(ItemStack itemstack, EntityLivingBase player) {}

	@Override
	@Optional.Method(modid = "Baubles")
	public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {}

	@Override
	@Optional.Method(modid = "Baubles")
	public boolean canEquip(ItemStack itemstack, EntityLivingBase player) 
	{
		return true;
	}

	@Override
	@Optional.Method(modid = "Baubles")
	public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) 
	{
		return true;
	}

	@Override
	public void updateInPedestal(World world, int x, int y, int z)
	{
		if (!world.isRemote && ProjectEConfig.repairPedCooldown != -1)
		{
			DMPedestalTile tile = ((DMPedestalTile) world.getTileEntity(x, y, z));
			if (tile.getActivityCooldown() == 0)
			{
				List<EntityPlayerMP> list = world.getEntitiesWithinAABB(EntityPlayerMP.class, tile.getEffectBounds());
				for (EntityPlayerMP player : list)
				{
					repairAllItems(player);
				}
				tile.setActivityCooldown(ProjectEConfig.repairPedCooldown);
			}
			else
			{
				tile.decrementActivityCooldown();
			}
		}
	}

	@Override
	public List<String> getPedestalDescription()
	{
		List<String> list = Lists.newArrayList();
		if (ProjectEConfig.repairPedCooldown != -1)
		{
			list.add(EnumChatFormatting.BLUE + StatCollector.translateToLocal("pe.repairtalisman.pedestal1"));
			list.add(EnumChatFormatting.BLUE +
					String.format(StatCollector.translateToLocal("pe.repairtalisman.pedestal2"), MathUtils.tickToSecFormatted(ProjectEConfig.repairPedCooldown)));
		}
		return list;
	}

	@Override
	public void updateInAlchChest(AlchChestTile tile, ItemStack stack)
	{
		if (tile.getWorldObj().isRemote)
		{
			return;
		}

		byte coolDown = stack.stackTagCompound.getByte("Cooldown");

		if (coolDown > 0)
		{
			stack.stackTagCompound.setByte("Cooldown", (byte) (coolDown - 1));
		}
		else
		{
			boolean hasAction = false;

			for (int i = 0; i < tile.getSizeInventory(); i++)
			{
				ItemStack invStack = tile.getStackInSlot(i);

				if (invStack == null || invStack.getItem() instanceof RingToggle || !invStack.getItem().isRepairable())
				{
					continue;
				}

				if (!invStack.getHasSubtypes() && invStack.getMaxDamage() != 0 && invStack.getItemDamage() > 0)
				{
					invStack.setItemDamage(invStack.getItemDamage() - 1);
					tile.setInventorySlotContents(i, invStack);

					if (!hasAction)
					{
						hasAction = true;
					}
				}
			}

			if (hasAction)
			{
				stack.stackTagCompound.setByte("Cooldown", (byte) 19);
				tile.markDirty();
			}
		}
	}

	@Override
	public boolean updateInAlchBag(ItemStack[] inv, EntityPlayer player, ItemStack stack)
	{
		if (player.worldObj.isRemote)
		{
			return false;
		}

		byte coolDown = stack.stackTagCompound.getByte("Cooldown");

		if (coolDown > 0)
		{
			stack.stackTagCompound.setByte("Cooldown", (byte) (coolDown - 1));
		}
		else
		{
			boolean hasAction = false;

			for (int i = 0; i < inv.length; i++)
			{
				ItemStack invStack = inv[i];

				if (invStack == null || invStack.getItem() instanceof RingToggle || !invStack.getItem().isRepairable())
				{
					continue;
				}

				if (!invStack.getHasSubtypes() && invStack.getMaxDamage() != 0 && invStack.getItemDamage() > 0)
				{
					invStack.setItemDamage(invStack.getItemDamage() - 1);

					if (!hasAction)
					{
						hasAction = true;
					}
				}
			}

			if (hasAction)
			{
				stack.stackTagCompound.setByte("Cooldown", (byte) 19);
				return true;
			}
		}
		return false;
	}

	@Override
	public String getTooltipLocalisationPrefix()
	{
		return "pe.repairtalisman";
	}
}
