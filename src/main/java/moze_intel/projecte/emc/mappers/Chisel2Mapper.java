package moze_intel.projecte.emc.mappers;

import com.cricketcraft.chisel.api.carving.CarvingUtils;
import com.cricketcraft.chisel.api.carving.ICarvingGroup;
import com.cricketcraft.chisel.api.carving.ICarvingRegistry;
import com.cricketcraft.chisel.api.carving.ICarvingVariation;
import com.google.common.collect.ImmutableList;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import moze_intel.projecte.emc.IMappingCollector;
import moze_intel.projecte.emc.NormalizedSimpleStack;
import moze_intel.projecte.utils.PELogger;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


//Thanks to bdew for a first implementation of this: https://github.com/bdew/ProjectE/blob/f1b08624ff47c6cc716576701024cdb38ff3d297/src/main/java/moze_intel/projecte/emc/ChiselMapper.java
public class Chisel2Mapper implements IEMCMapper<NormalizedSimpleStack, Integer> {

	public static final List<String> chiselBlockNames = ImmutableList.of("marble", "limestone", "andesite", "granite", "diorite");

	@Override
	public String getName() {
		return "Chisel2Mapper";
	}

	@Override
	public String getDescription() {
		return "Add mappings for Blocks that are created with the Chisel2-Chisel.";
	}

	@Override
	public boolean isAvailable() {
		return Loader.isModLoaded("chisel") && "Chisel 2".equals(FMLCommonHandler.instance().findContainerFor("chisel").getName());
	}

	@Override
	public void addMappings(IMappingCollector<NormalizedSimpleStack, Integer> mapper, Configuration config) {
		ICarvingRegistry carvingRegistry = CarvingUtils.getChiselRegistry();
		if (carvingRegistry == null) return;
		for (String name: chiselBlockNames) {
			Block block = Block.getBlockFromName("chisel:" + name);
			if (block != null) {
				mapper.setValue(NormalizedSimpleStack.getNormalizedSimpleStackFor(block), 1, IMappingCollector.FixedValue.FixAndInherit);
			}
		}

		for (String name : carvingRegistry.getSortedGroupNames()) {
			handleCarvingGroup(mapper, config, carvingRegistry.getGroup(name));
		}
	}

	protected void handleCarvingGroup(IMappingCollector<NormalizedSimpleStack, Integer> mapper, Configuration config, ICarvingGroup group) {
		//XXX: Generates way too much Configs
		/*if (!config.getBoolean(group.getName(), "enableCarvingGroups", true, "Enable ICarvingGroup with name=" + group.getName() + (group.getOreName() == null ? "" :  " and oreName=" + group.getOreName())) ) {
			return;
		}*/
		List<NormalizedSimpleStack> stacks = new ArrayList<NormalizedSimpleStack>();
		for (ICarvingVariation v : group.getVariations()) {
			stacks.add(NormalizedSimpleStack.getNormalizedSimpleStackFor(Block.getIdFromBlock(v.getBlock()), v.getBlockMeta()));
		}
		if (group.getOreName() != null) {
			for (ItemStack ore : OreDictionary.getOres(group.getOreName())) {
				stacks.add(NormalizedSimpleStack.getNormalizedSimpleStackFor(ore));
			}
		}
		for (int i = 1; i < stacks.size(); i++) {
			mapper.addConversion(1, stacks.get(0), Collections.singletonList(stacks.get(i)));
			mapper.addConversion(1, stacks.get(i), Collections.singletonList(stacks.get(0)));
		}
		PELogger.logInfo(String.format("Added %d Blocks for CarvingGroup %s", stacks.size(), group.getName()));
	}
}
