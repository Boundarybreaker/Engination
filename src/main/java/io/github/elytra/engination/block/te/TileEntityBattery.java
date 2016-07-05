/*
 * MIT License
 *
 * Copyright (c) 2016 Isaac Ellingson (Falkreon)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.elytra.engination.block.te;

import com.google.common.primitives.Ints;

import io.github.elytra.engination.energy.RedstoneFlux;
import io.github.elytra.engination.energy.RedstoneFluxAccess;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;

public class TileEntityBattery extends TileEntityMachineBase implements cofh.api.energy.IEnergyHandler, ITickable {
	private TileEntity downTile = null;
	private RedstoneFluxAccess downAccess = null;
	
	@SuppressWarnings("deprecation")
	public TileEntityBattery() {
		energy.setCapacity(2_000_000);
		energy.setRateIn(1_000);
		energy.setRateOut(1_000);
	}
	
	@Override
	public boolean canConnectEnergy(EnumFacing from) {
		return (from==EnumFacing.UP || from==EnumFacing.DOWN);
	}

	@Override
	public int extractEnergy(EnumFacing from, int maxExtract, boolean simulate) {
		if (from!=EnumFacing.DOWN) return 0;
		return Ints.saturatedCast(energy.extractEnergy(Ints.saturatedCast(maxExtract), simulate));
	}

	@Override
	public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {
		if (from!=EnumFacing.UP) return 0;
		return Ints.saturatedCast(energy.insertEnergy(Ints.saturatedCast(maxReceive), simulate));
	}

	@Override
	public int getEnergyStored(EnumFacing from) {
		return Ints.saturatedCast(energy.getEnergy());
	}

	@Override
	public int getMaxEnergyStored(EnumFacing from) {
		return Ints.saturatedCast(energy.getCapacity());
	}
	
	public float getStoragePercent() {
		//A bit of type trickery here. Longs might lose some precision casting to doubles, but since the large numbers are relative to each other an ULP will never be close to 1/100th.
		//doubles might even be overkill for this.
		return (float)( (double)energy.getEnergy() / (double)energy.getCapacity() );
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (capability==null) return false;
		
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ||
			capability == RedstoneFlux.CAPABILITY_CORE_ENERGY) return true;
		
		if (capability == RedstoneFlux.TESLA_ENERGY_PRODUCER) {
			return facing==EnumFacing.DOWN;
		}
		
		if (capability == RedstoneFlux.TESLA_ENERGY_CONSUMER ||
			capability == RedstoneFlux.TESLA_ENERGY_STORAGE) {
			return facing==EnumFacing.UP;
		}
		
		return super.hasCapability(capability, facing);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability==null) return null;
		

		if (capability == RedstoneFlux.CAPABILITY_CORE_ENERGY) {
			return (T) energy.getCapabilityCoreWrapper();
		}
		
		if (capability == RedstoneFlux.TESLA_ENERGY_PRODUCER && facing==EnumFacing.DOWN) {
			return (T) energy.getTeslaWrapper();
		}
		
		if (capability == RedstoneFlux.TESLA_ENERGY_CONSUMER ||
			capability == RedstoneFlux.TESLA_ENERGY_STORAGE) {
			if (facing==EnumFacing.UP) return (T) energy.getTeslaWrapper();
		}
		
		return super.getCapability(capability, facing);
	}

	@Override
	public void update() {
		super.update();
		if (worldObj.isRemote) return;
		if (this.pos.getY()==0) return; //Don't push down!
		
		pushEnergyDown();
	}
	
	public void pushEnergyDown() {
		BlockPos neighbor = pos.down();
		TileEntity te = worldObj.getTileEntity(neighbor);
		if (te==null) return;
		if (te!=downTile) {
			downTile = te;
			downAccess = RedstoneFlux.getAccess(worldObj, neighbor, EnumFacing.UP);
		}
		
		if (downAccess==RedstoneFlux.NULL_ACCESS) {
			return;
		}

		
		long simulatedEnergyRemoved = downAccess.insertEnergy(energy.getEnergy(), true);
		if (simulatedEnergyRemoved<=0) return;
		long pulledFromReserves = energy.extractEnergy(simulatedEnergyRemoved, false);
		long actualEnergyRemoved = downAccess.insertEnergy(pulledFromReserves, false);
		
		this.markDirty();
		//IBlockState curState = worldObj.getBlockState(pos);
		//worldObj.notifyBlockUpdate(pos, curState, curState, 3);
		
		if (actualEnergyRemoved!=pulledFromReserves) {
			//TODO: Ideally, mark this tile visually as malfunctioning via IBlockState.
			//There's not much I could do if this turned out to be dropping RF, but it'd be nice to know.
		}
	}
}
