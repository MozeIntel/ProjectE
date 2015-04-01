package moze_intel.projecte.emc.mappers;

import moze_intel.projecte.emc.IMappingCollector;
import moze_intel.projecte.emc.IngredientMap;
import moze_intel.projecte.emc.NormalizedSimpleStack;
import moze_intel.projecte.utils.PELogger;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.*;

public class CraftingMapper implements IEMCMapper<NormalizedSimpleStack, Integer> {

	List<IRecipeMapper> recipeMappers = Arrays.asList(new VanillaRecipeMapper(), new VanillaOreRecipeMapper());
	Set<Class> canNotMap = new HashSet<Class>();

	@Override
	public void addMappings(IMappingCollector<NormalizedSimpleStack, Integer> mapper, final Configuration config) {
		Iterator<IRecipe> iter = CraftingManager.getInstance().getRecipeList().iterator();
		while (iter.hasNext()) {
			IRecipe recipe = iter.next();
			boolean handled = false;
			ItemStack recipeOutput = recipe.getRecipeOutput();
			if (recipeOutput == null) continue;
			NormalizedSimpleStack recipeOutputNorm = NormalizedSimpleStack.getNormalizedSimpleStackFor(recipeOutput);
			for (IRecipeMapper recipeMapper : recipeMappers) {
				if (!config.getBoolean("enable" + recipeMapper.getName(), "IRecipeImplementations", true, recipeMapper.getDescription()))
					continue;
				if (recipeMapper.canHandle(recipe)) {
					handled = true;
					Iterable<CraftingIngredients> craftingIngredientIterable = recipeMapper.getIngredientsFor(recipe);
					if (craftingIngredientIterable != null) {
						for (CraftingIngredients variation : craftingIngredientIterable) {
							IngredientMap<NormalizedSimpleStack> ingredientMap = new IngredientMap<NormalizedSimpleStack>();
							for (ItemStack stack : variation.fixedIngredients) {
								if (stack == null || stack.getItem() == null) continue;
								if (stack.getItem().doesContainerItemLeaveCraftingGrid(stack)) {
									if (stack.getItem().hasContainerItem(stack)) {
										ingredientMap.addIngredient(NormalizedSimpleStack.getNormalizedSimpleStackFor(stack.getItem().getContainerItem(stack)), -1);
									}
									ingredientMap.addIngredient(NormalizedSimpleStack.getNormalizedSimpleStackFor(stack), 1);
								} else if (config.getBoolean("emcDependencyForUnconsumedItems", "", true, "If this option is enabled items that are made by crafting, with unconsumed ingredients, should only get an emc value, if the unconsumed item also has a value. (Examples: Extra Utilities Sigil, Cutting Board, Mixer, Juicer...)")) {
									//Container Item does not leave the crafting grid: we add an EMC dependency anyway.
									ingredientMap.addIngredient(NormalizedSimpleStack.getNormalizedSimpleStackFor(stack), 0);
								}
							}
							for (Iterable<ItemStack> multiIngredient : variation.multiIngredients) {
								NormalizedSimpleStack normalizedSimpleStack = NormalizedSimpleStack.createGroup(multiIngredient);
								ingredientMap.addIngredient(normalizedSimpleStack, 1);
								for (ItemStack stack : multiIngredient) {
									if (stack == null || stack.getItem() == null) continue;
									if (stack.getItem().doesContainerItemLeaveCraftingGrid(stack)) {
										IngredientMap<NormalizedSimpleStack> groupIngredientMap = new IngredientMap<NormalizedSimpleStack>();
										if (stack.getItem().hasContainerItem(stack)) {
											groupIngredientMap.addIngredient(NormalizedSimpleStack.getNormalizedSimpleStackFor(stack.getItem().getContainerItem(stack)), -1);
										}
										groupIngredientMap.addIngredient(NormalizedSimpleStack.getNormalizedSimpleStackFor(stack), 1);
										mapper.addConversionMultiple(1, normalizedSimpleStack, groupIngredientMap.getMap());
									}
								}
							}
							if (recipeOutput.stackSize > 0) {
								mapper.addConversionMultiple(recipeOutput.stackSize, recipeOutputNorm, ingredientMap.getMap());
							} else {
								PELogger.logWarn("Ignoring Recipe because outnumber <= 0: " + ingredientMap.getMap().toString() + " -> " + recipeOutput);
							}
						}
					} else {
						PELogger.logWarn("RecipeMapper " + recipeMapper + " failed to map Recipe" + recipe);
					}
					break;
				}
			}
			if (!handled) {
				if (!canNotMap.contains(recipe.getClass())) {
					canNotMap.add(recipe.getClass());
					PELogger.logWarn("Can not map Crafting Recipes with Type: " + recipe.getClass().getName());
				}
			}
		}
	}

