package moze_intel.projecte.emc.collector;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMappingCollector<T, V extends Comparable<V>> implements IMappingCollector<T, V>
{
	public void addConversion(int outnumber, T output, Iterable<T> ingredients) {
		addConversion(outnumber, output, listToMapOfCounts(ingredients));
	}

	protected Map<T, Integer> listToMapOfCounts(Iterable<T> iterable) {
		Map<T, Integer> map = new HashMap<T, Integer>();
		for (T ingredient : iterable) {
			if (map.containsKey(ingredient)) {
				int amount = map.get(ingredient);
				map.put(ingredient, amount + 1);
			} else {
				map.put(ingredient, 1);
			}
		}
		return map;
	}

	@Override
	public void setValueFromConversion(int outnumber, T something, Iterable<T> ingredients)
	{
		this.setValueFromConversion(outnumber, something, listToMapOfCounts(ingredients));
	}

	public abstract void setValueFromConversion(int outnumber, T something, Map<T, Integer> ingredientsWithAmount);

	public abstract void addConversion(int outnumber, T output, Map<T, Integer> ingredientsWithAmount);

	@Override
	public void finishCollection() {

	}
}
