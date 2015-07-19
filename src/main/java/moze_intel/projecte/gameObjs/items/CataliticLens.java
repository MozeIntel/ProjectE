package moze_intel.projecte.gameObjs.items;

import moze_intel.projecte.api.tooltip.keybinds.ITTProjectile;
import moze_intel.projecte.gameObjs.entity.EntityLensProjectile;
import moze_intel.projecte.utils.Constants;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class CataliticLens extends DestructionCatalyst implements ITTProjectile
{
	public CataliticLens() 
	{
		super("catalitic_lens", (byte)7);
		this.setNoRepair();
	}
	
	@Override
	public boolean shootProjectile(EntityPlayer player, ItemStack stack)
	{
		World world = player.worldObj;
		int requiredEmc = Constants.EXPLOSIVE_LENS_COST[this.getCharge(stack)];
		
		if (!consumeFuel(player, stack, requiredEmc, true))
		{
			return false;
		}

		world.playSoundAtEntity(player, "projecte:item.pepower", 1.0F, 1.0F);
		world.spawnEntityInWorld(new EntityLensProjectile(world, player, this.getCharge(stack)));
		return true;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register)
	{
		this.itemIcon = register.registerIcon(this.getTexture("catalitic_lens"));
	}

	@Override
	public String getTooltipLocalisationPrefix()
	{
		return "pe.catalitic";
	}
}
