package com.cellsimulation.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable 2D coordinate on the simulation grid.
 *
 * <p>A {@code Position} is defined by a row and a column index. The class
 * exposes no setter and carries no behavior beyond identity: it is a pure
 * value object.
 *
 * <p>Two positions are considered equal if and only if they share the same
 * row and column, which makes {@code Position} safe to use as a key in
 * hash-based collections such as {@link java.util.HashMap} or
 * {@link java.util.HashSet}.
 *
 * <p>No bounds validation is performed in the constructor; checking that a
 * position lies inside a given grid is the responsibility of {@code Grid}.
 */
public final class Position implements Serializable {

    private final int row;
    private final int column;

    /**
     * Creates a new position from a row and a column index.
     *
     * @param row    the row index
     * @param column the column index
     */
    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

    /**
     * Returns the row of this position on the grid.
     *
     * @return the row index
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns the column of this position on the grid.
     *
     * @return the column index
     */
    public int getColumn() {
        return column;
    }

    /**
     * Indicates whether another object is a {@code Position} with the same
     * row and column as this one.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if {@code obj} is a {@code Position} with identical
     *         row and column, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Position)) {
            return false;
        }
        Position other = (Position) obj;
        return row == other.row && column == other.column;
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}, derived
     * from the row and column.
     *
     * @return the hash code of this position
     */
    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    /**
     * Returns a human-readable representation of this position, useful for
     * debugging and logging.
     *
     * @return a string of the form {@code "(row, column)"}
     */
    @Override
    public String toString() {
        return "(" + row + ", " + column + ")";
    }
}
