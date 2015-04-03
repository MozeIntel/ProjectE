package moze_intel.projecte.gameObjs.tiles;

import moze_intel.projecte.utils.Constants;
import net.minecraft.util.StatCollector;

public class RelayMK2Tile extends RelayMK1Tile
{
	public RelayMK2Tile()
	{
		super(12, Constants.RELAY_MK2_MAX, Constants.RELAY_MK2_OUTPUT);
	}

	@Override
	public String getInventoryName()
	{
		return StatCollector.translateToLocal("pe.relay.mk2");
	}
}
