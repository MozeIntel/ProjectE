package moze_intel.projecte.gameObjs.tiles;

import moze_intel.projecte.utils.Constants;
import net.minecraft.util.StatCollector;

public class CollectorMK3Tile extends CollectorMK1Tile
{
	public CollectorMK3Tile()
	{
		super(Constants.COLLECTOR_MK3_MAX, Constants.COLLECTOR_MK3_GEN, 17, 18);
	}

	@Override
	public String getInventoryName()
	{
		return StatCollector.translateToLocal("tile.pe_collector_MK3.name");
	}
}