	@Override
	public String getName() {
		return "CraftingMapper";
	}

	@Override
	public String getDescription() {
		return "Add Conversions for Crafting Recipes gathered from net.minecraft.item.crafting.CraftingManager";
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	public static interface IRecipeMapper {
		public String getName();

		public String getDescription();

		public boolean canHandle(IRecipe recipe);

		public Iterable<CraftingIngredients> getIngredientsFor(IRecipe recipe);
	}

	public static class CraftingIngredients {
		public Iterable<ItemStack> fixedIngredients;
		public Iterable<Iterable<ItemStack>> multiIngredients;

		public CraftingIngredients(Iterable<ItemStack> fixedIngredients, Iterable<Iterable<ItemStack>> multiIngredients) {
			this.fixedIngredients = fixedIngredients;
			this.multiIngredients = multiIngredients;
		}
	}

	protected static class VanillaRecipeMapper implements IRecipeMapper {

		@Override
		public String getName() {
			return "VanillaRecipeMapper";
		}

		@Override
		public String getDescription() {
			return "Maps `IRecipe` crafting recipes that extend `ShapedRecipes` or `ShapelessRecipes`";
		}

		@Override
		public boolean canHandle(IRecipe recipe) {
			return recipe instanceof ShapedRecipes || recipe instanceof ShapelessRecipes;
		}

		@Override
		public Iterable<CraftingIngredients> getIngredientsFor(IRecipe recipe) {
			Iterable recipeItems = null;
			if (recipe instanceof ShapedRecipes) {
				recipeItems = Arrays.asList(((ShapedRecipes) recipe).recipeItems);
			} else if (recipe instanceof ShapelessRecipes) {
				recipeItems = ((ShapelessRecipes) recipe).recipeItems;
			}
			List<ItemStack> inputs = new LinkedList<ItemStack>();
			for (Object o : recipeItems) {
				if (o == null) continue;
				if (o instanceof ItemStack) {
					ItemStack recipeItem = (ItemStack) o;
					inputs.add(recipeItem);
				} else {
					PELogger.logWarn("Illegal Ingredient in Crafting Recipe: " + o.toString());
				}
			}
			return Arrays.asList(new CraftingIngredients(inputs, new LinkedList()));
		}

	}

	protected static class VanillaOreRecipeMapper implements IRecipeMapper {

		@Override
		public String getName() {
			return "VanillaOreRecipeMapper";
		}

		@Override
		public String getDescription() {
			return "Maps `IRecipe` crafting recipes that extend `ShapedOreRecipe` or `ShapelessOreRecipe`. This includes CraftingRecipes that use OreDictionary ingredients.";
		}

		@Override
		public boolean canHandle(IRecipe recipe) {
			return recipe instanceof ShapedOreRecipe || recipe instanceof ShapelessOreRecipe;
		}

		@Override
		public Iterable<CraftingIngredients> getIngredientsFor(IRecipe recipe) {
			List<IngredientMap<ItemStack>> inputs = new LinkedList<IngredientMap<ItemStack>>();
			Iterable<Object> recipeItems = null;
			if (recipe instanceof ShapedOreRecipe) {
				recipeItems = Arrays.asList(((ShapedOreRecipe) recipe).getInput());
			} else if (recipe instanceof ShapelessOreRecipe) {
				recipeItems = ((ShapelessOreRecipe) recipe).getInput();
			}
			if (recipeItems == null) return null;
			ArrayList<Iterable<ItemStack>> variableInputs = new ArrayList<Iterable<ItemStack>>();
			ArrayList<ItemStack> fixedInputs = new ArrayList<ItemStack>();
			for (Object recipeItem : recipeItems) {
				if (recipeItem instanceof ItemStack) {
					fixedInputs.add((ItemStack) recipeItem);
				} else if (recipeItem instanceof Collection) {
					List<ItemStack> recipeItemOptions = new LinkedList<ItemStack>();
					Collection recipeItemCollection = ((Collection) recipeItem);
					if (recipeItemCollection.size() == 1) {
						Object element = recipeItemCollection.iterator().next();
						if (element instanceof ItemStack) {
							fixedInputs.add((ItemStack) element);
						} else {
							PELogger.logWarn("Can not map recipe " + recipe + " because found " + element.toString() + " instead of ItemStack");
							return null;
						}
						continue;
					}
					for (Object option : recipeItemCollection) {
						if (option instanceof ItemStack) {
							recipeItemOptions.add((ItemStack) option);
						} else {
							PELogger.logWarn("Can not map recipe " + recipe + " because found " + option.toString() + " instead of ItemStack");
							return null;
						}
					}
					variableInputs.add(recipeItemOptions);
				}
			}
			return Arrays.asList(new CraftingIngredients(fixedInputs, variableInputs));
		}
	}
}
