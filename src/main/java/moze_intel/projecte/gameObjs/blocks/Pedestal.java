package moze_intel.projecte.gameObjs.blocks;


import cpw.mods.fml.common.network.NetworkRegistry;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.IPedestalItem;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.ObjHandler;
import moze_intel.projecte.gameObjs.items.TimeWatch;
import moze_intel.projecte.gameObjs.tiles.DMPedestalTile;
import moze_intel.projecte.gameObjs.tiles.TileEmc;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.ClientSyncPedestalPKT;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.PELogger;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class Pedestal extends Block implements ITileEntityProvider {

    public Pedestal() {
        super(Material.rock);
        this.setCreativeTab(ObjHandler.cTab);
        this.setHardness(1.0F);
        this.setBlockBounds(0.1875F, 0.0F, 0.1875F, 0.8125F, 0.75F, 0.8125F);
        this.setBlockTextureName(PECore.MODID.toLowerCase() + ":dm");
        setBlockName("pe_dmPedestal");
    }

    public void breakBlock(World world, int x, int y, int z, Block block, int meta)
    {
        DMPedestalTile tile = ((DMPedestalTile) world.getTileEntity(x, y, z));
        if (tile.getItemStack() != null)
        {
            WorldHelper.spawnEntityItem(world, tile.getItemStack().copy(), x, y, z);
        }
        tile.invalidate();
        super.breakBlock(world, x, y, z, block, meta);
    }

    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        if (!world.isRemote)
        {
            DMPedestalTile tile = ((DMPedestalTile) world.getTileEntity(x, y, z));
            if (player.isSneaking())
            {
                player.openGui(PECore.instance, Constants.PEDESTAL_GUI, world, x, y, z);
            }
            else
            {
                if (tile.getItemStack() != null && tile.getItemStack().getItem() instanceof IPedestalItem)
                {
                    if(!ProjectEConfig.enableTimeWatch && tile.getItemStack().getItem() instanceof TimeWatch) {
                        player.addChatComponentMessage(new ChatComponentTranslation("pe.timewatch.disabled"));
                        PELogger.logDebug("Pedestal: Attempt to use a TimeWatch with Pedestal. TiemWatch is Disabled");
                    } else {
                        tile.setActive(!tile.getActive());
                    }
                }
                PELogger.logDebug("Pedestal: " + (tile.getActive() ? "ON" : "OFF"));
            }
            PacketHandler.sendToAllAround(new ClientSyncPedestalPKT(tile), new NetworkRegistry.TargetPoint(world.provider.dimensionId, x, y, z, 32));
        }
        return true;
    }

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase ent, ItemStack stack)
	{
		TileEntity tile = world.getTileEntity(x, y, z);
		if (stack.hasTagCompound() && stack.stackTagCompound.getBoolean("ProjectEBlock") && tile instanceof TileEmc)
		{
			stack.stackTagCompound.setInteger("x", x);
			stack.stackTagCompound.setInteger("y", y);
			stack.stackTagCompound.setInteger("z", z);

			tile.readFromNBT(stack.stackTagCompound);
		}
	}


	@Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public int getRenderType()
    {
        return Constants.PEDESTAL_RENDER_ID;
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z)
    {
        return 12;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int par2) {
        return new DMPedestalTile();
    }
}
