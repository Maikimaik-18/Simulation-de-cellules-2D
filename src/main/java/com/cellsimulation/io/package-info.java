/**
 * Persistence layer: save and load simulation state to and from disk.
 *
 * <p>Uses Java serialization ({@link java.io.ObjectOutputStream} /
 * {@link java.io.ObjectInputStream}). All persisted types in the model
 * package must implement {@link java.io.Serializable}.
 */
package com.cellsimulation.io;
