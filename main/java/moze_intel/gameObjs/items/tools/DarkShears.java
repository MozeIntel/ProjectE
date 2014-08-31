package moze_intel.gameObjs.items.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import moze_intel.MozeCore;
import moze_intel.config.ProjectEConfig;
import moze_intel.gameObjs.entity.LootBall;
import moze_intel.gameObjs.items.ItemCharge;
import moze_intel.network.packets.SwingItemPKT;
import moze_intel.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class DarkShears extends ItemCharge
{
	public DarkShears()
	{
		super("dm_shears", (byte) 3);
	}
	
	@Override
	public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase ent)
	{
		if (block.getMaterial() != Material.leaves && block != Blocks.web && block != Blocks.tallgrass && block != Blocks.vine && block != Blocks.tripwire && !(block instanceof IShearable))
		{
			return super.onBlockDestroyed(stack, world, block, x, y, z, ent);
		}
		else
		{
			return true;
		}
	}
	
	@Override
	public boolean func_150897_b(Block block)
	{
		return block == Blocks.web || block == Blocks.redstone_wire || block == Blocks.tripwire;
	}

	@Override
	public float func_150893_a(ItemStack stack, Block block)
	{
		return block != Blocks.web && block.getMaterial() != Material.leaves ? (block == Blocks.wool ? 5.0F : super.func_150893_a(stack, block)) : 15.0F;
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		if (!world.isRemote)
		{
			byte charge = this.getCharge(stack);
			
			int offset = 0;
			
			switch (charge)
			{
				case 0:
					offset = 4;
					break;
				case 1:
					offset = 8;
					break;
				case 2:
					offset = 16;
					break;
			}
			
			AxisAlignedBB bBox = player.boundingBox.expand(offset, offset / 2, offset);
			List<Entity> list = world.getEntitiesWithinAABB(IShearable.class, bBox);
			
			if (list.isEmpty())
			{
				return stack;
			}
			
			List<ItemStack> drops = new ArrayList();
			
			for (Entity ent : list)
			{
				IShearable target = (IShearable) ent;
				
				if (target.isShearable(stack, ent.worldObj, (int) ent.posX, (int) ent.posY, (int) ent.posZ))
				{
					ArrayList<ItemStack> entDrops = target.onSheared(stack, ent.worldObj, (int) ent.posX, (int) ent.posY, (int) ent.posZ, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, stack));
					
					if (entDrops.isEmpty())
					{
						continue;
					}
					
					for (ItemStack drop : entDrops)
					{
						drop.stackSize += Utils.randomIntInRange(6, 3);
					}
					
					drops.addAll(entDrops);
				}
			}
			
			if (!drops.isEmpty())
			{
				world.spawnEntityInWorld(new LootBall(world, drops, player.posX, player.posY, player.posZ));
				MozeCore.pktHandler.sendTo(new SwingItemPKT(), (EntityPlayerMP) player);
			}
		}
		
		return stack;
	}

	@Override
	public boolean onBlockStartBreak(ItemStack itemstack, int x, int y, int z, EntityPlayer player)
	{
		if (player.worldObj.isRemote)
		{
			return false;
		}
		
		Block block = player.worldObj.getBlock(x, y, z);
		
		if (block instanceof IShearable)
		{
			IShearable target = (IShearable) block;
			
			if (target.isShearable(itemstack, player.worldObj, x, y, z))
			{
				ArrayList<ItemStack> drops = target.onSheared(itemstack, player.worldObj, x, y, z, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemstack));
				Random rand = new Random();

				for(ItemStack stack : drops)
				{
					float f = 0.7F;
					double d = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
					double d1 = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
					double d2 = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
					EntityItem entityitem = new EntityItem(player.worldObj, (double)x + d, (double)y + d1, (double)z + d2, stack);
					entityitem.delayBeforeCanPickup = 10;
					player.worldObj.spawnEntityInWorld(entityitem);
				}

				itemstack.damageItem(1, player);
				player.addStat(StatList.mineBlockStatArray[Block.getIdFromBlock(block)], 1);
			}
		}
	
		return false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register)
	{
		if(ProjectEConfig.UseOldResources == false){
			this.itemIcon = register.registerIcon(this.getTexture("dm_tools", "shears"));
		}
		else{
			this.itemIcon = register.registerIcon(this.getTexture("dm_tools", "shears_old"));
		}
	}
}
