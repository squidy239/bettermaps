package net.fractalinfinity.bettermaps;

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class KDTree3D<T> {
    private Node<T> root;
    private final ReadWriteLock lock;

    public KDTree3D() {
        root = null;
        lock = new ReentrantReadWriteLock();
    }

    private static class Node<T> {
        int[] point;
        T data;
        Node<T> left, right;

        Node(int[] point, T data) {
            this.point = point;
            this.data = data;
            this.left = this.right = null;
        }
    }

    public void insert(int[] point, T data) {
        if (point == null || point.length != 3 || !isValidPoint(point)) {
            throw new IllegalArgumentException("Point must be a 3D array with values between 0 and 255 inclusive, the point was "+ Arrays.toString(point));
        }

        lock.writeLock().lock();
        try {
            root = insert(root, point, data, 0);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Node<T> insert(Node<T> node, int[] point, T data, int depth) {
        if (node == null) {
            return new Node<>(point, data);
        }

        int axis = depth % 3;
        if (point[axis] <= node.point[axis]) {
            node.left = insert(node.left, point, data, depth + 1);
        } else {
            node.right = insert(node.right, point, data, depth + 1);
        }

        return node;
    }

    public T findNearest(int[] target) {
        if (target == null || target.length != 3 || !isValidPoint(target)) {
            throw new IllegalArgumentException("Target must be a 3D array with values between 0 and 255 inclusive, the target was: "+Arrays.toString(target));
        }

        lock.readLock().lock();
        try {
            if (root == null) {
                return null;
            }
            return findNearest(root, target, 0).data;
        } finally {
            lock.readLock().unlock();
        }
    }

    private Node<T> findNearest(Node<T> node, int[] target, int depth) {
        if (node == null) {
            return null;
        }

        int axis = depth % 3;
        Node<T> nextBranch = target[axis] <= node.point[axis] ? node.left : node.right;
        Node<T> otherBranch = target[axis] <= node.point[axis] ? node.right : node.left;

        Node<T> best = findNearest(nextBranch, target, depth + 1);

        if (best == null || squaredDistance(node.point, target) < squaredDistance(best.point, target)) {
            best = node;
        }

        if (otherBranch != null) {
            int axisDist = target[axis] - node.point[axis];
            if (axisDist * axisDist <= squaredDistance(best.point, target)) {
                Node<T> otherBest = findNearest(otherBranch, target, depth + 1);
                if (otherBest != null && squaredDistance(otherBest.point, target) < squaredDistance(best.point, target)) {
                    best = otherBest;
                }
            }
        }

        return best;
    }

    private int squaredDistance(int[] p1, int[] p2) {
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            int diff = p1[i] - p2[i];
            sum += diff * diff;
        }
        return sum;
    }

    private boolean isValidPoint(int[] point) {
        for (int coord : point) {
            if (coord < 0 || coord > 255) {
                return false;
            }
        }
        return true;
    }
}