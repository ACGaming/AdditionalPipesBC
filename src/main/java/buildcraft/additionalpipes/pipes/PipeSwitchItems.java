package buildcraft.additionalpipes.pipes;

import net.minecraft.item.Item;
import buildcraft.transport.PipeTransportItems;

public class PipeSwitchItems extends PipeSwitch {

	public PipeSwitchItems(Item item) {
		super(new PipeTransportItems(), item, 20);
	}

}
