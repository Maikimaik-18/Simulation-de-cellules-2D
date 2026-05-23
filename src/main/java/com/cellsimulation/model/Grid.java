package com.cellsimulation.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.cellsimulation.neighborhood.NeighborhoodStrategy;

/**
 * Two-dimensional playground hosting the simulated population.
 *
 * <p>A {@code Grid} owns a rectangular array of cells, each of which may hold
 * a single {@link Person} or be empty ({@code null}). It also remembers
 * whether the world wraps around its edges (toric mode) or is bounded.
 *
 * <p>In <em>toric</em> mode every position is valid: row and column indices
 * are mapped back into range using {@link Math#floorMod(int, int)} so that
 * negative or out-of-range coordinates wrap consistently. In <em>bounded</em>
 * mode positions that fall outside the rectangle are either ignored
 * ({@link #getCell(Position)} returns {@code null}) or rejected
 * ({@link #setCell(Position, Person)} throws).
 *
 * <p>The class is {@link Serializable} so that the whole simulation state
 * can be persisted to disk through the {@code SaveService}.
 */
public class Grid implements Serializable {

    private final int width;
    private final int height;
    private final boolean toricMode;
    private final Person[][] cells;

    /**
     * Creates a new empty grid with the given dimensions and edge-wrapping
     * policy.
     *
     * @param width     the number of columns; must be greater than or equal
     *                  to {@code 1}
     * @param height    the number of rows; must be greater than or equal to
     *                  {@code 1}
     * @param toricMode {@code true} if the grid wraps around its edges,
     *                  {@code false} for a bounded rectangle
     * @throws IllegalArgumentException if {@code width < 1} or
     *                                  {@code height < 1}
     */
    public Grid(int width, int height, boolean toricMode) {
        if (width < 1) {
            throw new IllegalArgumentException(
                    "width must be >= 1, got " + width);
        }
        if (height < 1) {
            throw new IllegalArgumentException(
                    "height must be >= 1, got " + height);
        }
        this.width = width;
        this.height = height;
        this.toricMode = toricMode;
        this.cells = new Person[height][width];
    }

    /**
     * @return the number of columns of this grid
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the number of rows of this grid
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return {@code true} if this grid wraps around its edges,
     *         {@code false} if it is bounded
     */
    public boolean isToricMode() {
        return toricMode;
    }

    /**
     * Returns the person occupying the given position, or {@code null} if
     * the cell is empty.
     *
     * <p>In toric mode the coordinates of {@code position} are wrapped with
     * {@link Math#floorMod(int, int)} before the lookup. In bounded mode a
     * position lying outside the rectangle yields {@code null} rather than
     * an exception, which keeps neighbor scans simple.
     *
     * @param position the position to read
     * @return the person at the (possibly wrapped) position, or {@code null}
     *         if the cell is empty or out of bounds in non-toric mode
     */
    public Person getCell(Position position) {
        if (toricMode) {
            int row = Math.floorMod(position.getRow(), height);
            int column = Math.floorMod(position.getColumn(), width);
            return cells[row][column];
        }
        if (!isInside(position)) {
            return null;
        }
        return cells[position.getRow()][position.getColumn()];
    }

    /**
     * Places (or removes) a person at the given position.
     *
     * <p>Passing {@code null} as {@code person} empties the cell. In toric
     * mode the coordinates of {@code position} are wrapped with
     * {@link Math#floorMod(int, int)}. In bounded mode an out-of-bounds
     * position is rejected.
     *
     * @param position the target position
     * @param person   the person to place, or {@code null} to empty the cell
     * @throws IllegalArgumentException in bounded mode, if {@code position}
     *                                  lies outside the grid
     */
    public void setCell(Position position, Person person) {
        if (toricMode) {
            int row = Math.floorMod(position.getRow(), height);
            int column = Math.floorMod(position.getColumn(), width);
            cells[row][column] = person;
            return;
        }
        if (!isInside(position)) {
            throw new IllegalArgumentException(
                    "position " + position + " is outside the grid "
                            + width + "x" + height);
        }
        cells[position.getRow()][position.getColumn()] = person;
    }

    /**
     * Empties the cell at the given position.
     *
     * <p>Equivalent to {@code setCell(position, null)} and obeys the same
     * coordinate policy: wrap in toric mode, reject out-of-bounds positions
     * in bounded mode.
     *
     * @param position the position to clear
     * @throws IllegalArgumentException in bounded mode, if {@code position}
     *                                  lies outside the grid
     */
    public void removeCell(Position position) {
        setCell(position, null);
    }

    /**
     * Indicates whether the given position lies inside this grid.
     *
     * <p>In toric mode every position is considered inside since the grid
     * wraps around. In bounded mode the position must satisfy
     * {@code 0 <= row < height} and {@code 0 <= column < width}.
     *
     * @param position the position to test
     * @return {@code true} if the position is inside the grid,
     *         {@code false} otherwise
     */
    public boolean isInside(Position position) {
        if (toricMode) {
            return true;
        }
        int row = position.getRow();
        int column = position.getColumn();
        return row >= 0 && row < height && column >= 0 && column < width;
    }

    /**
     * Returns the empty cells among the neighbors of the given position,
     * according to the supplied strategy.
     *
     * <p>The strategy first lists candidate neighbor positions; this method
     * then filters them by keeping only those whose corresponding cell is
     * empty ({@code getCell(pos) == null}).
     *
     * @param position     the reference position
     * @param neighborhood the strategy used to enumerate the neighbors
     * @return the list of empty neighbor positions; never {@code null},
     *         possibly empty
     */
    public List<Position> getEmptyNeighbors(Position position, NeighborhoodStrategy neighborhood) {
        List<Position> candidates = neighborhood.getNeighbors(this, position);
        List<Position> empties = new ArrayList<>(candidates.size());
        for (Position candidate : candidates) {
            if (getCell(candidate) == null) {
                empties.add(candidate);
            }
        }
        return empties;
    }
}
