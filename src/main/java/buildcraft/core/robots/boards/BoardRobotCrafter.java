/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.core.robots.boards;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;

import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import buildcraft.api.boards.RedstoneBoardRobot;
import buildcraft.api.boards.RedstoneBoardRobotNBT;
import buildcraft.api.gates.ActionParameterItemStack;
import buildcraft.api.gates.IActionParameter;
import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.api.robots.IDockingStation;
import buildcraft.core.inventory.StackHelper;
import buildcraft.core.robots.AIRobotCraftFurnace;
import buildcraft.core.robots.AIRobotCraftGeneric;
import buildcraft.core.robots.AIRobotCraftWorkbench;
import buildcraft.core.robots.AIRobotGotoSleep;
import buildcraft.core.robots.AIRobotGotoStationToUnload;
import buildcraft.core.robots.AIRobotSleep;
import buildcraft.core.robots.AIRobotUnload;
import buildcraft.core.robots.DockingStation;
import buildcraft.silicon.statements.ActionRobotCraft;
import buildcraft.transport.gates.ActionIterator;
import buildcraft.transport.gates.ActionSlot;

public class BoardRobotCrafter extends RedstoneBoardRobot {

	private ItemStack order;
	private ArrayList<ItemStack> craftingBlacklist = new ArrayList<ItemStack>();
	private HashSet<IDockingStation> reservedStations = new HashSet<IDockingStation>();

	public BoardRobotCrafter(EntityRobotBase iRobot) {
		super(iRobot);
	}

	@Override
	public RedstoneBoardRobotNBT getNBTHandler() {
		return BoardRobotCrafterNBT.instance;
	}

	@Override
	public void update() {
		if (robot.containsItems()) {
			// Always makes sure that when starting a craft, the inventory is
			// clean.

			// TODO: We should call load or drop, in order to clean items even
			// if no destination is to be found
			startDelegateAI(new AIRobotGotoStationToUnload(robot, robot.getZoneToWork()));
			return;
		}

		order = getCraftingOrder();

		if (order == null) {
			craftingBlacklist.clear();
			startDelegateAI(new AIRobotSleep(robot));
			return;
		}

		IRecipe recipe = lookForWorkbenchRecipe(order);

		if (recipe != null) {
			startDelegateAI(new AIRobotCraftWorkbench(robot, recipe));
			return;
		}

		ItemStack furnaceInput = lookForFurnaceRecipe(order);

		if (furnaceInput != null) {
			startDelegateAI(new AIRobotCraftFurnace(robot, furnaceInput));
		}

		/*
		 * recipe = lookForAssemblyTableRecipe(order);
		 *
		 * if (recipe != null) { startDelegateAI(new
		 * AIRobotCraftAssemblyTable(robot)); }
		 *
		 * recipe = lookForIntegrationTableRecipe(order);
		 *
		 * if (recipe != null) { startDelegateAI(new
		 * AIRobotCraftIntegrationTable(robot)); }
		 */

		craftingBlacklist.add(order);
	}

	@Override
	public void delegateAIEnded(AIRobot ai) {
		if (ai instanceof AIRobotCraftGeneric) {
			if (!ai.success()) {
				craftingBlacklist.add(order);
			} else {
				// The extra crafted items may make some crafting possible
				craftingBlacklist.clear();
			}
		} else if (ai instanceof AIRobotGotoStationToUnload) {
			if (ai.success()) {
				startDelegateAI(new AIRobotUnload(robot));
			} else {
				startDelegateAI(new AIRobotGotoSleep(robot));
			}
		}
	}

	private IRecipe lookForWorkbenchRecipe(ItemStack order) {
		for (Object o : CraftingManager.getInstance().getRecipeList()) {
			IRecipe r = (IRecipe) o;

			if (r instanceof ShapedRecipes
					|| r instanceof ShapelessRecipes
					|| r instanceof ShapedOreRecipe
					|| r instanceof ShapelessOreRecipe) {
				if (StackHelper.isMatchingItem(r.getRecipeOutput(), order)) {
					return r;
				}
			}
		}

		return null;
	}

	private ItemStack lookForFurnaceRecipe(ItemStack order) {
		for (Object o : FurnaceRecipes.smelting().getSmeltingList().entrySet()) {
			Map.Entry e = (Map.Entry) o;
			ItemStack input = (ItemStack) e.getKey();
			ItemStack output = (ItemStack) e.getValue();

			if (StackHelper.isMatchingItem(output, order)) {
				return input;
			}
		}

		return null;
	}

	private boolean isBlacklisted(ItemStack stack) {
		for (ItemStack black : craftingBlacklist) {
			if (StackHelper.isMatchingItem(stack, black)) {
				return true;
			}
		}

		return false;
	}

	private ItemStack getCraftingOrder() {
		// [1] priority from the current station order

		DockingStation s = (DockingStation) robot.getLinkedStation();

		for (ActionSlot slot : new ActionIterator(s.pipe.pipe)) {
			if (slot.action instanceof ActionRobotCraft) {
				for (IActionParameter p : slot.parameters) {
					if (p != null && p instanceof ActionParameterItemStack) {
						ActionParameterItemStack param = (ActionParameterItemStack) p;
						ItemStack stack = param.getItemStackToDraw();

						if (stack != null && !isBlacklisted(stack)) {
							return stack;
						}
					}
				}
			}
		}

		// [2] if no order, will look at the "request" stations (either from
		// inventories or machines).
		// when taking a "request" order, lock the target station

		return null;
	}

}
