package moze_intel.projecte.gameObjs.container.slots.transmuteportable;

import moze_intel.projecte.gameObjs.ObjHandler;
import moze_intel.projecte.gameObjs.container.inventory.TransmuteTabletInventory;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.utils.Utils;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotTabletInput extends Slot {
	private TransmuteTabletInventory table;

	public SlotTabletInput(TransmuteTabletInventory table, int par2, int par3, int par4) {
		super(table, par2, par3, par4);
		this.table = table;
	}

	@Override
	public boolean isItemValid(ItemStack stack) {
		return !this.getHasStack() && Utils.doesItemHaveEmc(stack);
	}

	@Override
	public void putStack(ItemStack stack) {
		if (stack == null) {
			return;
		}

		super.putStack(stack);

		if (stack.getItem() == ObjHandler.kleinStars) {
			int remainingEmc = Utils.getKleinStarMaxEmc(stack) - (int) Math.ceil(ItemPE.getEmc(stack));

			if (table.emc >= remainingEmc) {
				ItemPE.addEmc(stack, remainingEmc);
				table.removeEmc(remainingEmc);
			} else {
				ItemPE.addEmc(stack, table.emc);
				table.emc = 0;
			}
		}

		if (stack.getItem() != ObjHandler.tome) {
			table.handleKnowledge(stack.copy());
		} else {
			table.updateOutputs();
		}
	}

	@Override
	public int getSlotStackLimit() {
		return 1;
	}
}
