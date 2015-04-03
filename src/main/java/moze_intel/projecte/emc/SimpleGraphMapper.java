package moze_intel.projecte.emc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SimpleGraphMapper<T, V extends Comparable<V>> extends GraphMapper<T, V> {
	static boolean OVERWRITE_FIXED_VALUES = false;
	protected V ZERO;
	public SimpleGraphMapper(IValueArithmetic<V> arithmetic) {
		super(arithmetic);
		ZERO = arithmetic.getZero();
	}

	protected static<K,V extends Comparable<V>> boolean hasSmaller(Map<K,V> m, K key, V value) {
		return (m.containsKey(key) && value.compareTo(m.get(key)) >= 0);
	}

	protected static<K, V extends Comparable<V>> boolean updateMapWithMinimum(Map<K,V> m, K key, V value) {
		if (!hasSmaller(m,key,value)) {
			//No Value or a value that is smaller than this
			m.put(key, value);
			return true;
		}
		return false;
	}

	protected boolean canOverride(T something, V value) {
		if (OVERWRITE_FIXED_VALUES) return  true;
		if (fixValueBeforeInherit.containsKey(something)) {
			return fixValueBeforeInherit.get(something).compareTo(value) == 0;
		}
		return true;
	}

	@Override
	public Map<T, V> generateValues() {
		Map<T, V> values = new HashMap<T, V>();
		Map<T, V> newValueFor = new HashMap<T, V>();
		Map<T, V> nextValueFor = new HashMap<T, V>();
		Map<T,Object> reasonForChange = new HashMap<T, Object>();


		for (Map.Entry<T,V> entry: fixValueBeforeInherit.entrySet()) {
			newValueFor.put(entry.getKey(),entry.getValue());
			reasonForChange.put(entry.getKey(), "fixValueBefore");
		}
		while (!newValueFor.isEmpty()) {
			while (!newValueFor.isEmpty()) {
				debugPrintln("Loop");
				for (Map.Entry<T, V> entry : newValueFor.entrySet()) {
					if (canOverride(entry.getKey(),entry.getValue()) && updateMapWithMinimum(values, entry.getKey(), entry.getValue())) {
						//The new Value is now set in 'values'
						debugFormat("Set Value for %s to %s because %s", entry.getKey(), entry.getValue(), reasonForChange.get(entry.getKey()));
						//We have a new value for 'entry.getKey()' now we need to update everything that uses it as an ingredient.
						for (Conversion conversion : getUsesFor(entry.getKey())) {
							//Calculate how much the conversion-output costs with the new Value for entry.getKey
							V conversionValue = arithmetic.div(valueForConversion(values, conversion), conversion.outnumber);
							if (conversionValue.compareTo(ZERO) > 0 || arithmetic.isFree(conversionValue)) {
								//We could calculate a valid value for the conversion
								if (!hasSmaller(values, conversion.output, conversionValue)) {
									//And there is no smaller value for that conversion output yet
									if (updateMapWithMinimum(nextValueFor, conversion.output, conversionValue)) {
										//So we mark that new value to set it in the next iteration.
										reasonForChange.put(conversion.output, entry.getKey());
									}
								}
							}
						}
					}
				}

				//Swap nextValueFor into newValueFor and clear newValueFor
				{
					newValueFor.clear();
					Map<T, V> tmp = nextValueFor;
					nextValueFor = newValueFor;
					newValueFor = tmp;
				}
			}
			//Iterate over all Conversions for a single conversion output
			for (Map.Entry<T, List<Conversion>> entry : conversionsFor.entrySet()) {
				V minConversionValue = null;
				//For all Conversions. All these have the same output.
				for (Conversion conversion : entry.getValue()) {
					//entry.getKey() == conversion.output
					//How much do the ingredients cost:
					V conversionValue = valueForConversion(values, conversion);
					//What would the output cost be, if that conversion would be used
					V conversionValueSingle = arithmetic.div(conversionValue, conversion.outnumber);
					//What is the actual emc value for the conversion output
					V resultValue = values.containsKey(entry.getKey()) ? arithmetic.mul(conversion.outnumber, values.get(entry.getKey())) : ZERO;

					//Find the smallest EMC value for the conversion.output
					if (conversionValueSingle.compareTo(ZERO) > 0 || arithmetic.isFree(conversionValueSingle)) {
						if (minConversionValue == null || minConversionValue.compareTo(conversionValueSingle) > 0) {
							minConversionValue = conversionValueSingle;
						}
					}
					//the cost for the ingredients is greater zero, but smaller than the value that the output has.
					//This is a Loophole. We remove it by setting the value to 0.
					if (canOverride(entry.getKey(),ZERO) && ZERO.compareTo(conversionValue) < 0 && conversionValue.compareTo(resultValue) < 0) {
						debugFormat("Setting %s to 0 because result (%s) > cost (%s): %s", entry.getKey(), resultValue, conversionValue, conversion);
						newValueFor.put(conversion.output, ZERO);
						reasonForChange.put(conversion.output, "exploit recipe");
					}
				}
				if (minConversionValue == null || minConversionValue.equals(ZERO)) {
					//we could not find any valid conversion
					if (values.containsKey(entry.getKey()) && !values.get(entry.getKey()).equals(ZERO) && canOverride(entry.getKey(), ZERO) && !hasSmaller(values, entry.getKey(), ZERO)) {
						//but the value for the conversion output is > 0, so we set it to 0.
						debugFormat("Removing Value for %s because it does not have any nonzero-conversions anymore.", entry.getKey());
						newValueFor.put(entry.getKey(), ZERO);
						reasonForChange.put(entry.getKey(), "all conversions dead");
					}
				}
			}
		}
		debugPrintln("");
		for (Map.Entry<T, V> fixedValueAfterInherit : fixValueAfterInherit.entrySet()) {
			values.put(fixedValueAfterInherit.getKey(), fixedValueAfterInherit.getValue());
		}
		//Remove all 'free' items from the output-values
		for (Iterator<T> iter = values.keySet().iterator(); iter.hasNext();) {
			T something = iter.next();
			if (arithmetic.isFree(values.get(something))) {
				iter.remove();
			}
		}
		return values;
	}

	/**
	 * Calculate the combined Cost for the ingredients in the Conversion.
	 * @param values The values for the ingredients to use in the calculation
	 * @param conversion The Conversion for which to calculate the combined ingredient cost.
	 * @return The combined ingredient value, ZERO or arithmetic.getFree()
	 */
	protected V valueForConversion(Map<T, V> values, Conversion conversion) {
		V value = conversion.value;
		boolean allIngredientsAreFree = true;
		boolean hasPositiveIngredientValues = false;
		for (Map.Entry<T, Integer> entry:conversion.ingredientsWithAmount.entrySet()) {
			if (values.containsKey(entry.getKey())) {
				//value = value + amount * ingredientcost
				V ingredientValue = values.get(entry.getKey());
				if (ingredientValue.compareTo(ZERO) != 0) {
					if (!arithmetic.isFree(ingredientValue)) {
						value = arithmetic.add(value, arithmetic.mul(entry.getValue(), ingredientValue));
						if (ingredientValue.compareTo(ZERO) > 0 && entry.getValue() > 0) hasPositiveIngredientValues = true;
						allIngredientsAreFree = false;
					}
				} else {
					//There is an ingredient with value = 0 => we cannot calculate the combined ingredient cost.
					return ZERO;
				}
			} else {
				//There is an ingredient that does not have a value => we cannot calculate the combined ingredient cost.
				return ZERO;
			}
		}
		//When all the ingredients for are 'free' or ingredients with negative amount made the Conversion have a value <= 0 this item should be free
		if (allIngredientsAreFree || (hasPositiveIngredientValues && value.compareTo(ZERO) <= 0)) return arithmetic.getFree();
		return value;
	}
}
