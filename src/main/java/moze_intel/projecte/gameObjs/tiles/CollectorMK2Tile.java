package moze_intel.projecte.gameObjs.tiles;

import moze_intel.projecte.utils.Constants;
import net.minecraft.util.StatCollector;

public class CollectorMK2Tile extends CollectorMK1Tile
{
	public CollectorMK2Tile()
	{
		super(Constants.COLLECTOR_MK2_MAX, Constants.COLLECTOR_MK2_GEN, 13, 14);
	}

	@Override
	public String getInventoryName()
	{
		return StatCollector.translateToLocal("tile.pe_collector_MK2.name");
	}
}
