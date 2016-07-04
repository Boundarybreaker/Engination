package io.github.elytra.engination.energy;

import com.google.common.primitives.Ints;

import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import cofh.api.energy.IEnergyStorage;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

/**
 * This class is very literally {@link gigiaherz.capabilities.api.energy.IEnergyHandler} extended out to longs.
 * 
 * CapabilityCore's maximum instantaneous energy capacity is not disgustingly large like Tesla's is.
 */
public interface RedstoneFluxAccess {
	/**
     * Obtains the maximum contained energy.
     *
     * @return the current capacity.
     */
	public long getCapacity();
	
	/**
     * Obtains the current contained energy.
     *
     * @return the current energy level.
     */
	public long getEnergy();
	
	/**
     * Attempts to extract the specified amount of energy.
     *
     * @param maxExtract The maximum amount of energy to extract.
     * @param simulate   If true, the energy is not subtracted from the buffer.
     * @return The energy actually extracted.
     */
	long extractEnergy(long maxExtract, boolean simulate);
	
	/**
     * Attempts to insert the specified amount of energy.
     *
     * @param maxReceive The maximum amount of energy to insert.
     * @param simulate   If true, the energy is not added to the buffer.
     * @return The energy actually inserted.
     */
	long insertEnergy(long maxReceive, boolean simulate);
	
	
	public static class NullWrapper implements RedstoneFluxAccess {
		@Override
		public long getCapacity() { return 0; }

		@Override
		public long getEnergy() { return 0; }

		@Override
		public long extractEnergy(long maxExtract, boolean simulate) {
			return 0;
		}

		@Override
		public long insertEnergy(long maxReceive, boolean simulate) {
			return 0;
		}
	}
	
	public static class CapabilityWrapper implements RedstoneFluxAccess {
		private final gigaherz.capabilities.api.energy.IEnergyHandler delegate;
		
		public CapabilityWrapper(gigaherz.capabilities.api.energy.IEnergyHandler delegate) {
			this.delegate = delegate;
		}

		@Override
		public long getCapacity() {
			return delegate.getCapacity();
		}

		@Override
		public long getEnergy() {
			return delegate.getEnergy();
		}

		@Override
		public long extractEnergy(long maxExtract, boolean simulate) {
			return delegate.extractEnergy(Ints.saturatedCast(maxExtract), simulate);
		}

		@Override
		public long insertEnergy(long maxReceive, boolean simulate) {
			return delegate.insertEnergy(Ints.saturatedCast(maxReceive), simulate);
		}
	}
	
	public static class TeslaWrapper implements RedstoneFluxAccess {
		private net.darkhax.tesla.api.ITeslaHolder holder = null;
		private net.darkhax.tesla.api.ITeslaProducer producer = null;
		private net.darkhax.tesla.api.ITeslaConsumer consumer = null;
		
		public TeslaWrapper(TileEntity tile, EnumFacing side) {
			if (tile.hasCapability(RedstoneFlux.TESLA_ENERGY_STORAGE, side)) holder = tile.getCapability(RedstoneFlux.TESLA_ENERGY_STORAGE, side);
			if (tile.hasCapability(RedstoneFlux.TESLA_ENERGY_PRODUCER, side)) producer = tile.getCapability(RedstoneFlux.TESLA_ENERGY_PRODUCER, side);
			if (tile.hasCapability(RedstoneFlux.TESLA_ENERGY_CONSUMER, side)) consumer = tile.getCapability(RedstoneFlux.TESLA_ENERGY_CONSUMER, side);
			
			
		}

		@Override
		public long getCapacity() {
			return holder.getCapacity();
		}

		@Override
		public long getEnergy() {
			return holder.getStoredPower();
		}

		@Override
		public long extractEnergy(long maxExtract, boolean simulate) {
			if (producer!=null) return producer.takePower(maxExtract, simulate);
			else return 0L;
		}

		@Override
		public long insertEnergy(long maxReceive, boolean simulate) {
			if (consumer!=null) return consumer.givePower(maxReceive, simulate);
			else return 0L;
		}
	}
	
	public static class FluxWrapper implements RedstoneFluxAccess {
		private TileEntity delegate;
		private EnumFacing side;
		
		public FluxWrapper(TileEntity tile, EnumFacing side) {
			this.delegate = tile;
			this.side = side;
		}

		@Override
		public long getCapacity() {
			if (this.delegate instanceof IEnergyStorage) {
				return ((IEnergyStorage) delegate).getMaxEnergyStored();
			} else return 0;
		}

		@Override
		public long getEnergy() {
			if (this.delegate instanceof IEnergyProvider) {
				return ((IEnergyProvider)delegate).getEnergyStored(side);
			} else return 0;
		}

		@Override
		public long extractEnergy(long maxExtract, boolean simulate) {
			if (this.delegate instanceof IEnergyProvider) {
				return ((IEnergyProvider) delegate).extractEnergy(side, Ints.saturatedCast(maxExtract), simulate);
			} else return 0;
		}

		@Override
		public long insertEnergy(long maxReceive, boolean simulate) {
			if (this.delegate instanceof IEnergyReceiver) {
				return ((IEnergyReceiver) delegate).receiveEnergy(side, Ints.saturatedCast(maxReceive), simulate);
			} else return 0;
		}
	}
}