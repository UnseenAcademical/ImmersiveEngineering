/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.api.crafting;

import blusunrize.immersiveengineering.api.ApiUtils;
import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author BluSunrize - 20.02.2016
 * <p>
 * The recipe for the Squeezer
 */
public class MixerRecipe extends MultiblockRecipe
{
	public static float energyModifier = 1;
	public static float timeModifier = 1;

	public final IngredientStack[] itemInputs;
	public final FluidStack fluidInput;
	public final FluidStack fluidOutput;
	public final int fluidAmount;

	public MixerRecipe(FluidStack fluidOutput, FluidStack fluidInput, Object[] itemInputs, int energy)
	{
		this.fluidOutput = fluidOutput;
		this.fluidAmount = fluidOutput.getAmount();
		this.fluidInput = fluidInput;
		this.itemInputs = new IngredientStack[itemInputs==null?0: itemInputs.length];
		if(itemInputs!=null)
			for(int i = 0; i < itemInputs.length; i++)
				this.itemInputs[i] = ApiUtils.createIngredientStack(itemInputs[i]);
		this.totalProcessEnergy = (int)Math.floor(energy*energyModifier);
		this.totalProcessTime = (int)Math.floor(fluidOutput.getAmount()*timeModifier);

		this.fluidInputList = Lists.newArrayList(this.fluidInput);
		this.inputList = Lists.newArrayList(this.itemInputs);
		this.fluidOutputList = Lists.newArrayList(this.fluidOutput);
	}

	public static ArrayList<MixerRecipe> recipeList = new ArrayList<>();

	public static MixerRecipe addRecipe(FluidStack fluidOutput, FluidStack fluidInput, Object[] itemInput, int energy)
	{
		MixerRecipe r = new MixerRecipe(fluidOutput, fluidInput, itemInput, energy);
		recipeList.add(r);
		return r;
	}

	public static MixerRecipe findRecipe(FluidStack fluid, NonNullList<ItemStack> components)
	{
		if(fluid==null)
			return null;
		for(MixerRecipe recipe : recipeList)
			if(recipe.matches(fluid, components))
				return recipe;
		return null;
	}

	public FluidStack getFluidOutput(FluidStack input, NonNullList<ItemStack> components)
	{
		return this.fluidOutput;
	}

	public boolean matches(FluidStack fluid, NonNullList<ItemStack> components)
	{
		return compareToInputs(fluid, components, this.fluidInput, this.itemInputs);
	}

	protected boolean compareToInputs(FluidStack fluid, NonNullList<ItemStack> components, FluidStack fluidInput, IngredientStack[] itemInputs)
	{
		if(fluid!=null&&fluid.containsFluid(fluidInput))
		{
			ArrayList<ItemStack> queryList = new ArrayList<ItemStack>(components.size());
			for(ItemStack s : components)
				if(!s.isEmpty())
					queryList.add(s.copy());

			for(IngredientStack add : itemInputs)
				if(add!=null)
				{
					int addAmount = add.inputSize;
					Iterator<ItemStack> it = queryList.iterator();
					while(it.hasNext())
					{
						ItemStack query = it.next();
						if(!query.isEmpty())
						{
							if(add.matches(query))
								if(query.getCount() > addAmount)
								{
									query.shrink(addAmount);
									addAmount = 0;
								}
								else
								{
									addAmount -= query.getCount();
									query.setCount(0);
								}
							if(query.getCount() <= 0)
								it.remove();
							if(addAmount <= 0)
								break;
						}
					}
					if(addAmount > 0)
						return false;
				}
			return true;
		}
		return false;
	}


	public int[] getUsedSlots(FluidStack input, NonNullList<ItemStack> components)
	{
		Set<Integer> usedSlotSet = new HashSet<Integer>();
		for(int i = 0; i < itemInputs.length; i++)
		{
			IngredientStack ingr = itemInputs[i];
			for(int j = 0; j < components.size(); j++)
				if(!usedSlotSet.contains(j)&&!components.get(j).isEmpty()&&ingr.matchesItemStack(components.get(j)))
				{
					usedSlotSet.add(j);
					break;
				}
		}
		int it = 0;
		int[] processSlots = new int[usedSlotSet.size()];
		for(Integer slot : usedSlotSet)
			processSlots[it++] = slot;
		return processSlots;
	}

	@Override
	public int getMultipleProcessTicks()
	{
		return 0;
	}

	@Override
	public CompoundNBT writeToNBT(CompoundNBT nbt)
	{
		nbt.put("fluidInput", fluidInput.writeToNBT(new CompoundNBT()));
		if(this.itemInputs.length > 0)
		{
			ListNBT list = new ListNBT();
			for(IngredientStack add : this.itemInputs)
				list.add(add.writeToNBT(new CompoundNBT()));
			nbt.put("itemInputs", list);
		}
		return nbt;
	}

	@Nullable
	public static MixerRecipe loadFromNBT(CompoundNBT nbt)
	{
		FluidStack fluidInput = FluidStack.loadFluidStackFromNBT(nbt.getCompound("fluidInput"));
		IngredientStack[] itemInputs = null;
		if(nbt.contains("itemInputs", NBT.TAG_LIST))
		{
			ListNBT list = nbt.getList("itemInputs", NBT.TAG_COMPOUND);
			itemInputs = new IngredientStack[list.size()];
			for(int i = 0; i < itemInputs.length; i++)
				itemInputs[i] = IngredientStack.readFromNBT(list.getCompound(i));
		}
		for(MixerRecipe recipe : recipeList)
			if(recipe.fluidInput.equals(fluidInput))
			{
				if(itemInputs==null&&recipe.itemInputs.length < 1)
					return recipe;
				else if(itemInputs!=null&&recipe.itemInputs.length==itemInputs.length)
				{
					boolean b = true;
					for(int i = 0; i < itemInputs.length; i++)
						if(!itemInputs[i].equals(recipe.itemInputs[i]))
						{
							b = false;
							break;
						}
					if(b)
						return recipe;
				}
			}
		return null;
	}

	@Override
	public boolean shouldCheckItemAvailability()
	{
		return false;
	}
}