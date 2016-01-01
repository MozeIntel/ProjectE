package moze_intel.projecte.emc;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import moze_intel.projecte.emc.collector.IMappingCollector;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.PELogger;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class NormalizedSimpleStack {
	public static Map<String, Set<Integer>> idWithUsedMetaData = Maps.newHashMap();

	public static NormalizedSimpleStack getFor(String itemName, int damage, NBTTagCompound nbt) {
		NSSItem normStack;
		try
		{
			normStack = new NSSItem(itemName, damage, nbt);
		} catch (Exception e) {
			PELogger.logFatal("Could not create NSSItem: " + e.getMessage());
			return null;
		}
		Set<Integer> usedMetadata;
		if (!idWithUsedMetaData.containsKey(normStack.itemName)) {
			usedMetadata = Sets.newHashSet();
			idWithUsedMetaData.put(normStack.itemName, usedMetadata);
		} else {
			usedMetadata = idWithUsedMetaData.get(normStack.itemName);
		}
		usedMetadata.add(normStack.damage);
		return normStack;
	}
	
	public static NormalizedSimpleStack getFor(String itemName, int damage){
		return getFor(itemName, damage, null);
	}

	public static NormalizedSimpleStack getFor(Block block) {
		return getFor(block, 0);
	}

	public static NormalizedSimpleStack getFor(Block block, int meta) {
		return getFor(GameRegistry.findUniqueIdentifierFor(block), meta, null);
	}

	public static NormalizedSimpleStack getFor(Item item) {
		return getFor(item, 0, null);
	}

	public static NormalizedSimpleStack getFor(Item item, int meta, NBTTagCompound stackTagCompound) {
		return getFor(GameRegistry.findUniqueIdentifierFor(item), meta, stackTagCompound);
	}

	private static NormalizedSimpleStack getFor(GameRegistry.UniqueIdentifier uniqueIdentifier, int damage, NBTTagCompound nbt)
	{
		if (uniqueIdentifier == null) return null;
		return getFor(uniqueIdentifier.modId + ":" + uniqueIdentifier.name, damage, nbt);
	}



	public static NormalizedSimpleStack getFor(ItemStack stack) {
		if (stack == null || stack.getItem() == null) return null;
		return getFor(stack.getItem(), stack.getItemDamage(), stack.stackTagCompound);
	}

	public static NormalizedSimpleStack getFor(net.minecraftforge.fluids.Fluid fluid) {
		//TODO cache The fluid normalizedSimpleStacks?
		return new NSSFluid(fluid);
	}

	public static <V extends Comparable<V>> void addMappings(IMappingCollector<NormalizedSimpleStack, V> mapper) {
		for (Map.Entry<String, Set<Integer>> entry : idWithUsedMetaData.entrySet()) {
			entry.getValue().remove(OreDictionary.WILDCARD_VALUE);
			entry.getValue().add(0);
			NormalizedSimpleStack stackWildcard = new NSSItem(entry.getKey(), OreDictionary.WILDCARD_VALUE);
			for (int metadata : entry.getValue()) {
				mapper.addConversion(1, stackWildcard, Arrays.asList((NormalizedSimpleStack)new NSSItem(entry.getKey(), metadata)));
			}
		}

		for (Map.Entry<String, NormalizedSimpleStack> entry: oreDictStacks.entrySet()) {
			NormalizedSimpleStack oreDictStack = entry.getValue();
			List<ItemStack> list = ItemHelper.getODItems(entry.getKey());
			for (ItemStack i: list) {
				mapper.addConversion(1, oreDictStack, Arrays.asList(NormalizedSimpleStack.getFor(i)));
				mapper.addConversion(1, NormalizedSimpleStack.getFor(i), Arrays.asList(oreDictStack));
			}
		}
	}

	public abstract boolean equals(Object o);
	public abstract String json();


	private static Map<String, NormalizedSimpleStack> oreDictStacks = Maps.newHashMap();
	public static NormalizedSimpleStack forOreDictionary(String oreDictionaryName)
	{
		if (oreDictStacks.containsKey(oreDictionaryName))
			return oreDictStacks.get(oreDictionaryName);
		List<ItemStack> list = OreDictionary.getOres(oreDictionaryName);
		if (list == null || list.size() == 0) {
			return null;
		}
		NormalizedSimpleStack nss = new NSSOreDictionary(oreDictionaryName);
		oreDictStacks.put(oreDictionaryName, nss);
		return nss;
	}

	public static class NSSItem extends NormalizedSimpleStack {
		public final String itemName;
		public final int damage;
		public final NBTTagCompound nbt;
		private NSSItem(String itemName, int damage, NBTTagCompound nbt) {
			this.itemName = itemName;
			if (Item.itemRegistry.getObject(itemName) == null) {
				throw new IllegalArgumentException("Invalid Item with itemName = " + itemName);
			}
			this.damage = damage;
			this.nbt = nbt;
		}
		
		private NSSItem(String itemName, int damage){
			this(itemName, damage, null);
		}

		@Override
		public int hashCode() {
			return itemName.hashCode() ^ damage;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof NSSItem) {
				NSSItem other = (NSSItem) obj;

				return this.itemName.equals(other.itemName) && this.damage == other.damage;
			}

			return false;
		}

		@Override
		public String json()
		{
			return String.format("%s|%s", itemName,  damage == OreDictionary.WILDCARD_VALUE ? "*" : damage);
		}

		@Override
		public String toString() {
			Object obj = Item.itemRegistry.getObject(itemName);

			if (obj != null) {
				return String.format("%s(%s:%s)", itemName, Item.itemRegistry.getIDForObject(obj), damage == OreDictionary.WILDCARD_VALUE ? "*" : damage);
			}

			return String.format("%s(???:%s)", itemName, damage == OreDictionary.WILDCARD_VALUE ? "*" : damage);
		}
	}

	public static NormalizedSimpleStack createFake(String description) {
		return new NSSFake(description);
	}

	public static class NSSFake extends NormalizedSimpleStack {
		public final String description;
		public final int counter;
		private static int fakeItemCounter = 0;
		public NSSFake(String description)
		{
			this.counter = fakeItemCounter++;
			this.description = description;
		}

		public boolean equals(Object o) {
			if (o instanceof NSSFake) {
				return o == this;
			}
			return false;
		}

		@Override
		public String json()
		{
			return "FAKE|" + this.counter + " " + this.description;
		}

		@Override
		public String toString() {
			return "NSSFAKE" + counter + ": " + description;
		}
	}

	public static class NSSFluid extends NormalizedSimpleStack {

		public final String name;
		private NSSFluid(net.minecraftforge.fluids.Fluid f) {
			this.name = f.getName();
		}
		public boolean equals(Object o) {
			if (o instanceof NSSFluid) {
				return name.equals(((NSSFluid) o).name);
			}
			return false;
		}

		@Override
		public String json()
		{
			return "FLUID|"+this.name;
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public String toString() {
			return "Fluid: " + this.name;
		}
	}

	public static class NSSOreDictionary extends NormalizedSimpleStack {

		public final String od;
		private NSSOreDictionary(String od) {
			this.od = od;
		}

		@Override
		public int hashCode()
		{
			return od.hashCode();
		}

		@Override
		public boolean equals(Object o)
		{
			if (o instanceof NSSOreDictionary) {
				return this.od.equals(((NSSOreDictionary) o).od);
			}
			return false;
		}

		@Override
		public String json()
		{
			return "OD|" + this.od;
		}

		@Override
		public String toString() {
			return "OD: " + od;
		}
	}
	//TODO Add NBT support here
	public static NormalizedSimpleStack fromSerializedItem(String serializedItem) {
		int pipeIndex = serializedItem.lastIndexOf('|');
		if (pipeIndex < 0)
		{
			throw new IllegalArgumentException(String.format("Cannot parse '%s' as itemstack. Missing | to separate metadata.", serializedItem));
		}
		String itemName = serializedItem.substring(0, pipeIndex);
		String itemDamageString = serializedItem.substring(pipeIndex + 1);
		int itemDamage;
		if (itemDamageString.equals("*"))
		{
			itemDamage = OreDictionary.WILDCARD_VALUE;
		}
		else
		{
			try
			{
				itemDamage = Integer.parseInt(itemDamageString);
			} catch (NumberFormatException e)
			{
				throw new IllegalArgumentException(String.format("Could not parse '%s' to metadata-integer", itemDamageString), e);
			}
		}

		return NormalizedSimpleStack.getFor(itemName, itemDamage);
	}
}
