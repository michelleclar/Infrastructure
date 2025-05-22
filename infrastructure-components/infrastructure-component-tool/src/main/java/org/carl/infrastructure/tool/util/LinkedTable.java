package org.carl.infrastructure.tool.util;

import org.carl.infrastructure.tool.annotations.NotThreadSafe;

public class LinkedTable<T> {
    private final Node<T>[] nodes;
    private int size;
    private int head;

    @SuppressWarnings("unchecked")
    public LinkedTable(int capacity) {
        nodes = new Node[capacity];
        size = 0;
        head = -1;
    }

    @NotThreadSafe
    public void insert(T value) {
        if (size >= nodes.length) {
            throw new ArrayIndexOutOfBoundsException("Table is full");
        }
        Node<T> tNode = new Node<>(value);
        nodes[size] = tNode;
        if (head == -1) {
            head = 0;
        } else {
            nodes[size - 1].next = size;
        }
        size++;
    }

    @Override
    public String toString() {
        int current = head;
        StringBuilder str = new StringBuilder();
        while (current != -1) {
            str.append(nodes[current].value);
            current = nodes[current].next;
            if (current != -1) {
                str.append(" -> ");
            }
        }
        return str.toString();
    }

    public static class Node<T> {
        private final T value;
        private int next = -1;

        public Node(T value) {
            this.value = value;
        }
    }
}
