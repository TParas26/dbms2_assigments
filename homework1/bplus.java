// use of ChatGPT (GPT-5.6 Luna) for creating initial code. Later modified and tested thouroughly to match the requirements
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class bplus {
    static class Entry {
        int key, pointer;
        Entry(int key, int pointer) { this.key = key; this.pointer = pointer; }
    }

    static abstract class Node {
        InternalNode parent;
        abstract boolean isLeaf();
        abstract int firstKey();
    }

    static class LeafNode extends Node {
        List<Entry> entries = new ArrayList<>();
        LeafNode next, previous;
        boolean isLeaf() { return true; }
        int firstKey() { return entries.get(0).key; }
    }

    static class InternalNode extends Node {
        List<Integer> keys = new ArrayList<>();
        List<Node> children = new ArrayList<>();
        boolean isLeaf() { return false; }
        int firstKey() { return children.get(0).firstKey(); }
    }

    static class BPlusTree {
        final int d;
        Node root;
        LeafNode firstLeaf;

        BPlusTree(int d) {
            if (d < 1) throw new IllegalArgumentException("d must be at least 1.");
            this.d = d;
            root = new LeafNode();
            firstLeaf = (LeafNode) root;
        }

        int search(int key) {
            LeafNode leaf = findLeaf(key);
            for (Entry e : leaf.entries) {
                if (e.key == key) return e.pointer;
                if (e.key > key) break;
            }
            return -1;
        }

        boolean insert(int key, int pointer) {
            LeafNode leaf = findLeaf(key);
            int pos = 0;
            while (pos < leaf.entries.size() && leaf.entries.get(pos).key < key) pos++;
            if (pos < leaf.entries.size() && leaf.entries.get(pos).key == key) return false;
            leaf.entries.add(pos, new Entry(key, pointer));

            if (leaf.entries.size() > 2 * d) splitLeaf(leaf);
            else updateAncestorKeys(leaf);
            return true;
        }

        boolean delete(int key) {
            LeafNode leaf = findLeaf(key);
            int pos = -1;
            for (int i = 0; i < leaf.entries.size(); i++) {
                if (leaf.entries.get(i).key == key) { pos = i; break; }
            }
            if (pos < 0) return false;

            leaf.entries.remove(pos);
            if (leaf == root) return true;
            if (leaf.entries.size() >= d) {
                updateAncestorKeys(leaf);
                return true;
            }
            rebalanceLeaf(leaf);
            return true;
        }

        List<Entry> rangeSearch(int low, int high) {
            List<Entry> result = new ArrayList<>();
            if (low > high) return result;
            LeafNode leaf = findLeaf(low);

            while (leaf != null) {
                for (Entry e : leaf.entries) {
                    if (e.key > high) return result;
                    if (e.key >= low) result.add(new Entry(e.key, e.pointer));
                }
                leaf = leaf.next;
            }
            return result;
        }

        void printTree() {
            Deque<Node> q = new ArrayDeque<>();
            q.add(root);

            while (!q.isEmpty()) {
                int n = q.size();
                StringBuilder line = new StringBuilder();

                for (int i = 0; i < n; i++) {
                    Node node = q.removeFirst();

                    if (node.isLeaf()) {
                        LeafNode leaf = (LeafNode) node;
                        line.append("[");
                        for (int j = 0; j < leaf.entries.size(); j++) {
                            if (j > 0) line.append(" ");
                            Entry e = leaf.entries.get(j);
                            line.append("(").append(e.key).append(": ").append(e.pointer).append(")");
                        }
                        line.append("]");
                    } else {
                        InternalNode in = (InternalNode) node;
                        line.append("[");
                        for (int j = 0; j < in.keys.size(); j++) {
                            if (j > 0) line.append(" ");
                            line.append("(").append(in.keys.get(j)).append(")");
                        }
                        line.append("]");
                        for (Node child : in.children) q.addLast(child);
                    }
                    if (i < n - 1) line.append(" ");
                }
                System.out.println(line);
            }
        }

        void printStatistics() {
            System.out.println("Tree Height: " + height());
            System.out.println("Node Count: " + countNodes(root));
            System.out.println("Key Count: " + countKeys());
        }

        LeafNode findLeaf(int key) {
            Node cur = root;
            while (!cur.isLeaf()) {
                InternalNode in = (InternalNode) cur;
                int i = 0;
                while (i < in.keys.size() && key >= in.keys.get(i)) i++;
                cur = in.children.get(i);
            }
            return (LeafNode) cur;
        }

        void splitLeaf(LeafNode leaf) {
            LeafNode right = new LeafNode();
            int split = leaf.entries.size() / 2;
            while (leaf.entries.size() > split) right.entries.add(leaf.entries.remove(split));

            right.next = leaf.next;
            right.previous = leaf;
            if (leaf.next != null) leaf.next.previous = right;
            leaf.next = right;

            if (leaf.parent == null) {
                InternalNode newRoot = new InternalNode();
                newRoot.children.add(leaf);
                newRoot.children.add(right);
                newRoot.keys.add(right.firstKey());
                leaf.parent = newRoot;
                right.parent = newRoot;
                root = newRoot;
                return;
            }

            InternalNode parent = leaf.parent;
            int index = parent.children.indexOf(leaf);
            parent.children.add(index + 1, right);
            parent.keys.add(index, right.firstKey());
            right.parent = parent;

            if (parent.keys.size() > 2 * d) splitInternal(parent);
            else updateAncestorKeys(parent);
        }

        void splitInternal(InternalNode node) {
            int middle = d;
            int promoted = node.keys.get(middle);
            InternalNode right = new InternalNode();

            for (int i = middle + 1; i < node.keys.size(); i++) right.keys.add(node.keys.get(i));
            for (int i = middle + 1; i < node.children.size(); i++) {
                Node child = node.children.get(i);
                right.children.add(child);
                child.parent = right;
            }

            while (node.keys.size() > middle) node.keys.remove(node.keys.size() - 1);
            while (node.children.size() > middle + 1) node.children.remove(node.children.size() - 1);

            if (node.parent == null) {
                InternalNode newRoot = new InternalNode();
                newRoot.keys.add(promoted);
                newRoot.children.add(node);
                newRoot.children.add(right);
                node.parent = newRoot;
                right.parent = newRoot;
                root = newRoot;
                return;
            }

            InternalNode parent = node.parent;
            int index = parent.children.indexOf(node);
            parent.children.add(index + 1, right);
            parent.keys.add(index, promoted);
            right.parent = parent;

            if (parent.keys.size() > 2 * d) splitInternal(parent);
            else updateAncestorKeys(parent);
        }

        void rebalanceLeaf(LeafNode leaf) {
            InternalNode parent = leaf.parent;
            int index = parent.children.indexOf(leaf);

            LeafNode left = index > 0 ? (LeafNode) parent.children.get(index - 1) : null;
            LeafNode right = index + 1 < parent.children.size()
                    ? (LeafNode) parent.children.get(index + 1) : null;

            if (left != null && left.entries.size() > d) {
                leaf.entries.add(0, left.entries.remove(left.entries.size() - 1));
                parent.keys.set(index - 1, leaf.firstKey());
                updateAncestorKeys(parent);
                return;
            }

            if (right != null && right.entries.size() > d) {
                leaf.entries.add(right.entries.remove(0));
                parent.keys.set(index, right.firstKey());
                if (index > 0) parent.keys.set(index - 1, leaf.firstKey());
                updateAncestorKeys(parent);
                return;
            }

            if (left != null) {
                left.entries.addAll(leaf.entries);
                left.next = leaf.next;
                if (leaf.next != null) leaf.next.previous = left;
                parent.children.remove(index);
                parent.keys.remove(index - 1);
                updateAncestorKeys(parent);
                handleInternalUnderflow(parent);
            } else if (right != null) {
                leaf.entries.addAll(right.entries);
                leaf.next = right.next;
                if (right.next != null) right.next.previous = leaf;
                parent.children.remove(index + 1);
                parent.keys.remove(index);
                updateAncestorKeys(parent);
                handleInternalUnderflow(parent);
            }
        }

        void handleInternalUnderflow(InternalNode node) {
            if (node == root) {
                if (node.children.size() == 1) {
                    root = node.children.get(0);
                    root.parent = null;
                }
                return;
            }
            if (node.keys.size() >= d) {
                updateAncestorKeys(node);
                return;
            }
            rebalanceInternal(node);
        }

        void rebalanceInternal(InternalNode node) {
            InternalNode parent = node.parent;
            int index = parent.children.indexOf(node);

            InternalNode left = index > 0 ? (InternalNode) parent.children.get(index - 1) : null;
            InternalNode right = index + 1 < parent.children.size()
                    ? (InternalNode) parent.children.get(index + 1) : null;

            if (left != null && left.keys.size() > d) {
                int separator = parent.keys.get(index - 1);
                Node child = left.children.remove(left.children.size() - 1);
                int borrowedSeparator = left.keys.remove(left.keys.size() - 1);
                node.children.add(0, child);
                child.parent = node;
                node.keys.add(0, separator);
                parent.keys.set(index - 1, borrowedSeparator);
                updateAncestorKeys(parent);
                return;
            }

            if (right != null && right.keys.size() > d) {
                int separator = parent.keys.get(index);
                Node child = right.children.remove(0);
                int borrowedSeparator = right.keys.remove(0);
                node.children.add(child);
                child.parent = node;
                node.keys.add(separator);
                parent.keys.set(index, borrowedSeparator);
                updateAncestorKeys(parent);
                return;
            }

            if (left != null) {
                int separator = parent.keys.get(index - 1);
                left.keys.add(separator);
                left.keys.addAll(node.keys);
                for (Node child : node.children) {
                    left.children.add(child);
                    child.parent = left;
                }
                parent.children.remove(index);
                parent.keys.remove(index - 1);
                updateAncestorKeys(parent);
                handleInternalUnderflow(parent);
            } else if (right != null) {
                int separator = parent.keys.get(index);
                node.keys.add(separator);
                node.keys.addAll(right.keys);
                for (Node child : right.children) {
                    node.children.add(child);
                    child.parent = node;
                }
                parent.children.remove(index + 1);
                parent.keys.remove(index);
                updateAncestorKeys(parent);
                handleInternalUnderflow(parent);
            }
        }

        void updateAncestorKeys(Node node) {
            Node current = node;
            InternalNode parent = current.parent;

            while (parent != null) {
                int childIndex = parent.children.indexOf(current);
                if (childIndex > 0) parent.keys.set(childIndex - 1, current.firstKey());
                current = parent;
                parent = current.parent;
            }
        }

        int countNodes(Node node) {
            if (node.isLeaf()) return 1;
            int count = 1;
            for (Node child : ((InternalNode) node).children) count += countNodes(child);
            return count;
        }

        int countKeys() {
            int count = 0;
            LeafNode leaf = firstLeaf;
            while (leaf != null) {
                count += leaf.entries.size();
                leaf = leaf.next;
            }
            return count;
        }

        int height() {
            int h = 1;
            Node cur = root;
            while (!cur.isLeaf()) {
                h++;
                cur = ((InternalNode) cur).children.get(0);
            }
            return h;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !args[0].equalsIgnoreCase("init")) {
            System.out.println("Usage: java bplus init <d>");
            return;
        }

        int d;
        try {
            d = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("d must be an integer.");
            return;
        }

        if (d < 1) {
            System.out.println("d must be at least 1.");
            return;
        }

        BPlusTree tree = new BPlusTree(d);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;

        while ((line = reader.readLine()) != null) processCommand(tree, line);
    }

    static void processCommand(BPlusTree tree, String line) {
        line = line.trim();
        if (line.isEmpty()) return;

        String[] p = line.split("\\s+");
        String command = p[0].toUpperCase();

        try {
            switch (command) {
                case "INSERT":
                    if (p.length != 3) {
                        System.out.println("Invalid INSERT command.");
                        return;
                    }
                    if (tree.insert(Integer.parseInt(p[1]), Integer.parseInt(p[2])))
                        System.out.println("key inserted");
                    else
                        System.out.println("key already exists");
                    break;

                case "DELETE":
                    if (p.length != 2) {
                        System.out.println("Invalid DELETE command.");
                        return;
                    }
                    if (tree.delete(Integer.parseInt(p[1])))
                        System.out.println("key deleted");
                    else
                        System.out.println("key not found");
                    break;

                case "SEARCH":
                    if (p.length != 2) {
                        System.out.println("Invalid SEARCH command.");
                        return;
                    }
                    int result = tree.search(Integer.parseInt(p[1]));
                    if (result == -1)
                        System.out.println("key not found");
                    else
                        System.out.println("key found, point is " + result);
                    break;

                case "RANGESEARCH":
                    if (p.length != 3) {
                        System.out.println("Invalid RANGESEARCH command.");
                        return;
                    }
                    List<Entry> results = tree.rangeSearch(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    if (results.isEmpty()) {
                        System.out.println("key not found");
                    } else {
                        System.out.println("found");
                        for (Entry e : results) System.out.println(e.key + " " + e.pointer);
                    }
                    break;

                case "PRINT":
                    if (p.length == 1) tree.printTree();
                    else if (p.length == 2 && p[1].equalsIgnoreCase("STATISTICS"))
                        tree.printStatistics();
                    else
                        System.out.println("Invalid PRINT command.");
                    break;

                default:
                    System.out.println("Invalid command.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
        }
    }
}
