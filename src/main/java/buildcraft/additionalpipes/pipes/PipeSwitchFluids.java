package buildcraft.additionalpipes.pipes;

import net.minecraft.item.Item;
import buildcraft.transport.PipeTransportFluids;


public class PipeSwitchFluids extends PipeSwitch {

	public PipeSwitchFluids(Item item) {
		super(new PipeTransportFluids(), item, 22);
	}

}
