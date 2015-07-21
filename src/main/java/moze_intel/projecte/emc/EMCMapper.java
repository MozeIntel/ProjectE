package moze_intel.projecte.emc;

import moze_intel.projecte.PECore;
import moze_intel.projecte.emc.mappers.Chisel2Mapper;
import moze_intel.projecte.emc.arithmetics.HiddenFractionArithmetic;
import moze_intel.projecte.emc.mappers.APICustomEMCMapper;
import moze_intel.projecte.emc.mappers.CraftingMapper;
import moze_intel.projecte.emc.mappers.CustomEMCMapper;
import moze_intel.projecte.emc.mappers.IEMCMapper;
import moze_intel.projecte.emc.mappers.LazyMapper;
import moze_intel.projecte.emc.mappers.NEIMapper;
import moze_intel.projecte.emc.mappers.OreDictionaryMapper;
import moze_intel.projecte.emc.mappers.SmeltingMapper;
import moze_intel.projecte.emc.pregenerated.PregeneratedEMC;
import moze_intel.projecte.emc.valuetranslators.FractionToIntegerTranslator;
import moze_intel.projecte.playerData.Transmutation;
import moze_intel.projecte.utils.PELogger;
import moze_intel.projecte.utils.PrefixConfiguration;

import com.google.common.collect.Maps;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.math.Fraction;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EMCMapper 
{
	public static Map<SimpleStack, Integer> emc = new LinkedHashMap<SimpleStack, Integer>();
	public static Map<NormalizedSimpleStack, Integer> graphMapperValues;

	public static void map()
	{
		List<IEMCMapper<NormalizedSimpleStack, Integer>> emcMappers = Arrays.asList(
				new OreDictionaryMapper(),
				new NEIMapper(),
				new LazyMapper(),
				new Chisel2Mapper(),
				APICustomEMCMapper.instance,
				new CustomEMCMapper(),
				new CraftingMapper(),
				new moze_intel.projecte.emc.mappers.FluidMapper(),
				new SmeltingMapper()
		);
		IValueGenerator<NormalizedSimpleStack, Integer> graphMapper = new FractionToIntegerTranslator<NormalizedSimpleStack>(new SimpleGraphMapper<NormalizedSimpleStack, Fraction>(new HiddenFractionArithmetic()));

		Configuration config = new Configuration(new File(PECore.CONFIG_DIR, "mapping.cfg"));
		config.load();

		boolean shouldUsePregenerated = config.getBoolean("pregenerate", "general", false, "When the next EMC mapping occurs write the results to config/ProjectE/pregenerated_emc.json and only ever run the mapping again" +
						" when that file does not exist, this setting is set to false, or an error occurred parsing that file.");

		if (shouldUsePregenerated && PECore.PREGENERATED_EMC_FILE.canRead() && PregeneratedEMC.tryRead(PECore.PREGENERATED_EMC_FILE, graphMapperValues = Maps.newHashMap()))
		{
			PELogger.logInfo(String.format("Loaded %d values from pregenerated EMC File", graphMapperValues.size()));
		}
		else
		{


			SimpleGraphMapper.setLogFoundExploits(config.getBoolean("logEMCExploits", "general", true,
					"Log known EMC Exploits. This can not and will not find all possible exploits. " +
							"This will only find exploits that result in fixed/custom emc values that the algorithm did not overwrite. " +
							"Exploits that derive from conversions that are unknown to ProjectE will not be found."
			));

			PELogger.logInfo("Starting to collect Mappings...");
			for (IEMCMapper<NormalizedSimpleStack, Integer> emcMapper : emcMappers)
			{
				try
				{
					if (config.getBoolean(emcMapper.getName(), "enabledMappers", emcMapper.isAvailable(), emcMapper.getDescription()) && emcMapper.isAvailable())
					{
						emcMapper.addMappings(graphMapper, new PrefixConfiguration(config, "mapperConfigurations." + emcMapper.getName()));
						PELogger.logInfo("Collected Mappings from " + emcMapper.getClass().getName());
					}
				} catch (Exception e)
				{
					PELogger.logFatal(String.format("Exception during Mapping Collection from Mapper %s. PLEASE REPORT THIS! EMC VALUES MIGHT BE INCONSISTENT!", emcMapper.getClass().getName()));
					e.printStackTrace();
				}
			}
			NormalizedSimpleStack.addMappings(graphMapper);
			PELogger.logInfo("Starting to generate Values:");

			config.save();

			graphMapperValues = graphMapper.generateValues();
			PELogger.logInfo("Generated Values...");

			filterEMCMap(graphMapperValues);

			if (shouldUsePregenerated) {
				//Should have used pregenerated, but the file was not read => regenerate.
				try
				{
					PregeneratedEMC.write(PECore.PREGENERATED_EMC_FILE, graphMapperValues);
					PELogger.logInfo("Wrote Pregen-file!");
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}


		for (Map.Entry<NormalizedSimpleStack, Integer> entry: graphMapperValues.entrySet()) {
			if (entry.getKey() instanceof NormalizedSimpleStack.NSSItem)
			{
				NormalizedSimpleStack.NSSItem normStackItem = (NormalizedSimpleStack.NSSItem)entry.getKey();
				emc.put(new SimpleStack(normStackItem.id, 1, normStackItem.damage), entry.getValue());
			}
		}

		Transmutation.cacheFullKnowledge();
		FuelMapper.loadMap();
	}

	/**
	 * Remove all entrys from the map, that are not {@link moze_intel.projecte.emc.NormalizedSimpleStack.NSSItem}s, have a value < 0 or WILDCARD_VALUE as metadata.
	 * @param map
	 */
	static void filterEMCMap(Map<NormalizedSimpleStack, Integer> map) {
		for(Iterator<Map.Entry<NormalizedSimpleStack, Integer>> iter = graphMapperValues.entrySet().iterator(); iter.hasNext();) {
			Map.Entry<NormalizedSimpleStack, Integer> entry = iter.next();
			NormalizedSimpleStack normStack = entry.getKey();
			if (normStack instanceof NormalizedSimpleStack.NSSItem && entry.getValue() > 0) {
				NormalizedSimpleStack.NSSItem normStackItem = (NormalizedSimpleStack.NSSItem)normStack;
				if (normStackItem.damage != OreDictionary.WILDCARD_VALUE) {
					continue;
				}
			}
			iter.remove();
		}
	}

	public static boolean mapContains(SimpleStack key)
	{
		SimpleStack copy = key.copy();
		copy.qnty = 1;

		return emc.containsKey(copy);
	}

	public static int getEmcValue(SimpleStack stack)
	{
		SimpleStack copy = stack.copy();
		copy.qnty = 1;

		return emc.get(copy);
	}

	public static void clearMaps() {
		emc.clear();
	}
}
