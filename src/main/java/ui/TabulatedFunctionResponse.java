package ui;

import functions.Insertable;
import functions.Point;
import functions.Removable;
import functions.TabulatedFunction;

import java.util.ArrayList;
import java.util.List;

public class TabulatedFunctionResponse {
    private final String name;
    private final String storage;
    private final double leftBound;
    private final double rightBound;
    private final List<Point> points;
    private final boolean insertable;
    private final boolean removable;

    private TabulatedFunctionResponse(String name, String storage, double leftBound, double rightBound, List<Point> points,
                                      boolean insertable, boolean removable) {
        this.name = name;
        this.storage = storage;
        this.leftBound = leftBound;
        this.rightBound = rightBound;
        this.points = points;
        this.insertable = insertable;
        this.removable = removable;
    }

    public static TabulatedFunctionResponse from(String name, String storage, TabulatedFunction function, int maxPoints) {
        List<Point> normalized = new ArrayList<>();
        int limit = Math.min(function.getCount(), maxPoints);
        int stride = function.getCount() <= limit ? 1 : (int) Math.ceil((double) function.getCount() / limit);
        for (int i = 0; i < function.getCount(); i += stride) {
            normalized.add(new Point(function.getX(i), function.getY(i)));
        }
        if (normalized.size() < limit && function.getCount() > 0) {
            Point last = new Point(function.getX(function.getCount() - 1), function.getY(function.getCount() - 1));
            if (normalized.isEmpty() || normalized.get(normalized.size() - 1).x != last.x) {
                normalized.add(last);
            }
        }
        boolean canInsert = function instanceof Insertable;
        boolean canRemove = function instanceof Removable;
        return new TabulatedFunctionResponse(name, storage, function.leftBound(), function.rightBound(), normalized,
                canInsert, canRemove);
    }

    public String getName() {
        return name;
    }

    public String getStorage() {
        return storage;
    }

    public double getLeftBound() {
        return leftBound;
    }

    public double getRightBound() {
        return rightBound;
    }

    public List<Point> getPoints() {
        return points;
    }
    public boolean isInsertable() {
        return insertable;
    }

    public boolean isRemovable() {
        return removable;
    }
}